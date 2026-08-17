package tw.com.baozugong

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tw.com.baozugong.backup.BackupManager
import tw.com.baozugong.backup.CloudBackupStore
import tw.com.baozugong.data.*
import java.time.LocalDate
import java.time.YearMonth

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AppDatabase.get(app)
    val repository = AppRepository(db)
    val settingsStore = SettingsStore(app)
    val backup = BackupManager(db)
    val cloudBackup = CloudBackupStore(app)
    val venues = repository.venues.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val rooms = repository.rooms.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val tenants = repository.tenants.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val leases = repository.leases.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val invoices = repository.invoices.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    val dashboard = combine(rooms, invoices) { roomRows, billRows ->
        val month = YearMonth.now().toString(); val today = LocalDate.now().toString()
        val current = billRows.filter { it.billingMonth == month && it.rawStatus != InvoiceStatus.VOID }
        DashboardSummary(
            expected = current.sumOf { it.total }, paid = current.sumOf { minOf(it.paid, it.total) },
            unpaid = current.sumOf { (it.total-it.paid).coerceAtLeast(0) },
            overdue = current.filter { it.dueDate < today }.sumOf { (it.total-it.paid).coerceAtLeast(0) },
            rentedRooms = roomRows.count { it.status == RoomStatus.RENTED }, vacantRooms = roomRows.count { it.status == RoomStatus.VACANT },
            totalRooms = roomRows.count { it.status != RoomStatus.DISABLED }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DashboardSummary())

    init { viewModelScope.launch { repository.generateMissingInvoices() } }
    fun run(action: suspend AppRepository.() -> Unit) = viewModelScope.launch { repository.action() }
}
