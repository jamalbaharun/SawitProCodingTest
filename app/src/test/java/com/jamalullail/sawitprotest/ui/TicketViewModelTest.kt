package com.jamalullail.sawitprotest.ui

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import com.jamalullail.sawitprotest.data.local.TicketEntity
import com.jamalullail.sawitprotest.data.repository.TicketRepository
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TicketViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private val repository = mockk<TicketRepository>(relaxed = true)
    private lateinit var viewModel: TicketViewModel
    
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        // Ensure we mock the correct repository methods used in init/observeTickets
        every { repository.getAllTickets(any(), any()) } returns flowOf(emptyList())
        every { repository.getUnsyncedCount() } returns flowOf(0)
        
        // Suspend functions returning Result should be explicitly mocked to avoid ClassCastException in relaxed mocks
        coEvery { repository.saveTicket(any(), any(), any(), any(), any()) } returns Result.success(true)
        
        viewModel = TicketViewModel(repository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `netWeight updates correctly when inbound or outbound changes`() = runTest {
        viewModel.netWeight.test {
            // Initial value
            assertEquals(0.0, awaitItem(), 0.0)

            viewModel.inboundWeight.value = "1000"
            assertEquals(1000.0, awaitItem(), 0.0)

            viewModel.outboundWeight.value = "200"
            assertEquals(800.0, awaitItem(), 0.0)
        }
    }

    @Test
    fun `saveTicket triggers error when outbound weight is greater than inbound`() = runTest {
        // Arrange: Mock the repository to return the specific validation error
        val errorMsg = "Outbound weight cannot exceed inbound weight"
        coEvery { 
            repository.saveTicket(any(), any(), any(), any(), any()) 
        } returns Result.failure(IllegalArgumentException(errorMsg))

        viewModel.inboundWeight.value = "500"
        viewModel.outboundWeight.value = "1000"
        viewModel.licenseNumber.value = "B 1234 ABC"
        viewModel.driverName.value = "John"

        // Act
        viewModel.saveTicket()
        advanceUntilIdle()

        // Assert
        val state = viewModel.saveState.value
        assertTrue("Expected Error state but was $state", state is SaveTicketUiState.Error)
        assertEquals(errorMsg, (state as SaveTicketUiState.Error).message)
    }

    @Test
    fun `saveTicket triggers error when license is too short`() = runTest {
        // Arrange
        val errorMsg = "Invalid License Number format"
        coEvery { 
            repository.saveTicket(any(), any(), any(), any(), any()) 
        } returns Result.failure(IllegalArgumentException(errorMsg))

        viewModel.licenseNumber.value = "B"
        viewModel.driverName.value = "John"
        viewModel.inboundWeight.value = "1000"
        viewModel.outboundWeight.value = "500"

        // Act
        viewModel.saveTicket()
        advanceUntilIdle()

        // Assert
        val state = viewModel.saveState.value
        assertTrue("Expected Error state but was $state", state is SaveTicketUiState.Error)
        assertEquals(errorMsg, (state as SaveTicketUiState.Error).message)
    }

    @Test
    fun `uiState emits success when repository returns tickets`() = runTest {
        // Arrange
        val tickets = listOf(
            TicketEntity("1", 1000L, "B 123", "Driver 1", 100.0, 50.0, 50.0, true)
        )
        every { repository.getAllTickets(any(), any()) } returns flowOf(tickets)
        
        // Re-init to trigger observeTickets with mocked data
        val newViewModel = TicketViewModel(repository)

        // Act & Assert using Turbine
        newViewModel.uiState.test {
            assertEquals(TicketUiState.Loading, awaitItem())
            val successState = awaitItem() as TicketUiState.Success
            assertEquals(tickets, successState.tickets)
        }
    }

    @Test
    fun `updateSortOrder updates sorting and re-fetches tickets`() = runTest {
        // Act
        viewModel.updateSortOrder("driverName")
        advanceUntilIdle()
        
        // Assert
        verify { repository.getAllTickets(any(), "driverName") }
    }
}
