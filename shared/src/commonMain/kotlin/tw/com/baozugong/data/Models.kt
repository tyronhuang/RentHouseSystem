package tw.com.baozugong.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "venues")
data class Venue(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val active: Boolean = true, val note: String = "")

@Entity(
    tableName = "rooms",
    foreignKeys = [ForeignKey(entity = Venue::class, parentColumns = ["id"], childColumns = ["venueId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("venueId")],
)
data class RentalRoom(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val venueId: Long,
    val name: String,
    val defaultRent: Long = 0,
    val status: String = RoomStatus.VACANT,
    val note: String = "",
)

object RoomStatus { const val VACANT = "VACANT"; const val RENTED = "RENTED"; const val DISABLED = "DISABLED" }

@Entity(tableName = "tenants")
data class Tenant(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val phone: String = "",
    val lineName: String = "",
    val note: String = "",
    val archived: Boolean = false,
)

@Entity(
    tableName = "leases",
    foreignKeys = [
        ForeignKey(entity = RentalRoom::class, parentColumns = ["id"], childColumns = ["roomId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = Tenant::class, parentColumns = ["id"], childColumns = ["tenantId"], onDelete = ForeignKey.RESTRICT),
    ],
    indices = [Index("roomId"), Index("tenantId")],
)
data class Lease(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val roomId: Long,
    val tenantId: Long,
    val startDate: String,
    val endDate: String,
    val monthlyRent: Long,
    val dueDay: Int,
    val deposit: Long = 0,
    val waterFee: Long = 0,
    val managementFee: Long = 0,
    val electricityFee: Long = 0,
    val note: String = "",
    val status: String = LeaseStatus.ACTIVE,
    val endedAt: String? = null,
)

object LeaseStatus { const val ACTIVE = "ACTIVE"; const val ENDED = "ENDED" }

@Entity(
    tableName = "invoices",
    foreignKeys = [ForeignKey(entity = Lease::class, parentColumns = ["id"], childColumns = ["leaseId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("leaseId"), Index(value = ["leaseId", "billingMonth"], unique = true)],
)
data class Invoice(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val leaseId: Long,
    val billingMonth: String,
    val dueDate: String,
    val status: String = InvoiceStatus.PENDING,
    val note: String = "",
    val createdAt: String,
)

object InvoiceStatus { const val PENDING = "PENDING"; const val VOID = "VOID" }

@Entity(
    tableName = "invoice_items",
    foreignKeys = [ForeignKey(entity = Invoice::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("invoiceId")],
)
data class InvoiceItem(@PrimaryKey(autoGenerate = true) val id: Long = 0, val invoiceId: Long, val type: String, val title: String, val amount: Long)

@Entity(
    tableName = "payments",
    foreignKeys = [ForeignKey(entity = Invoice::class, parentColumns = ["id"], childColumns = ["invoiceId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("invoiceId")],
)
data class Payment(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val invoiceId: Long,
    val amount: Long,
    val paidDate: String,
    val method: String,
    val note: String = "",
    val createdAt: String,
    val updatedAt: String,
)

data class RoomListRow(val id: Long, val venueId: Long, val venueName: String, val name: String, val defaultRent: Long, val status: String, val note: String)
data class LeaseListRow(val id: Long, val roomId: Long, val roomName: String, val venueName: String, val tenantId: Long, val tenantName: String, val tenantPhone: String, val startDate: String, val endDate: String, val monthlyRent: Long, val dueDay: Int, val deposit: Long, val waterFee: Long, val managementFee: Long, val electricityFee: Long, val note: String, val status: String, val endedAt: String?)
data class InvoiceListRow(val id: Long, val leaseId: Long, val billingMonth: String, val dueDate: String, val rawStatus: String, val roomId: Long, val roomName: String, val venueName: String, val tenantName: String, val tenantPhone: String, val total: Long, val paid: Long)

data class DashboardSummary(
    val expected: Long = 0,
    val paid: Long = 0,
    val unpaid: Long = 0,
    val overdue: Long = 0,
    val rentedRooms: Int = 0,
    val vacantRooms: Int = 0,
    val totalRooms: Int = 0,
)

fun InvoiceListRow.displayStatus(today: String): String = when {
    rawStatus == InvoiceStatus.VOID -> "已作廢"
    paid >= total && total > 0 -> "已收"
    paid > 0 -> "部分收款"
    dueDate < today -> "逾期"
    else -> "待收"
}
