package tw.com.baozugong.backup

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.immediateTransaction
import androidx.room.useWriterConnection
import org.json.JSONArray
import org.json.JSONObject
import tw.com.baozugong.data.*
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class BackupPreview(val createdAt: String, val venues: Int, val rooms: Int, val tenants: Int, val leases: Int, val invoices: Int, val payments: Int)

class BackupManager(private val db: AppDatabase) {
    private val dao = db.dao()

    suspend fun export(resolver: ContentResolver, uri: Uri, password: String) {
        require(password.length >= 6) { "備份密碼至少需要 6 個字元" }
        val json = snapshot().toString().toByteArray(Charsets.UTF_8)
        resolver.openOutputStream(uri)?.use { it.write(encrypt(json, password)) } ?: error("無法寫入備份檔")
    }

    suspend fun exportToFolder(resolver: ContentResolver, folderUri: Uri, password: String, automatic: Boolean): Uri {
        require(password.length >= 6) { "備份密碼至少需要 6 個字元" }
        val parentId = DocumentsContract.getTreeDocumentId(folderUri)
        val parent = DocumentsContract.buildDocumentUriUsingTree(folderUri, parentId)
        val stamp = java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmm").format(LocalDateTime.now())
        val prefix = if (automatic) "包租公_自動備份" else "包租公_雲端備份"
        val target = DocumentsContract.createDocument(resolver, parent, "application/octet-stream", "${prefix}_${stamp}.bzg")
            ?: error("無法在選擇的雲端資料夾建立備份")
        export(resolver, target, password)
        return target
    }

    suspend fun preview(resolver: ContentResolver, uri: Uri, password: String): BackupPreview {
        val root = read(resolver, uri, password)
        return BackupPreview(root.getString("createdAt"), root.array("venues").length(), root.array("rooms").length(), root.array("tenants").length(), root.array("leases").length(), root.array("invoices").length(), root.array("payments").length())
    }

    suspend fun restore(resolver: ContentResolver, uri: Uri, password: String) {
        val root = read(resolver, uri, password)
        require(root.getInt("version") in 1..2) { "不支援此備份版本" }
        db.useWriterConnection { connection ->
            connection.immediateTransaction {
                dao.clearAll()
                dao.restoreVenues(root.array("venues").objects().map { Venue(it.long("id"),it.str("name"),it.bool("active"),it.str("note")) })
                dao.restoreRooms(root.array("rooms").objects().map { RentalRoom(it.long("id"),it.long("venueId"),it.str("name"),it.long("defaultRent"),it.str("status"),it.str("note")) })
                dao.restoreTenants(root.array("tenants").objects().map { Tenant(it.long("id"),it.str("name"),it.str("phone"),it.str("lineName"),it.str("note"),it.bool("archived")) })
                dao.restoreLeases(root.array("leases").objects().map { Lease(id=it.long("id"),roomId=it.long("roomId"),tenantId=it.long("tenantId"),startDate=it.str("startDate"),endDate=it.str("endDate"),monthlyRent=it.long("monthlyRent"),dueDay=it.int("dueDay"),deposit=it.long("deposit"),waterFee=it.optLongOrZero("waterFee"),managementFee=it.optLongOrZero("managementFee"),electricityFee=it.optLongOrZero("electricityFee"),note=it.str("note"),status=it.str("status"),endedAt=it.optString("endedAt").ifBlank { null }) })
                dao.restoreInvoices(root.array("invoices").objects().map { Invoice(it.long("id"),it.long("leaseId"),it.str("billingMonth"),it.str("dueDate"),it.str("status"),it.str("note"),it.str("createdAt")) })
                dao.restoreInvoiceItems(root.array("items").objects().map { InvoiceItem(it.long("id"),it.long("invoiceId"),it.str("type"),it.str("title"),it.long("amount")) })
                dao.restorePayments(root.array("payments").objects().map { Payment(it.long("id"),it.long("invoiceId"),it.long("amount"),it.str("paidDate"),it.str("method"),it.str("note"),it.str("createdAt"),it.str("updatedAt")) })
            }
        }
    }

