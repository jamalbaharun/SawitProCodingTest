package com.jamalullail.sawitprotest.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jamalullail.sawitprotest.R
import com.jamalullail.sawitprotest.data.local.TicketEntity
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketListScreen(
    viewModel: TicketViewModel,
    onAddTicket: () -> Unit,
    onEditTicket: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unsyncedCount by viewModel.unsyncedCount.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ListTopBar(unsyncedCount, onSync = viewModel::syncNow, onSort = viewModel::updateSortOrder)
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAddTicket) {
                Icon(Icons.Default.Add, contentDescription = "Add Ticket")
            }
        }
    ) { padding ->
        ListContent(padding, uiState, onEditTicket)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ListTopBar(unsyncedCount: Int, onSync: () -> Unit, onSort: (String) -> Unit) {
    var showSortMenu by remember { mutableStateOf(false) }
    LargeTopAppBar(
        title = { Text(stringResource(R.string.app_name)) },
        actions = {
            if (unsyncedCount > 0) {
                IconButton(onClick = onSync) {
                    BadgedBox(badge = { Badge { Text(unsyncedCount.toString()) } }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.btn_sync_now))
                    }
                }
            }
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.Default.MoreVert, contentDescription = "Sort")
            }
            DropdownMenu(expanded = showSortMenu, onDismissRequest = { showSortMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sort_date)) },
                    onClick = { onSort("date"); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sort_driver)) },
                    onClick = { onSort("driverName"); showSortMenu = false }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.menu_sort_license)) },
                    onClick = { onSort("licenseNumber"); showSortMenu = false }
                )
            }
        }
    )
}

@Composable
private fun ListContent(padding: PaddingValues, state: TicketUiState, onEdit: (String) -> Unit) {
    when (state) {
        is TicketUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        is TicketUiState.Success -> {
            LazyColumn(
                contentPadding = padding,
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Fix for Unused Import: Use member function items(count)
                items(state.tickets.size) { index ->
                    val ticket = state.tickets[index]
                    TicketItem(ticket = ticket, onClick = { onEdit(ticket.id) })
                }
            }
        }
        is TicketUiState.Error -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text(state.message, color = MaterialTheme.colorScheme.error)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketItem(ticket: TicketEntity, onClick: () -> Unit) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }
    OutlinedCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = ticket.licenseNumber, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (!ticket.isSynced) {
                    Text(stringResource(R.string.status_pending_sync), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
                Text(text = dateFormat.format(Date(ticket.dateTime)), style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "Driver: ${ticket.driverName}", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(R.string.unit_kg, ticket.netWeight.toString()),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TicketFormScreen(
    ticketId: String?,
    viewModel: TicketViewModel,
    onNavigateBack: () -> Unit
) {
    val saveState by viewModel.saveState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.errorMessage.collectLatest { message -> snackbarHostState.showSnackbar(message) }
    }

    LaunchedEffect(ticketId) {
        if (ticketId != null) {
            viewModel.getTicketById(ticketId)?.let { viewModel.setFormData(it) }
        } else {
            viewModel.clearFormData()
        }
    }

    LaunchedEffect(saveState) {
        if (saveState is SaveTicketUiState.Success) {
            viewModel.resetSaveState()
            onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (ticketId == null) R.string.title_new_ticket else R.string.title_edit_ticket)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        }
    ) { padding ->
        TicketFormContent(Modifier.padding(padding), ticketId, viewModel, saveState)
    }
}

@Composable
private fun TicketFormContent(
    modifier: Modifier,
    ticketId: String?,
    viewModel: TicketViewModel,
    saveState: SaveTicketUiState
) {
    val licenseNumber by viewModel.licenseNumber.collectAsStateWithLifecycle()
    val driverName by viewModel.driverName.collectAsStateWithLifecycle()
    val inboundWeight by viewModel.inboundWeight.collectAsStateWithLifecycle()
    val outboundWeight by viewModel.outboundWeight.collectAsStateWithLifecycle()
    val netWeight by viewModel.netWeight.collectAsStateWithLifecycle()

    val isWeightError = remember(inboundWeight, outboundWeight) {
        (outboundWeight.toDoubleOrNull() ?: 0.0) > (inboundWeight.toDoubleOrNull() ?: 0.0)
    }

    Column(modifier.padding(16.dp).fillMaxSize(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        OutlinedTextField(
            value = licenseNumber,
            onValueChange = { viewModel.licenseNumber.value = it.uppercase() },
            label = { Text(stringResource(R.string.label_license_number)) },
            modifier = Modifier.fillMaxWidth(),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
            isError = licenseNumber.isEmpty()
        )

        OutlinedTextField(
            value = driverName,
            onValueChange = { viewModel.driverName.value = it },
            label = { Text(stringResource(R.string.label_driver_name)) },
            modifier = Modifier.fillMaxWidth(),
            isError = driverName.isEmpty()
        )

        WeightInputs(viewModel, isWeightError)
        NetWeightCard(netWeight)

        Spacer(modifier = Modifier.weight(1f))

        if (saveState is SaveTicketUiState.Error) {
            Text(saveState.message, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(bottom = 8.dp))
        }

        SaveButton(
            isLoading = saveState is SaveTicketUiState.Loading,
            enabled = licenseNumber.isNotEmpty() && driverName.isNotEmpty() && !isWeightError,
            onClick = { viewModel.saveTicket(ticketId) }
        )
    }
}

@Composable
private fun WeightInputs(viewModel: TicketViewModel, isWeightError: Boolean) {
    val inboundWeight by viewModel.inboundWeight.collectAsStateWithLifecycle()
    val outboundWeight by viewModel.outboundWeight.collectAsStateWithLifecycle()

    OutlinedTextField(
        value = inboundWeight,
        onValueChange = { viewModel.inboundWeight.value = it },
        label = { Text(stringResource(R.string.label_inbound_weight)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )

    OutlinedTextField(
        value = outboundWeight,
        onValueChange = { viewModel.outboundWeight.value = it },
        label = { Text(stringResource(R.string.label_outbound_weight)) },
        modifier = Modifier.fillMaxWidth(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isWeightError,
        supportingText = { if (isWeightError) Text(stringResource(R.string.error_weight_mismatch), color = MaterialTheme.colorScheme.error) }
    )
}

@Composable
private fun NetWeightCard(netWeight: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(stringResource(R.string.label_net_weight), style = MaterialTheme.typography.labelMedium)
            Text(
                stringResource(R.string.unit_kg, netWeight.toString()),
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun SaveButton(isLoading: Boolean, enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth(), enabled = !isLoading && enabled) {
        if (isLoading) {
            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
        } else {
            Text(stringResource(R.string.btn_save_ticket))
        }
    }
}
