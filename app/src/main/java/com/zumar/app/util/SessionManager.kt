package com.zumar.app.util

import android.content.Context
import android.content.SharedPreferences
import com.zumar.app.model.Beneficiary
import com.zumar.app.model.NotificationPrefs
import com.zumar.app.model.Transaction
import com.zumar.app.model.User
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Very simple on-device "backend": stores registered users, their demo
 * wallet balance/transactions/beneficiaries/notification prefs, and the
 * logged-in session inside SharedPreferences as JSON.
 *
 * Passwords and PINs are hashed (SHA-256) before being stored, so this
 * device never keeps them in plain text. Note this is still a DEMO
 * account system — there is no server, and true production-grade
 * security (HTTPS, salted hashing, rate limiting, brute-force
 * protection) requires a real backend, which is the next step once
 * this app is ready to go live.
 */
class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("zumar_prefs", Context.MODE_PRIVATE)

    fun registerUser(user: User): Boolean {
        val users = getAllUsersJson()
        for (i in 0 until users.length()) {
            val obj = users.getJSONObject(i)
            if (obj.getString("email").equals(user.email, ignoreCase = true)) return false
            if (obj.getString("phone") == user.phone) return false
        }
        users.put(userToJson(user))
        prefs.edit().putString(KEY_USERS, users.toString()).apply()
        return true
    }

    /** identifier can be email OR phone number. Checks WITHOUT starting the session. */
    fun checkCredentials(identifier: String, hashedPassword: String): String? {
        val users = getAllUsersJson()
        for (i in 0 until users.length()) {
            val obj = users.getJSONObject(i)
            val matchesIdentifier = obj.getString("email").equals(identifier, ignoreCase = true) ||
                obj.getString("phone") == identifier
            if (matchesIdentifier && obj.getString("hashedPassword") == hashedPassword) {
                return obj.getString("email")
            }
        }
        return null
    }

    fun findEmailByIdentifier(identifier: String): String? {
        val users = getAllUsersJson()
        for (i in 0 until users.length()) {
            val obj = users.getJSONObject(i)
            if (obj.getString("email").equals(identifier, ignoreCase = true) || obj.getString("phone") == identifier) {
                return obj.getString("email")
            }
        }
        return null
    }

    fun resetPassword(email: String, newHashedPassword: String) {
        updateUserJson(email) { it.put("hashedPassword", newHashedPassword) }
    }

    fun completeLogin(email: String) {
        prefs.edit().putString(KEY_CURRENT_EMAIL, email).apply()
    }

    fun getCurrentUser(): User? {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return null
        val users = getAllUsersJson()
        for (i in 0 until users.length()) {
            val obj = users.getJSONObject(i)
            if (obj.getString("email").equals(email, ignoreCase = true)) return jsonToUser(obj)
        }
        return null
    }

    fun updateBalance(newBalance: Double) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) { it.put("balance", newBalance) }
    }

    fun addTransaction(label: String, amount: Double, time: String, direction: String, network: String? = null, phone: String? = null) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) { obj ->
            val existing = obj.optJSONArray("transactions") ?: JSONArray()
            val tx = JSONObject()
            tx.put("label", label)
            tx.put("amount", amount)
            tx.put("time", time)
            tx.put("direction", direction)
            if (network != null) tx.put("network", network)
            if (phone != null) tx.put("phone", phone)
            val updated = JSONArray()
            updated.put(tx)
            for (j in 0 until existing.length()) updated.put(existing.getJSONObject(j))
            obj.put("transactions", updated)
        }
    }

    fun getTransactions(): List<Transaction> = getCurrentUser()?.transactions ?: emptyList()

    fun updateProfile(firstName: String, middleName: String, lastName: String, phone: String, address: String, state: String) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) {
            it.put("firstName", firstName)
            it.put("middleName", middleName)
            it.put("lastName", lastName)
            it.put("phone", phone)
            it.put("address", address)
            it.put("state", state)
        }
    }

    fun addBeneficiary(name: String, phone: String) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) { obj ->
            val existing = obj.optJSONArray("beneficiaries") ?: JSONArray()
            val b = JSONObject()
            b.put("name", name)
            b.put("phone", phone)
            existing.put(b)
            obj.put("beneficiaries", existing)
        }
    }

    fun removeBeneficiary(index: Int) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) { obj ->
            val existing = obj.optJSONArray("beneficiaries") ?: JSONArray()
            val updated = JSONArray()
            for (j in 0 until existing.length()) if (j != index) updated.put(existing.getJSONObject(j))
            obj.put("beneficiaries", updated)
        }
    }

    fun updatePin(newHashedPin: String) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) { it.put("hashedPin", newHashedPin) }
    }

    fun updateNotifications(prefsObj: NotificationPrefs) {
        val email = prefs.getString(KEY_CURRENT_EMAIL, null) ?: return
        updateUserJson(email) {
            val n = JSONObject()
            n.put("transactionAlerts", prefsObj.transactionAlerts)
            n.put("promotions", prefsObj.promotions)
            n.put("securityAlerts", prefsObj.securityAlerts)
            it.put("notifications", n)
        }
    }

    fun logout() {
        prefs.edit().remove(KEY_CURRENT_EMAIL).apply()
    }

    fun isLoggedIn(): Boolean = prefs.getString(KEY_CURRENT_EMAIL, null) != null

    private fun updateUserJson(email: String, mutate: (JSONObject) -> Unit) {
        val users = getAllUsersJson()
        for (i in 0 until users.length()) {
            val obj = users.getJSONObject(i)
            if (obj.getString("email").equals(email, ignoreCase = true)) mutate(obj)
        }
        prefs.edit().putString(KEY_USERS, users.toString()).apply()
    }

    private fun getAllUsersJson(): JSONArray {
        val raw = prefs.getString(KEY_USERS, "[]")
        return JSONArray(raw)
    }

    private fun userToJson(u: User): JSONObject {
        val obj = JSONObject()
        obj.put("firstName", u.firstName)
        obj.put("middleName", u.middleName ?: "")
        obj.put("lastName", u.lastName)
        obj.put("phone", u.phone)
        obj.put("email", u.email)
        obj.put("hashedPassword", u.hashedPassword)
        obj.put("address", u.address)
        obj.put("state", u.state)
        obj.put("dob", u.dob)
        obj.put("gender", u.gender)
        obj.put("hashedPin", u.hashedPin)
        obj.put("balance", u.balance)
        obj.put("transactions", JSONArray())
        obj.put("beneficiaries", JSONArray())
        val n = JSONObject()
        n.put("transactionAlerts", true)
        n.put("promotions", true)
        n.put("securityAlerts", true)
        obj.put("notifications", n)
        return obj
    }

    private fun jsonToUser(obj: JSONObject): User {
        val txArray = obj.optJSONArray("transactions") ?: JSONArray()
        val txList = mutableListOf<Transaction>()
        for (i in 0 until txArray.length()) {
            val t = txArray.getJSONObject(i)
            txList.add(
                Transaction(
                    t.getString("label"), t.getDouble("amount"), t.getString("time"),
                    t.optString("direction", "out"), t.optString("network", null), t.optString("phone", null)
                )
            )
        }
        val benArray = obj.optJSONArray("beneficiaries") ?: JSONArray()
        val benList = mutableListOf<Beneficiary>()
        for (i in 0 until benArray.length()) {
            val b = benArray.getJSONObject(i)
            benList.add(Beneficiary(b.getString("name"), b.getString("phone")))
        }
        val n = obj.optJSONObject("notifications")
        val notif = if (n != null) NotificationPrefs(
            n.optBoolean("transactionAlerts", true),
            n.optBoolean("promotions", true),
            n.optBoolean("securityAlerts", true)
        ) else NotificationPrefs()

        return User(
            firstName = obj.getString("firstName"),
            middleName = obj.optString("middleName", ""),
            lastName = obj.getString("lastName"),
            phone = obj.getString("phone"),
            email = obj.getString("email"),
            hashedPassword = obj.getString("hashedPassword"),
            address = obj.getString("address"),
            state = obj.getString("state"),
            dob = obj.getString("dob"),
            gender = obj.getString("gender"),
            hashedPin = obj.getString("hashedPin"),
            balance = obj.optDouble("balance", 0.0),
            transactions = txList,
            beneficiaries = benList,
            notifications = notif
        )
    }

    companion object {
        private const val KEY_USERS = "users"
        private const val KEY_CURRENT_EMAIL = "current_email"

        fun generateOtp(): String = (1000..9999).random().toString()

        /** SHA-256 hash, hex-encoded. Used for passwords and transaction PINs. */
        fun hash(text: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(text.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
