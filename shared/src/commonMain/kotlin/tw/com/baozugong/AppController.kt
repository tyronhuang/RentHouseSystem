package tw.com.baozugong

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tw.com.baozugong.data.AppRepository
import tw.com.baozugong.data.DashboardSummary
import tw.com.baozugong.data.InvoiceStatus
import tw.com.baozugong.data.RoomStatus
import tw.com.baozugong.ui.currentDate
import tw.com.baozugong.ui.currentMonth

class AppController(
    val repository: AppRepository,
    private val scope: CoroutineScope,
) {
    val venues = repository.venues.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rooms = repository.rooms.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tenants = repository.tenants.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val leases = repository.leases.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val invoices = repository.invoices.stateIn(scope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dashboard = combine(rooms, invoices) { roomRows, billRows ->
        val month = currentMonth()
        val today = currentDate()
        val current = billRows.filter { it.billingMonth == month && it.rawStatus != InvoiceStatus.VOID }
        DashboardSummary(
            expected = current.sumOf { it.total },
            paid = current.sumOf { minOf(it.paid, it.total) },
            unpaid = current.sumOf { (it.total - it.paid).coerceAtLeast(0) },
            overdue = current.filter { it.dueDate < today }.sumOf { (it.total - it.paid).coerceAtLeast(0) },
            rentedRooms = roomRows.count { it.status == RoomStatus.RENTED },
            vacantRooms = roomRows.count { it.status == RoomStatus.VACANT },
            totalRooms = roomRows.count { it.status != RoomStatus.DISABLED },
        )
    }.stateIn(scope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())

    init {
        scope.launch { repository.generateMissingInvoices() }
    }

    fun run(action: suspend AppRepository.() -> Unit) = scope.launch { repository.action() }
}
