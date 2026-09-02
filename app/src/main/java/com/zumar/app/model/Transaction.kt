package com.zumar.app.model

data class Transaction(
    val label: String,
    val amount: Double,
    val time: String,
    val direction: String = "out", // "in" (funding) or "out" (airtime/data purchase)
    val network: String? = null,
    val phone: String? = null
)
