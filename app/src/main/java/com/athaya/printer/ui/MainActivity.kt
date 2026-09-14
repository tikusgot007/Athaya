package com.athaya.printer.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

/**
 * Single-activity host. No database, no login, no backend: navigation and
 * all state live purely in memory for the lifetime of this activity.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface {
                    val navController = rememberNavController()
                    NavHost(navController = navController, startDestination = Routes.HOME) {
                        composable(Routes.HOME) {
                            HomeScreen(
                                onCetakNota = { navController.navigate(Routes.NOTA) },
                                onCetakAlamat = { navController.navigate(Routes.ADDRESS) },
                                onPengaturanPrinter = { navController.navigate(Routes.PRINTER_SETTINGS) },
                            )
                        }
                        composable(Routes.NOTA) {
                            NotaScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.ADDRESS) {
                            AddressScreen(onBack = { navController.popBackStack() })
                        }
                        composable(Routes.PRINTER_SETTINGS) {
                            PrinterSettingsScreen(onBack = { navController.popBackStack() })
                        }
                    }
                }
            }
        }
    }
}

object Routes {
    const val HOME = "home"
    const val NOTA = "nota"
    const val ADDRESS = "address"
    const val PRINTER_SETTINGS = "printer_settings"
}
