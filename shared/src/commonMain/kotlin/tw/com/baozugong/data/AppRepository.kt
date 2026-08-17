package tw.com.baozugong.data

import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import tw.com.baozugong.domain.BillingRules

class AppRepository(private val db: AppDatabase) {
    private val dao = db.dao()
    val venues = dao.venues()
    val rooms = dao.rooms()
    val tenants = dao.tenants()
    val leases = dao.leases()
    val invoices = dao.invoices()

    suspend fun saveVenue(value: Venue) { if (value.id == 0L) dao.insertVenue(value) else dao.updateVenue(value) }
    suspend fun saveRoom(value: RentalRoom) { if (value.id == 0L) dao.insertRoom(value) else dao.updateRoom(value) }
    suspend fun saveTenant(value: Tenant): Long = if (value.id == 0L) dao.insertTenant(value) else { dao.updateTenant(value); value.id }

    suspend fun createLease(value: Lease) = db.useWriterConnection { connection ->
        connection.immediateTransaction {
            val room = dao.room(value.roomId) ?: error("找不到房間")
            require(room.status == RoomStatus.VACANT) { "房間目前不是空房" }
            dao.insertLease(value)
            dao.setRoomStatus(value.roomId, RoomStatus.RENTED)
        }
    }

    suspend fun endLease(leaseId: Long, date: String = currentDate()) = db.useWriterConnection { connection ->
        connection.immediateTransaction {
            val lease = dao.lease(leaseId) ?: return@immediateTransaction
            dao.updateLease(lease.copy(status = LeaseStatus.ENDED, endedAt = date))
            dao.setRoomStatus(lease.roomId, RoomStatus.VACANT)
        }
    }

    suspend fun updateLease(value: Lease) {
        require(value.startDate <= value.endDate) { "起租日不可晚於到期日" }
        require(value.monthlyRent > 0) { "月租必須大於 0" }
        require(value.dueDay in 1..31) { "繳租日必須介於 1 到 31 日" }
        dao.updateLease(value)
    }

    suspend fun renewLease(oldLeaseId: Long, newEndDate: String) {
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                val old = dao.lease(oldLeaseId) ?: return@immediateTransaction Unit
                require(old.status == LeaseStatus.ACTIVE) { "只有有效租約可以續約" }
                val newStart = BillingRules.renewalStart(old.endDate)
                require(newEndDate >= newStart) { "新到期日不可早於新起租日" }
                dao.updateLease(old.copy(status = LeaseStatus.ENDED, endedAt = old.endDate))
                dao.insertLease(old.copy(id = 0, startDate = newStart, endDate = newEndDate, status = LeaseStatus.ACTIVE, endedAt = null, note = listOf(old.note, "續約自舊租約 #${old.id}").filter { it.isNotBlank() }.joinToString(" · ")))
                Unit
            }
        }
    }

    suspend fun generateMissingInvoices(today: String = currentDate()) {
        dao.activeLeases().forEach { lease ->
            BillingRules.monthsToGenerate(lease.startDate, lease.endDate, today).forEach { month ->
                db.useWriterConnection { connection ->
                    connection.immediateTransaction {
                        if (dao.activeInvoiceCountForRoomMonth(lease.roomId, month) > 0) return@immediateTransaction
                        val id = dao.insertInvoice(Invoice(
                            leaseId = lease.id,
                            billingMonth = month,
                            dueDate = BillingRules.dueDate(month, lease.dueDay),
                            createdAt = currentTimestamp(),
                        ))
                        if (id > 0) {
                            dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "RENT", title = "租金", amount = lease.monthlyRent))
                            if (lease.waterFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "WATER", title = "水費", amount = lease.waterFee))
                            if (lease.managementFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "MANAGEMENT", title = "管理費", amount = lease.managementFee))
                            if (lease.electricityFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "ELECTRICITY", title = "電費", amount = lease.electricityFee))
                        }
                    }
                }
            }
        }
    }

    fun invoiceItems(invoiceId: Long): Flow<List<InvoiceItem>> = dao.invoiceItems(invoiceId)
    fun payments(invoiceId: Long): Flow<List<Payment>> = dao.payments(invoiceId)
    suspend fun addCharge(invoiceId: Long, title: String, amount: Long) = dao.insertInvoiceItem(InvoiceItem(invoiceId = invoiceId, type = "EXTRA", title = title, amount = amount))
    suspend fun updateCharge(value: InvoiceItem) = dao.updateInvoiceItem(value)
    suspend fun addPayment(invoiceId: Long, amount: Long, date: String, method: String, note: String) {
        val now = currentTimestamp()
        dao.insertPayment(Payment(invoiceId = invoiceId, amount = amount, paidDate = date, method = method, note = note, createdAt = now, updatedAt = now))
    }
    suspend fun updatePayment(value: Payment) = dao.updatePayment(value.copy(updatedAt = currentTimestamp()))
    suspend fun voidPayment(value: Payment) {
        if (!value.voided) dao.updatePayment(value.copy(voided = true, voidedAt = currentTimestamp(), updatedAt = currentTimestamp()))
    }
    suspend fun voidInvoice(invoiceId: Long) = dao.voidInvoice(invoiceId)
    suspend fun clearAll() = dao.clearAll()
    fun daoForBackup() = dao
    fun database() = db

    private fun currentDate(): String = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()
    private fun currentTimestamp(): String = Clock.System.now().toString()
}
