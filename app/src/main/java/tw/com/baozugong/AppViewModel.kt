package tw.com.baozugong

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import tw.com.baozugong.backup.BackupManager
import tw.com.baozugong.backup.CloudBackupStore
import tw.com.baozugong.data.*

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val db = AndroidDatabaseProvider.get(app)
    val repository = AppRepository(db)
    val controller = AppController(repository, viewModelScope)
    val settingsStore = SettingsStore(app)
    val backup = BackupManager(db)
    val cloudBackup = CloudBackupStore(app)
    val invoices = controller.invoices
    fun run(action: suspend AppRepository.() -> Unit) = controller.run(action)
}