    suspend fun exportCsv(resolver: ContentResolver, uri: Uri, rows: List<InvoiceListRow>) {
        val text = buildString {
            appendLine("月份,場館,房間,房客,到期日,應收,已收,狀態")
            rows.forEach { r -> appendLine(listOf(r.billingMonth,r.venueName,r.roomName,r.tenantName,r.dueDate,r.total,r.paid,r.displayStatus(java.time.LocalDate.now().toString())).joinToString(",") { csv(it.toString()) }) }
        }
        resolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8)?.use { it.write("\uFEFF$text") } ?: error("無法寫入 CSV")
    }

    private suspend fun snapshot(): JSONObject = JSONObject().apply {
        put("version", 2); put("createdAt", LocalDateTime.now().toString())
        put("venues", JSONArray(dao.allVenues().map { JSONObject().apply { put("id",it.id);put("name",it.name);put("active",it.active);put("note",it.note) } }))
        put("rooms", JSONArray(dao.allRooms().map { JSONObject().apply { put("id",it.id);put("venueId",it.venueId);put("name",it.name);put("defaultRent",it.defaultRent);put("status",it.status);put("note",it.note) } }))
        put("tenants", JSONArray(dao.allTenants().map { JSONObject().apply { put("id",it.id);put("name",it.name);put("phone",it.phone);put("lineName",it.lineName);put("note",it.note);put("archived",it.archived) } }))
        put("leases", JSONArray(dao.allLeases().map { JSONObject().apply { put("id",it.id);put("roomId",it.roomId);put("tenantId",it.tenantId);put("startDate",it.startDate);put("endDate",it.endDate);put("monthlyRent",it.monthlyRent);put("dueDay",it.dueDay);put("deposit",it.deposit);put("waterFee",it.waterFee);put("managementFee",it.managementFee);put("electricityFee",it.electricityFee);put("note",it.note);put("status",it.status);put("endedAt",it.endedAt ?: "") } }))
        put("invoices", JSONArray(dao.allInvoices().map { JSONObject().apply { put("id",it.id);put("leaseId",it.leaseId);put("billingMonth",it.billingMonth);put("dueDate",it.dueDate);put("status",it.status);put("note",it.note);put("createdAt",it.createdAt) } }))
        put("items", JSONArray(dao.allInvoiceItems().map { JSONObject().apply { put("id",it.id);put("invoiceId",it.invoiceId);put("type",it.type);put("title",it.title);put("amount",it.amount) } }))
        put("payments", JSONArray(dao.allPayments().map { JSONObject().apply { put("id",it.id);put("invoiceId",it.invoiceId);put("amount",it.amount);put("paidDate",it.paidDate);put("method",it.method);put("note",it.note);put("createdAt",it.createdAt);put("updatedAt",it.updatedAt) } }))
    }

    private fun read(resolver: ContentResolver, uri: Uri, password: String): JSONObject {
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: error("無法讀取備份檔")
        return try { JSONObject(decrypt(bytes, password).toString(Charsets.UTF_8)) } catch (_: Exception) { error("密碼錯誤或備份檔已損壞") }
    }

    private fun encrypt(data: ByteArray, password: String): ByteArray {
        val salt=ByteArray(16).also { SecureRandom().nextBytes(it) }; val iv=ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher=Cipher.getInstance("AES/GCM/NoPadding"); cipher.init(Cipher.ENCRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv))
        return "BZG1".toByteArray()+salt+iv+cipher.doFinal(data)
    }
    private fun decrypt(data: ByteArray, password: String): ByteArray {
        require(data.size > 32 && data.copyOfRange(0,4).toString(Charsets.UTF_8)=="BZG1")
        val salt=data.copyOfRange(4,20); val iv=data.copyOfRange(20,32); val cipher=Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE,key(password,salt),GCMParameterSpec(128,iv)); return cipher.doFinal(data.copyOfRange(32,data.size))
    }
    private fun key(password: String, salt: ByteArray) = SecretKeySpec(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(PBEKeySpec(password.toCharArray(),salt,120_000,256)).encoded,"AES")
    private fun csv(value:String)="\"${value.replace("\"","\"\"")}\""
}

private fun JSONObject.array(name:String)=getJSONArray(name)
private fun JSONArray.objects()=(0 until length()).map { getJSONObject(it) }
private fun JSONObject.long(name:String)=getLong(name)
private fun JSONObject.int(name:String)=getInt(name)
private fun JSONObject.str(name:String)=getString(name)
private fun JSONObject.bool(name:String)=getBoolean(name)
private fun JSONObject.optLongOrZero(name:String)=if(has(name)) optLong(name,0) else 0
