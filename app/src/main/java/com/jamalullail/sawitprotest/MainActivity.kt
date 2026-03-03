package com.jamalullail.sawitprotest

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.room.Room
import com.google.firebase.firestore.FirebaseFirestore
import com.jamalullail.sawitprotest.data.local.AppDatabase
import com.jamalullail.sawitprotest.data.repository.TicketRepository
import com.jamalullail.sawitprotest.ui.TicketFormScreen
import com.jamalullail.sawitprotest.ui.TicketListScreen
import com.jamalullail.sawitprotest.ui.TicketViewModel
import com.jamalullail.sawitprotest.ui.theme.SawitProTestTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "sawit-pro-db"
        ).build()
        
        // Initialize Firestore
        val firestore = FirebaseFirestore.getInstance()
        val repository = TicketRepository(db.ticketDao(), firestore)
        val viewModel = TicketViewModel(repository)

        setContent {
            SawitProTestTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    WeighbridgeNavHost(viewModel)
                }
            }
        }
    }
}

@Composable
fun WeighbridgeNavHost(viewModel: TicketViewModel) {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "ticket_list"
    ) {
        composable("ticket_list") {
            TicketListScreen(
                viewModel = viewModel,
                onAddTicket = { navController.navigate("ticket_form") },
                onEditTicket = { id -> navController.navigate("ticket_form?ticketId=$id") }
            )
        }
        composable(
            route = "ticket_form?ticketId={ticketId}",
            arguments = listOf(navArgument("ticketId") { 
                type = NavType.StringType
                nullable = true
                defaultValue = null 
            })
        ) { backStackEntry ->
            val ticketId = backStackEntry.arguments?.getString("ticketId")
            TicketFormScreen(
                ticketId = ticketId,
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
