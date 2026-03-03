package com.jamalullail.sawitprotest.data.repository

import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jamalullail.sawitprotest.data.local.TicketDao
import com.jamalullail.sawitprotest.data.local.TicketEntity
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketRepositorySyncTest {

    private lateinit var ticketDao: TicketDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var collectionRef: CollectionReference
    private lateinit var docRef: DocumentReference
    private lateinit var repository: TicketRepository

    @Before
    fun setup() {
        ticketDao = mockk<TicketDao>(relaxed = true)
        firestore = mockk<FirebaseFirestore>(relaxed = true)
        collectionRef = mockk<CollectionReference>(relaxed = true)
        docRef = mockk<DocumentReference>(relaxed = true)
        
        every { firestore.collection("tickets") } returns collectionRef
        every { collectionRef.document(any()) } returns docRef
        
        repository = TicketRepository(ticketDao, firestore)
    }

    @Test
    fun `saveTicket local success even if sync fails`() = runTest {
        // Arrange
        every { docRef.set(any()) } returns Tasks.forException(Exception("Network error"))
        
        // Act
        val result = repository.saveTicket(
            licenseNumber = "B 1234 ABC",
            driverName = "John",
            inboundWeight = 1000.0,
            outboundWeight = 800.0
        )

        // Assert
        coVerify { ticketDao.insertTicket(any()) } // Locally saved
        assertTrue("Local save should be successful", result.isSuccess) 
        assertFalse("Sync should be reported as failed", result.getOrThrow())
    }

    @Test
    fun `syncUnsyncedTickets updates room on success`() = runTest {
        // Arrange
        val unsyncedTicket = TicketEntity(
            id = "1", dateTime = 0L, licenseNumber = "B 1234 ABC", driverName = "B",
            inboundWeight = 10.0, outboundWeight = 5.0, netWeight = 5.0, isSynced = false
        )
        coEvery { ticketDao.getUnsyncedTickets() } returns listOf(unsyncedTicket)
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Act
        repository.syncUnsyncedTickets()

        // Assert
        coVerify { ticketDao.updateTicket(match { it.isSynced }) }
    }
}
