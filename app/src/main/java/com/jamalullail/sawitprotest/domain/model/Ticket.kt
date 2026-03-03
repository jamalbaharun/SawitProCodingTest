package com.jamalullail.sawitprotest.domain.model

data class Ticket(
    val id: String,
    val dateTime: Long,
    val licenseNumber: String,
    val driverName: String,
    val inboundWeight: Double,
    val outboundWeight: Double,
    val netWeight: Double,
    val isSynced: Boolean
)
