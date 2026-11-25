package com.example.doctracker

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

class MainActivity : ComponentActivity() {
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= 33) notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF1565C0))) { AppNav() }
        }
    }
}

@Composable
fun AppNav(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val idArg = listOf(navArgument("id") { type = NavType.LongType })

    NavHost(nav, startDestination = "list") {
        composable("list") {
            ListScreen(vm,
                onOpen = { nav.navigate("detail/$it") },
                onAdd = { nav.navigate("edit/0") },
                onCategories = { nav.navigate("categories") })
        }
        composable("detail/{id}", idArg) {
            DetailScreen(vm, it.arguments!!.getLong("id"),
                onBack = { nav.popBackStack() },
                onEdit = { id -> nav.navigate("edit/$id") })
        }
        composable("edit/{id}", idArg) {
            EditScreen(vm, it.arguments!!.getLong("id"), onDone = { nav.popBackStack() })
        }
        composable("categories") { CategoriesScreen(vm, onBack = { nav.popBackStack() }) }
    }
}
