package com.jamalullail.sawitprotest.data.repository

import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.jamalullail.sawitprotest.data.local.TicketDao
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseSyncTest {

    private lateinit var ticketDao: TicketDao
    private lateinit var firestore: FirebaseFirestore
    private lateinit var ticketsRef: CollectionReference
    private lateinit var docRef: DocumentReference
    private lateinit var repository: TicketRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        mockkStatic(Log::class)
        every { Log.e(any(), any(), any()) } returns 0
        every { Log.d(any(), any()) } returns 0

        ticketDao = mockk<TicketDao>(relaxed = true)
        firestore = mockk<FirebaseFirestore>(relaxed = true)
        ticketsRef = mockk<CollectionReference>(relaxed = true)
        docRef = mockk<DocumentReference>(relaxed = true)

        every { firestore.collection("tickets") } returns ticketsRef
        every { ticketsRef.document(any()) } returns docRef
        
        repository = TicketRepository(ticketDao, firestore, testDispatcher)
    }

    @Test
    fun `when firebase sync fails saveTicket still returns success for local storage`() = runTest(testDispatcher) {
        // Arrange
        every { docRef.set(any()) } returns Tasks.forException(Exception("Firebase Error"))

        // Act
        val result = repository.saveTicket(
            licenseNumber = "B 1234 ABC",
            driverName = "John Doe",
            inboundWeight = 1000.0,
            outboundWeight = 800.0
        )

        // Assert
        assertTrue("Local save should succeed even if sync fails", result.isSuccess)
        assertFalse("Sync status should be false", result.getOrThrow())
        coVerify { ticketDao.insertTicket(any()) }
    }

    @Test
    fun `when firebase sync succeeds isSynced is updated in Room`() = runTest(testDispatcher) {
        // Arrange
        every { docRef.set(any()) } returns Tasks.forResult(null)

        // Act
        val result = repository.saveTicket(
            licenseNumber = "B 1234 ABC",
            driverName = "John Doe",
            inboundWeight = 1000.0,
            outboundWeight = 800.0
        )

        // Assert
        assertTrue("Repository call should be successful", result.isSuccess)
        assertTrue("Sync status should be true", result.getOrThrow())
        coVerify { ticketDao.updateTicket(match { it.isSynced }) }
    }
}
