package com.jamalullail.sawitprotest.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

/**
 * Main database for the Weighbridge app.
 */
@Database(entities = [TicketEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    /**
     * Provides access to Ticket related database operations.
     */
    abstract fun ticketDao(): TicketDao
}
