package com.jamalullail.sawitprotest.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TicketDao {
    @Query("""
        SELECT * FROM tickets 
        WHERE (:query = '' OR driverName LIKE '%' || :query || '%' OR licenseNumber LIKE '%' || :query || '%')
        ORDER BY 
            CASE WHEN :sortBy = 'date' THEN dateTime END DESC,
            CASE WHEN :sortBy = 'driverName' THEN driverName END ASC,
            CASE WHEN :sortBy = 'licenseNumber' THEN licenseNumber END ASC,
            dateTime DESC
    """)
    fun getTickets(query: String, sortBy: String): Flow<List<TicketEntity>>

    @Query("SELECT * FROM tickets WHERE id = :id")
    suspend fun getTicketById(id: String): TicketEntity?

    @Query("SELECT COUNT(*) FROM tickets WHERE isSynced = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTicket(ticket: TicketEntity)

    @Query("SELECT * FROM tickets WHERE isSynced = 0")
    suspend fun getUnsyncedTickets(): List<TicketEntity>

    @Update
    suspend fun updateTicket(ticket: TicketEntity)
}
