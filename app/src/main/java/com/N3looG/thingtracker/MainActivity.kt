package com.N3looG.thingtracker

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.N3looG.thingtracker.model.LoanType
import com.N3looG.thingtracker.ui.screens.AddLoanScreen
import com.N3looG.thingtracker.ui.screens.HomeScreen
import com.N3looG.thingtracker.ui.theme.ThingTrackerTheme
import com.N3looG.thingtracker.viewmodel.LoanViewModel
import com.google.android.gms.ads.MobileAds

class MainActivity : AppCompatActivity() {

    private val viewModel: LoanViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        MobileAds.initialize(this) {}
        enableEdgeToEdge()
        setContent {
            ThingTrackerTheme {
                var currentScreen by remember { mutableStateOf("home") }
                var initialLoanType by remember { mutableStateOf(LoanType.LENT) }

                when (currentScreen) {
                    "home" -> HomeScreen(
                        viewModel = viewModel,
                        onNavigateToAdd = { activeType ->
                            initialLoanType = activeType
                            currentScreen = "add"
                        },
                        onExitApp = { finishAndRemoveTask() }
                    )
                    "add" -> AddLoanScreen(
                        viewModel = viewModel,
                        initialType = initialLoanType,
                        onNavigateBack = { currentScreen = "home" }
                    )
                }
            }
        }
    }
}
