package com.zumar.app.model

data class NotificationPrefs(
    val transactionAlerts: Boolean = true,
    val promotions: Boolean = true,
    val securityAlerts: Boolean = true
)
