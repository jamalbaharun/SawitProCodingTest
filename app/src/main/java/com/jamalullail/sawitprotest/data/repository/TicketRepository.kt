package com.jamalullail.sawitprotest.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.snapshots
import com.jamalullail.sawitprotest.data.local.TicketDao
import com.jamalullail.sawitprotest.data.local.TicketEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID

class TicketRepository(
    private val ticketDao: TicketDao,
    private val firestore: FirebaseFirestore,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val ticketsCollection = firestore.collection("tickets")

    fun startRealtimeSync(scope: CoroutineScope) {
        ticketsCollection.snapshots()
            .map { querySnapshot ->
                querySnapshot.documents.mapNotNull { doc ->
                    createEntityFromDocument(doc)
                }
            }
            .onEach { tickets ->
                persistRemoteTickets(scope, tickets)
            }
            .launchIn(scope)
    }

    private fun createEntityFromDocument(doc: com.google.firebase.firestore.DocumentSnapshot): TicketEntity? {
        return try {
            TicketEntity(
                id = doc.getString("id") ?: return null,
                dateTime = doc.getLong("dateTime") ?: 0L,
                licenseNumber = doc.getString("licenseNumber") ?: "",
                driverName = doc.getString("driverName") ?: "",
                inboundWeight = doc.getDouble("inboundWeight") ?: 0.0,
                outboundWeight = doc.getDouble("outboundWeight") ?: 0.0,
                netWeight = doc.getDouble("netWeight") ?: 0.0,
                isSynced = !doc.metadata.hasPendingWrites()
            )
        } catch (e: Exception) {
            Log.e("TicketRepository", "Error parsing doc: ${doc.id}", e)
            null
        }
    }

    private fun persistRemoteTickets(scope: CoroutineScope, tickets: List<TicketEntity>) {
        scope.launch(ioDispatcher) {
            for (ticket in tickets) {
                val localTicket = ticketDao.getTicketById(ticket.id)
                if (localTicket == null || localTicket.isSynced || ticket.isSynced) {
                    ticketDao.insertTicket(ticket)
                }
            }
        }
    }

    fun getAllTickets(query: String = "", sortBy: String = "date"): Flow<List<TicketEntity>> {
        return ticketDao.getTickets(query, sortBy)
    }

    suspend fun getTicketById(id: String): TicketEntity? = withContext(ioDispatcher) {
        ticketDao.getTicketById(id)
    }

    fun getUnsyncedCount(): Flow<Int> = ticketDao.getUnsyncedCount()

    suspend fun saveTicket(
        id: String? = null,
        licenseNumber: String,
        driverName: String,
        inboundWeight: Double,
        outboundWeight: Double
    ): Result<Boolean> = withContext(ioDispatcher) {
        try {
            validateInputs(licenseNumber, inboundWeight, outboundWeight)
            val ticket = createTicketEntity(id, licenseNumber, driverName, inboundWeight, outboundWeight)
            
            ticketDao.insertTicket(ticket)
            val syncSuccess = attemptSync(ticket)
            
            Result.success(syncSuccess)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun validateInputs(license: String, inbound: Double, outbound: Double) {
        val indonesianPlateRegex = Regex("^[A-Z]{1,2}\\s?\\d{1,4}\\s?[A-Z]{1,3}$")
        
        require(license.isNotBlank() && license.uppercase().matches(indonesianPlateRegex)) { 
            "Invalid License Plate format. Expected format like 'B 1234 RZK'" 
        }
        require(inbound >= 0 && outbound >= 0) { "Weights must be non-negative" }
        require(outbound <= inbound) { "Outbound weight cannot exceed inbound weight" }
    }

    private fun createTicketEntity(
        id: String?,
        license: String,
        driver: String,
        inbound: Double,
        outbound: Double
    ): TicketEntity {
        return TicketEntity(
            id = id ?: UUID.randomUUID().toString(),
            dateTime = System.currentTimeMillis(),
            licenseNumber = license.uppercase(),
            driverName = driver,
            inboundWeight = inbound,
            outboundWeight = outbound,
            netWeight = inbound - outbound,
            isSynced = false
        )
    }

    private suspend fun attemptSync(ticket: TicketEntity): Boolean {
        return try {
            withTimeoutOrNull(5000) {
                syncTicketToFirestore(ticket)
                true
            } ?: false
        } catch (e: Exception) {
            Log.e("TicketRepository", "Sync error", e)
            false
        }
    }

    private suspend fun syncTicketToFirestore(ticket: TicketEntity) {
        val ticketMap = mapOf(
            "id" to ticket.id,
            "dateTime" to ticket.dateTime,
            "licenseNumber" to ticket.licenseNumber,
            "driverName" to ticket.driverName,
            "inboundWeight" to ticket.inboundWeight,
            "outboundWeight" to ticket.outboundWeight,
            "netWeight" to ticket.netWeight,
            "isSynced" to true
        )
        ticketsCollection.document(ticket.id).set(ticketMap).await()
        ticketDao.updateTicket(ticket.copy(isSynced = true))
    }

    suspend fun syncUnsyncedTickets(): Result<Unit> = withContext(ioDispatcher) {
        try {
            val unsynced = ticketDao.getUnsyncedTickets()
            if (unsynced.isEmpty()) return@withContext Result.success(Unit)
            
            var hasError = false
            for (ticket in unsynced) {
                if (!attemptSync(ticket)) hasError = true
            }
            
            if (hasError) Result.failure(Exception("One or more tickets failed to sync"))
            else Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
