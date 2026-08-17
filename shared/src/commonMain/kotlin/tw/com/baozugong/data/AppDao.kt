package tw.com.baozugong.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    @Query("SELECT * FROM venues ORDER BY active DESC, id") fun venues(): Flow<List<Venue>>
    @Query("SELECT * FROM venues ORDER BY id") suspend fun allVenues(): List<Venue>
    @Insert suspend fun insertVenue(value: Venue): Long
    @Update suspend fun updateVenue(value: Venue)

    @Query("SELECT r.id, r.venueId, v.name venueName, r.name, r.defaultRent, r.status, r.note FROM rooms r JOIN venues v ON v.id=r.venueId ORDER BY v.id,r.name") fun rooms(): Flow<List<RoomListRow>>
    @Query("SELECT * FROM rooms ORDER BY id") suspend fun allRooms(): List<RentalRoom>
    @Query("SELECT * FROM rooms WHERE id=:id") suspend fun room(id: Long): RentalRoom?
    @Insert suspend fun insertRoom(value: RentalRoom): Long
    @Update suspend fun updateRoom(value: RentalRoom)
    @Query("UPDATE rooms SET status=:status WHERE id=:id") suspend fun setRoomStatus(id: Long, status: String)

    @Query("SELECT * FROM tenants ORDER BY archived,name") fun tenants(): Flow<List<Tenant>>
    @Query("SELECT * FROM tenants ORDER BY id") suspend fun allTenants(): List<Tenant>
    @Query("SELECT * FROM tenants WHERE id=:id") suspend fun tenant(id: Long): Tenant?
    @Insert suspend fun insertTenant(value: Tenant): Long
    @Update suspend fun updateTenant(value: Tenant)

    @Query("""
        SELECT l.id,l.roomId,r.name roomName,v.name venueName,l.tenantId,t.name tenantName,t.phone tenantPhone,
        l.startDate,l.endDate,l.monthlyRent,l.dueDay,l.deposit,l.waterFee,l.managementFee,l.electricityFee,l.note,l.status,l.endedAt
        FROM leases l JOIN rooms r ON r.id=l.roomId JOIN venues v ON v.id=r.venueId JOIN tenants t ON t.id=l.tenantId
        ORDER BY l.status,l.endDate
    """) fun leases(): Flow<List<LeaseListRow>>
    @Query("SELECT * FROM leases ORDER BY id") suspend fun allLeases(): List<Lease>
    @Query("SELECT * FROM leases WHERE id=:id") suspend fun lease(id: Long): Lease?
    @Query("SELECT * FROM leases WHERE status='ACTIVE'") suspend fun activeLeases(): List<Lease>
    @Insert suspend fun insertLease(value: Lease): Long
    @Update suspend fun updateLease(value: Lease)

    @Query("""
        SELECT i.id,i.leaseId,i.billingMonth,i.dueDate,i.status rawStatus,l.roomId,r.name roomName,v.name venueName,
        t.name tenantName,t.phone tenantPhone,
        COALESCE((SELECT SUM(amount) FROM invoice_items x WHERE x.invoiceId=i.id),0) total,
        COALESCE((SELECT SUM(amount) FROM payments p WHERE p.invoiceId=i.id),0) paid
        FROM invoices i JOIN leases l ON l.id=i.leaseId JOIN rooms r ON r.id=l.roomId
        JOIN venues v ON v.id=r.venueId JOIN tenants t ON t.id=l.tenantId
        ORDER BY i.billingMonth DESC,v.id,r.name
    """) fun invoices(): Flow<List<InvoiceListRow>>
    @Query("SELECT * FROM invoices ORDER BY id") suspend fun allInvoices(): List<Invoice>
    @Query("SELECT * FROM invoices WHERE id=:id") suspend fun invoice(id: Long): Invoice?
    @Query("SELECT COUNT(*) FROM invoices i JOIN leases l ON l.id=i.leaseId WHERE l.roomId=:roomId AND i.billingMonth=:billingMonth AND i.status!='VOID'")
    suspend fun activeInvoiceCountForRoomMonth(roomId: Long, billingMonth: String): Int
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun insertInvoice(value: Invoice): Long
    @Update suspend fun updateInvoice(value: Invoice)
    @Query("UPDATE invoices SET status='VOID' WHERE id=:id") suspend fun voidInvoice(id: Long)

    @Query("SELECT * FROM invoice_items ORDER BY id") suspend fun allInvoiceItems(): List<InvoiceItem>
    @Query("SELECT * FROM invoice_items WHERE invoiceId=:invoiceId ORDER BY id") fun invoiceItems(invoiceId: Long): Flow<List<InvoiceItem>>
    @Insert suspend fun insertInvoiceItem(value: InvoiceItem): Long
    @Update suspend fun updateInvoiceItem(value: InvoiceItem)

    @Query("SELECT * FROM payments ORDER BY id") suspend fun allPayments(): List<Payment>
    @Query("SELECT * FROM payments WHERE invoiceId=:invoiceId ORDER BY paidDate,id") fun payments(invoiceId: Long): Flow<List<Payment>>
    @Insert suspend fun insertPayment(value: Payment): Long
    @Update suspend fun updatePayment(value: Payment)

    @Insert suspend fun restoreVenues(values: List<Venue>)
    @Insert suspend fun restoreRooms(values: List<RentalRoom>)
    @Insert suspend fun restoreTenants(values: List<Tenant>)
    @Insert suspend fun restoreLeases(values: List<Lease>)
    @Insert suspend fun restoreInvoices(values: List<Invoice>)
    @Insert suspend fun restoreInvoiceItems(values: List<InvoiceItem>)
    @Insert suspend fun restorePayments(values: List<Payment>)

    @Query("DELETE FROM payments") suspend fun clearPayments()
    @Query("DELETE FROM invoice_items") suspend fun clearItems()
    @Query("DELETE FROM invoices") suspend fun clearInvoices()
    @Query("DELETE FROM leases") suspend fun clearLeases()
    @Query("DELETE FROM tenants") suspend fun clearTenants()
    @Query("DELETE FROM rooms") suspend fun clearRooms()
    @Query("DELETE FROM venues") suspend fun clearVenues()

    @Transaction
    suspend fun clearAll() {
        clearPayments(); clearItems(); clearInvoices(); clearLeases(); clearTenants(); clearRooms(); clearVenues()
    }
}
