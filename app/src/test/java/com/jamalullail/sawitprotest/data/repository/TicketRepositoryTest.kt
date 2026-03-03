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
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketRepositoryTest {

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
    fun `saveTicket saves to local Room before attempting Firebase sync`() = runTest {
        // Arrange
        val license = "B 1234 ABC"
        val driver = "John Doe"
        
        // Act
        repository.saveTicket(licenseNumber = license, driverName = driver, inboundWeight = 1000.0, outboundWeight = 500.0)

        // Assert
        coVerify(ordering = Ordering.SEQUENCE) {
            ticketDao.insertTicket(any()) // 1st: Local Save
            collectionRef.document(any()) // 2nd: Remote Sync
        }
    }

    @Test
    fun `saveTicket calculates net weight correctly`() = runTest {
        // Arrange
        val slot = slot<TicketEntity>()
        coEvery { ticketDao.insertTicket(capture(slot)) } just Runs
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Act
        repository.saveTicket(licenseNumber = "B 1234 ABC", driverName = "Driver", inboundWeight = 150.5, outboundWeight = 50.5)

        // Assert
        assertTrue("Slot should be captured", slot.isCaptured)
        assertEquals(100.0, slot.captured.netWeight, 0.1)
    }

    @Test
    fun `saveTicket returns success true when Firestore sync succeeds`() = runTest {
        // Arrange
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Act
        val result = repository.saveTicket(licenseNumber = "B 1234 ABC", driverName = "Driver", inboundWeight = 100.0, outboundWeight = 50.0)

        // Assert
        assertTrue(result.getOrThrow()) // syncSuccess == true
        coVerify { ticketDao.updateTicket(match { it.isSynced }) }
    }

    @Test
    fun `saveTicket returns success false when Firestore sync fails but Room still has data`() = runTest {
        // Arrange
        every { docRef.set(any()) } returns Tasks.forException(Exception("Network Error"))

        // Act
        val result = repository.saveTicket(licenseNumber = "B 1234 ABC", driverName = "Driver", inboundWeight = 100.0, outboundWeight = 50.0)

        // Assert
        assertTrue(result.isSuccess) // Repository call succeeded (local save worked)
        assertFalse(result.getOrThrow()) // syncSuccess == false
        coVerify { ticketDao.insertTicket(any()) } // Verified local persistence
    }

    @Test
    fun `syncUnsyncedTickets calls syncTicketToFirestore for each unsynced ticket`() = runTest {
        // Arrange
        val unsyncedTickets = listOf(
            TicketEntity("1", 0L, "B 123 ABC", "D1", 100.0, 50.0, 50.0, false),
            TicketEntity("2", 0L, "B 456 DEF", "D2", 200.0, 100.0, 100.0, false)
        )
        coEvery { ticketDao.getUnsyncedTickets() } returns unsyncedTickets
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Act
        repository.syncUnsyncedTickets()

        // Assert
        coVerify(exactly = 2) { docRef.set(any()) }
        coVerify(exactly = 2) { ticketDao.updateTicket(match { it.isSynced }) }
    }
}
