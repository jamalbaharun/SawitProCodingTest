package com.jamalullail.sawitprotest.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tickets")
data class TicketEntity(
    @PrimaryKey val id: String,
    val dateTime: Long,
    val licenseNumber: String,
    val driverName: String,
    val inboundWeight: Double,
    val outboundWeight: Double,
    val netWeight: Double,
    val isSynced: Boolean = false
)
