package com.zumar.app

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.zumar.app.model.NotificationPrefs
import com.zumar.app.model.Transaction
import com.zumar.app.model.User
import com.zumar.app.util.SessionManager
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class DashboardActivity : AppCompatActivity() {

    private lateinit var session: SessionManager
    private lateinit var tvBalance: TextView
    private lateinit var btnToggleBalance: ImageView
    private lateinit var llRecentTx: LinearLayout
    private lateinit var tvNoTxHome: TextView
    private lateinit var llHistoryList: LinearLayout
    private lateinit var tvNoTxHistory: TextView
    private var balance: Double = 0.0
    private var balanceVisible = true
    private var buyMode = "airtime" // airtime | data

    private lateinit var tabHome: View
    private lateinit var tabBuy: View
    private lateinit var tabHistory: View
    private lateinit var tabProfile: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        session = SessionManager(this)
        val user = session.getCurrentUser()
        if (user == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        balance = user.balance

        tabHome = findViewById(R.id.tabHome)
        tabBuy = findViewById(R.id.tabBuy)
        tabHistory = findViewById(R.id.tabHistory)
        tabProfile = findViewById(R.id.tabProfile)

        tvBalance = findViewById(R.id.tvBalance)
        btnToggleBalance = findViewById(R.id.btnToggleBalance)
        llRecentTx = findViewById(R.id.llRecentTx)
        tvNoTxHome = findViewById(R.id.tvNoTxHome)
        llHistoryList = findViewById(R.id.llHistoryList)
        tvNoTxHistory = findViewById(R.id.tvNoTxHistory)

        findViewById<TextView>(R.id.tvGreeting).text = "Hi, ${user.firstName}"
        updateBalanceDisplay()
        renderTransactions()

        findViewById<Button>(R.id.btnFundAccount).setOnClickListener { showFundDialog() }
        findViewById<TextView>(R.id.tvSeeAll).setOnClickListener { selectTab("history") }
        btnToggleBalance.setOnClickListener {
            balanceVisible = !balanceVisible
            updateBalanceDisplay()
        }

        findViewById<LinearLayout>(R.id.actionAirtime).setOnClickListener {
            selectTab("buy"); setBuyMode("airtime")
        }
        findViewById<LinearLayout>(R.id.actionData).setOnClickListener {
            selectTab("buy"); setBuyMode("data")
        }

        // Quick Buy chips -> pre-filled purchase dialogs
        findViewById<Button>(R.id.qbMtnAirtime).setOnClickListener { showAirtimeDialog("mtn", 100) }
        findViewById<Button>(R.id.qbAirtelData).setOnClickListener { showDataDialog("airtel", "1.5GB", 500) }
        findViewById<Button>(R.id.qbGloAirtime).setOnClickListener { showAirtimeDialog("glo", 200) }
        findViewById<Button>(R.id.qb9mobileData).setOnClickListener { showDataDialog("9mobile", "500MB", 150) }

        // Buy tab: segmented control
        findViewById<Button>(R.id.segAirtime).setOnClickListener { setBuyMode("airtime") }
        findViewById<Button>(R.id.segData).setOnClickListener { setBuyMode("data") }
        findViewById<Button>(R.id.buyMtn).setOnClickListener { openBuyForNetwork("mtn") }
        findViewById<Button>(R.id.buyAirtel).setOnClickListener { openBuyForNetwork("airtel") }
        findViewById<Button>(R.id.buyGlo).setOnClickListener { openBuyForNetwork("glo") }
        findViewById<Button>(R.id.buy9mobile).setOnClickListener { openBuyForNetwork("9mobile") }

        // Profile rows
        findViewById<TextView>(R.id.rowEditProfile).setOnClickListener { showEditProfileDialog() }
        findViewById<TextView>(R.id.rowBeneficiaries).setOnClickListener { showBeneficiariesDialog() }
        findViewById<TextView>(R.id.rowPin).setOnClickListener { showPinChangeDialog() }
        findViewById<TextView>(R.id.rowNotifications).setOnClickListener { showNotificationsDialog() }
        findViewById<TextView>(R.id.rowSupport).setOnClickListener {
            startActivity(Intent(this, CustomerServiceActivity::class.java))
        }
        findViewById<TextView>(R.id.rowAbout).setOnClickListener { showAboutDialog() }
        findViewById<TextView>(R.id.rowLogout).setOnClickListener {
            session.logout()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        // Bottom nav
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener { selectTab("home") }
        findViewById<LinearLayout>(R.id.navBuy).setOnClickListener { selectTab("buy") }
        findViewById<LinearLayout>(R.id.navHistory).setOnClickListener { selectTab("history") }
        findViewById<LinearLayout>(R.id.navProfile).setOnClickListener { selectTab("profile") }

        setBuyMode("airtime")
        selectTab("home")
    }

    // -------------------------------------------------------------------
    // Tabs
    // -------------------------------------------------------------------
    private fun selectTab(tab: String) {
        tabHome.visibility = if (tab == "home") View.VISIBLE else View.GONE
        tabBuy.visibility = if (tab == "buy") View.VISIBLE else View.GONE
        tabHistory.visibility = if (tab == "history") View.VISIBLE else View.GONE
        tabProfile.visibility = if (tab == "profile") View.VISIBLE else View.GONE

        val activeColor = getColor(R.color.green_primary)
        val inactiveColor = getColor(R.color.text_muted)
        findViewById<TextView>(R.id.navHomeText).setTextColor(if (tab == "home") activeColor else inactiveColor)
        findViewById<TextView>(R.id.navBuyText).setTextColor(if (tab == "buy") activeColor else inactiveColor)
        findViewById<TextView>(R.id.navHistoryText).setTextColor(if (tab == "history") activeColor else inactiveColor)
        findViewById<TextView>(R.id.navProfileText).setTextColor(if (tab == "profile") activeColor else inactiveColor)
    }

    private fun setBuyMode(mode: String) {
        buyMode = mode
        val segAirtime = findViewById<Button>(R.id.segAirtime)
        val segData = findViewById<Button>(R.id.segData)
        if (mode == "airtime") {
            segAirtime.setBackgroundResource(R.drawable.bg_tile_rounded)
            segAirtime.setTextColor(getColor(R.color.text_dark))
            segData.setBackgroundColor(getColor(android.R.color.transparent))
            segData.setTextColor(getColor(R.color.text_muted))
        } else {
            segData.setBackgroundResource(R.drawable.bg_tile_rounded)
            segData.setTextColor(getColor(R.color.text_dark))
            segAirtime.setBackgroundColor(getColor(android.R.color.transparent))
            segAirtime.setTextColor(getColor(R.color.text_muted))
        }
    }

    private fun openBuyForNetwork(network: String) {
        if (buyMode == "airtime") showAirtimeDialog(network, null) else showDataDialog(network, null, null)
    }

    // -------------------------------------------------------------------
    // Balance
    // -------------------------------------------------------------------
    private fun updateBalanceDisplay() {
        tvBalance.text = if (balanceVisible) "₦${format(balance)}" else "₦ • • • • • •"
        btnToggleBalance.setImageResource(if (balanceVisible) R.drawable.ic_eye else R.drawable.ic_eye_off)
    }

    private fun showFundDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_fund_wallet)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        val etAmount = dialog.findViewById<EditText>(R.id.etFundAmount)
        val btnConfirm = dialog.findViewById<Button>(R.id.btnConfirmFund)

        btnConfirm.setOnClickListener {
            val amount = etAmount.text.toString().trim().toDoubleOrNull()
            if (amount == null || amount <= 0) {
                Toast.makeText(this, "Enter a valid amount.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            balance += amount
            session.updateBalance(balance)
            session.addTransaction("Wallet funding", amount, nowString(), "in")
            updateBalanceDisplay()
            renderTransactions()
            dialog.dismiss()
            Toast.makeText(this, "₦${format(amount)} added to your wallet!", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    // -------------------------------------------------------------------
    // Airtime / Data purchase dialogs (PIN-gated)
    // -------------------------------------------------------------------
    private fun showAirtimeDialog(presetNetwork: String?, presetAmount: Int?) {
        val user = session.getCurrentUser() ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_buy_airtime)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        var network = presetNetwork ?: "mtn"
        var amount = presetAmount

        val netButtons = mapOf(
            "mtn" to dialog.findViewById<Button>(R.id.atNetMtn),
            "airtel" to dialog.findViewById<Button>(R.id.atNetAirtel),
            "glo" to dialog.findViewById<Button>(R.id.atNetGlo),
            "9mobile" to dialog.findViewById<Button>(R.id.atNet9mobile)
        )
        fun refreshNetSelection() {
            netButtons.forEach { (id, btn) -> btn.alpha = if (id == network) 1f else 0.45f }
        }
        netButtons.forEach { (id, btn) -> btn.setOnClickListener { network = id; refreshNetSelection() } }
        refreshNetSelection()

        val etPhone = dialog.findViewById<EditText>(R.id.atPhone)
        etPhone.setText(user.phone)

        val amtButtons = mapOf(
            100 to dialog.findViewById<Button>(R.id.atAmt100), 200 to dialog.findViewById<Button>(R.id.atAmt200),
            500 to dialog.findViewById<Button>(R.id.atAmt500), 1000 to dialog.findViewById<Button>(R.id.atAmt1000),
            2000 to dialog.findViewById<Button>(R.id.atAmt2000), 5000 to dialog.findViewById<Button>(R.id.atAmt5000)
        )
        fun refreshAmtSelection() {
            amtButtons.forEach { (v, btn) -> btn.alpha = if (v == amount) 1f else 0.6f }
        }
        amtButtons.forEach { (v, btn) -> btn.setOnClickListener { amount = v; refreshAmtSelection() } }
        refreshAmtSelection()

        val etPin = dialog.findViewById<EditText>(R.id.atPin)
        val tvError = dialog.findViewById<TextView>(R.id.atError)

        dialog.findViewById<Button>(R.id.btnPayAirtime).setOnClickListener {
            val amt = amount
            val phone = etPhone.text.toString().trim()
            val pin = etPin.text.toString().trim()

            if (amt == null) { showDialogError(tvError, "Select an amount."); return@setOnClickListener }
            if (phone.length < 10) { showDialogError(tvError, "Enter a valid phone number."); return@setOnClickListener }
            if (amt > balance) { showDialogError(tvError, "Insufficient balance — fund your wallet first."); return@setOnClickListener }
            if (pin.length != 4) { showDialogError(tvError, "Enter your 4-digit transaction PIN."); return@setOnClickListener }
            if (SessionManager.hash(pin) != user.hashedPin) { showDialogError(tvError, "Incorrect PIN. Try again."); return@setOnClickListener }

            balance -= amt
            session.updateBalance(balance)
            session.addTransaction("Airtime", amt.toDouble(), nowString(), "out", network, phone)
            updateBalanceDisplay()
            renderTransactions()
            dialog.dismiss()
            Toast.makeText(this, "Airtime of ₦${format(amt.toDouble())} sent to $phone.", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showDataDialog(presetNetwork: String?, presetPlanLabel: String?, presetPlanPrice: Int?) {
        val user = session.getCurrentUser() ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_buy_data)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        var network = presetNetwork ?: "mtn"
        var planLabel: String? = presetPlanLabel
        var planPrice: Int? = presetPlanPrice

        val netButtons = mapOf(
            "mtn" to dialog.findViewById<Button>(R.id.dtNetMtn), "airtel" to dialog.findViewById<Button>(R.id.dtNetAirtel),
            "glo" to dialog.findViewById<Button>(R.id.dtNetGlo), "9mobile" to dialog.findViewById<Button>(R.id.dtNet9mobile)
        )
        fun refreshNetSelection() { netButtons.forEach { (id, btn) -> btn.alpha = if (id == network) 1f else 0.45f } }
        netButtons.forEach { (id, btn) -> btn.setOnClickListener { network = id; refreshNetSelection() } }
        refreshNetSelection()

        val etPhone = dialog.findViewById<EditText>(R.id.dtPhone)
        etPhone.setText(user.phone)

        val plans = mapOf(
            R.id.dtPlan500mb to Pair("500MB", 150), R.id.dtPlan15gb to Pair("1.5GB", 500),
            R.id.dtPlan3gb to Pair("3GB", 1000), R.id.dtPlan10gb to Pair("10GB", 3000)
        )
        fun refreshPlanSelection() {
            plans.forEach { (id, plan) -> dialog.findViewById<LinearLayout>(id).alpha = if (plan.first == planLabel) 1f else 0.6f }
        }
        plans.forEach { (id, plan) ->
            dialog.findViewById<LinearLayout>(id).setOnClickListener {
                planLabel = plan.first; planPrice = plan.second; refreshPlanSelection()
            }
        }
        refreshPlanSelection()

        val etPin = dialog.findViewById<EditText>(R.id.dtPin)
        val tvError = dialog.findViewById<TextView>(R.id.dtError)

        dialog.findViewById<Button>(R.id.btnPayData).setOnClickListener {
            val label = planLabel
            val price = planPrice
            val phone = etPhone.text.toString().trim()
            val pin = etPin.text.toString().trim()

            if (label == null || price == null) { showDialogError(tvError, "Select a data plan."); return@setOnClickListener }
            if (phone.length < 10) { showDialogError(tvError, "Enter a valid phone number."); return@setOnClickListener }
            if (price > balance) { showDialogError(tvError, "Insufficient balance — fund your wallet first."); return@setOnClickListener }
            if (pin.length != 4) { showDialogError(tvError, "Enter your 4-digit transaction PIN."); return@setOnClickListener }
            if (SessionManager.hash(pin) != user.hashedPin) { showDialogError(tvError, "Incorrect PIN. Try again."); return@setOnClickListener }

            balance -= price
            session.updateBalance(balance)
            session.addTransaction("Data · $label", price.toDouble(), nowString(), "out", network, phone)
            updateBalanceDisplay()
            renderTransactions()
            dialog.dismiss()
            Toast.makeText(this, "$label data sent to $phone.", Toast.LENGTH_SHORT).show()
        }

        dialog.show()
    }

    private fun showDialogError(tv: TextView, msg: String) {
        tv.text = msg
        tv.visibility = View.VISIBLE
    }

    // -------------------------------------------------------------------
    // Profile dialogs
    // -------------------------------------------------------------------
    private fun showEditProfileDialog() {
        val user = session.getCurrentUser() ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_edit_profile)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        val etFirst = dialog.findViewById<EditText>(R.id.epFirstName).apply { setText(user.firstName) }
        val etMiddle = dialog.findViewById<EditText>(R.id.epMiddleName).apply { setText(user.middleName) }
        val etLast = dialog.findViewById<EditText>(R.id.epLastName).apply { setText(user.lastName) }
        val etPhone = dialog.findViewById<EditText>(R.id.epPhone).apply { setText(user.phone) }
        val etAddress = dialog.findViewById<EditText>(R.id.epAddress).apply { setText(user.address) }
        val etState = dialog.findViewById<EditText>(R.id.epState).apply { setText(user.state) }

        dialog.findViewById<Button>(R.id.btnSaveProfile).setOnClickListener {
            session.updateProfile(
                etFirst.text.toString().trim(), etMiddle.text.toString().trim(), etLast.text.toString().trim(),
                etPhone.text.toString().trim(), etAddress.text.toString().trim(), etState.text.toString().trim()
            )
            findViewById<TextView>(R.id.tvGreeting).text = "Hi, ${etFirst.text.toString().trim()}"
            dialog.dismiss()
            Toast.makeText(this, "Profile updated.", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun showBeneficiariesDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_beneficiaries)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        val llList = dialog.findViewById<LinearLayout>(R.id.llBeneficiaryList)
        val tvEmpty = dialog.findViewById<TextView>(R.id.tvNoBeneficiaries)
        val etName = dialog.findViewById<EditText>(R.id.benName)
        val etPhone = dialog.findViewById<EditText>(R.id.benPhone)

        fun renderList() {
            llList.removeAllViews()
            val list = session.getCurrentUser()?.beneficiaries ?: emptyList()
            tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
            val inflater = LayoutInflater.from(this)
            list.forEachIndexed { index, b ->
                val row = inflater.inflate(R.layout.item_beneficiary, llList, false)
                row.findViewById<TextView>(R.id.tvBenName).text = b.name
                row.findViewById<TextView>(R.id.tvBenPhone).text = b.phone
                row.findViewById<TextView>(R.id.btnRemoveBen).setOnClickListener {
                    session.removeBeneficiary(index)
                    renderList()
                    Toast.makeText(this, "Beneficiary removed.", Toast.LENGTH_SHORT).show()
                }
                llList.addView(row)
            }
        }
        renderList()

        dialog.findViewById<Button>(R.id.btnAddBeneficiary).setOnClickListener {
            val name = etName.text.toString().trim()
            val phone = etPhone.text.toString().trim()
            if (name.isEmpty() || phone.length < 10) {
                Toast.makeText(this, "Enter a name and valid phone number.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            session.addBeneficiary(name, phone)
            etName.text.clear()
            etPhone.text.clear()
            renderList()
            Toast.makeText(this, "Beneficiary added.", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun showPinChangeDialog() {
        val user = session.getCurrentUser() ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_pin_change)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        val etCurrent = dialog.findViewById<EditText>(R.id.pcCurrent)
        val etNew = dialog.findViewById<EditText>(R.id.pcNew)
        val etConfirm = dialog.findViewById<EditText>(R.id.pcConfirm)
        val tvError = dialog.findViewById<TextView>(R.id.pcError)

        dialog.findViewById<Button>(R.id.btnUpdatePin).setOnClickListener {
            val current = etCurrent.text.toString().trim()
            val next = etNew.text.toString().trim()
            val confirm = etConfirm.text.toString().trim()

            if (SessionManager.hash(current) != user.hashedPin) { showDialogError(tvError, "Current PIN is incorrect."); return@setOnClickListener }
            if (next.length != 4) { showDialogError(tvError, "New PIN must be 4 digits."); return@setOnClickListener }
            if (next != confirm) { showDialogError(tvError, "New PINs do not match."); return@setOnClickListener }

            session.updatePin(SessionManager.hash(next))
            dialog.dismiss()
            Toast.makeText(this, "PIN updated.", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun showNotificationsDialog() {
        val user = session.getCurrentUser() ?: return
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_notifications)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)

        val swTx = dialog.findViewById<Switch>(R.id.swTransactionAlerts).apply { isChecked = user.notifications.transactionAlerts }
        val swPromo = dialog.findViewById<Switch>(R.id.swPromotions).apply { isChecked = user.notifications.promotions }
        val swSecurity = dialog.findViewById<Switch>(R.id.swSecurityAlerts).apply { isChecked = user.notifications.securityAlerts }

        dialog.findViewById<Button>(R.id.btnSaveNotifications).setOnClickListener {
            session.updateNotifications(NotificationPrefs(swTx.isChecked, swPromo.isChecked, swSecurity.isChecked))
            dialog.dismiss()
            Toast.makeText(this, "Preferences saved.", Toast.LENGTH_SHORT).show()
        }
        dialog.show()
    }

    private fun showAboutDialog() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(R.layout.dialog_about)
        dialog.window?.setBackgroundDrawableResource(R.drawable.bg_card_rounded)
        dialog.show()
    }

    // -------------------------------------------------------------------
    // Transactions
    // -------------------------------------------------------------------
    private fun renderTransactions() {
        val txs = session.getTransactions()
        llRecentTx.removeAllViews()
        llHistoryList.removeAllViews()
        tvNoTxHome.visibility = if (txs.isEmpty()) View.VISIBLE else View.GONE
        tvNoTxHistory.visibility = if (txs.isEmpty()) View.VISIBLE else View.GONE

        val inflater = LayoutInflater.from(this)
        txs.take(4).forEach { tx -> llRecentTx.addView(buildTxRow(inflater, llRecentTx, tx)) }
        txs.forEach { tx -> llHistoryList.addView(buildTxRow(inflater, llHistoryList, tx)) }
    }

    private fun buildTxRow(inflater: LayoutInflater, parent: LinearLayout, tx: Transaction): View {
        val row = inflater.inflate(R.layout.item_transaction, parent, false)
        val isIn = tx.direction == "in"
        val netLabel = tx.network?.let { " · ${it.uppercase()}" } ?: ""
        row.findViewById<TextView>(R.id.tvTxLabel).text = tx.label + netLabel
        val phoneLabel = tx.phone?.let { " · $it" } ?: ""
        row.findViewById<TextView>(R.id.tvTxTime).text = tx.time + phoneLabel
        val amountView = row.findViewById<TextView>(R.id.tvTxAmount)
        amountView.text = (if (isIn) "+₦" else "-₦") + format(tx.amount)
        amountView.setTextColor(getColor(if (isIn) R.color.green_primary else R.color.spend_color))
        return row
    }

    private fun nowString(): String = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date())

    private fun format(value: Double): String {
        val nf = NumberFormat.getNumberInstance(Locale("en", "NG"))
        nf.minimumFractionDigits = 2
        nf.maximumFractionDigits = 2
        return nf.format(value)
    }
}
