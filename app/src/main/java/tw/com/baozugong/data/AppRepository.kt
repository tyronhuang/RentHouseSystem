package tw.com.baozugong.data

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import tw.com.baozugong.domain.BillingRules
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth

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

    suspend fun createLease(value: Lease) = db.withTransaction {
        val room = dao.room(value.roomId) ?: error("找不到房間")
        require(room.status == RoomStatus.VACANT) { "此房間目前不可出租" }
        dao.insertLease(value)
        dao.setRoomStatus(value.roomId, RoomStatus.RENTED)
    }

    suspend fun endLease(leaseId: Long, date: LocalDate = LocalDate.now()) = db.withTransaction {
        val lease = dao.lease(leaseId) ?: return@withTransaction
        dao.updateLease(lease.copy(status = LeaseStatus.ENDED, endedAt = date.toString()))
        dao.setRoomStatus(lease.roomId, RoomStatus.VACANT)
    }

    suspend fun updateLease(value: Lease) {
        require(LocalDate.parse(value.startDate) <= LocalDate.parse(value.endDate)) { "到期日不可早於起租日" }
        require(value.monthlyRent > 0) { "月租必須大於 0" }
        require(value.dueDay in 1..31) { "繳租日必須介於 1 到 31 日" }
        dao.updateLease(value)
    }

    suspend fun renewLease(oldLeaseId: Long, newEndDate: String) {
        db.withTransaction {
            val old = dao.lease(oldLeaseId) ?: return@withTransaction
            require(old.status == LeaseStatus.ACTIVE) { "只能續約有效租約" }
            val newStart = BillingRules.renewalStart(old.endDate)
            require(LocalDate.parse(newEndDate) >= LocalDate.parse(newStart)) { "新到期日必須晚於原租約" }
            dao.updateLease(old.copy(status = LeaseStatus.ENDED, endedAt = old.endDate))
            dao.insertLease(old.copy(id = 0, startDate = newStart, endDate = newEndDate, status = LeaseStatus.ACTIVE, endedAt = null, note = listOf(old.note, "續約自租約 #${old.id}").filter { it.isNotBlank() }.joinToString(" · ")))
        }
    }

    suspend fun generateMissingInvoices(today: LocalDate = LocalDate.now()) {
        dao.activeLeases().forEach { lease ->
            BillingRules.monthsToGenerate(lease.startDate, lease.endDate, today.toString()).forEach { month ->
                db.withTransaction {
                    if (dao.activeInvoiceCountForRoomMonth(lease.roomId, month.toString()) > 0) return@withTransaction
                    val id = dao.insertInvoice(Invoice(
                        leaseId = lease.id,
                        billingMonth = month,
                        dueDate = BillingRules.dueDate(month, lease.dueDay),
                        createdAt = LocalDateTime.now().toString()
                    ))
                    if (id > 0) {
                        dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "RENT", title = "月租", amount = lease.monthlyRent))
                        if (lease.waterFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "WATER", title = "水費", amount = lease.waterFee))
                        if (lease.managementFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "MANAGEMENT", title = "管理費", amount = lease.managementFee))
                        if (lease.electricityFee > 0) dao.insertInvoiceItem(InvoiceItem(invoiceId = id, type = "ELECTRICITY", title = "電費", amount = lease.electricityFee))
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
        val now = LocalDateTime.now().toString()
        dao.insertPayment(Payment(invoiceId = invoiceId, amount = amount, paidDate = date, method = method, note = note, createdAt = now, updatedAt = now))
    }
    suspend fun updatePayment(value: Payment) = dao.updatePayment(value.copy(updatedAt = LocalDateTime.now().toString()))
    suspend fun voidInvoice(invoiceId: Long) = dao.voidInvoice(invoiceId)
    suspend fun clearAll() = dao.clearAll()
    fun daoForBackup() = dao
    fun database() = db
}
