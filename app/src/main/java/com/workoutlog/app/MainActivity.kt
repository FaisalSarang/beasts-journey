package com.workoutlog.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.workoutlog.app.ui.AppViewModel
import com.workoutlog.app.ui.EditorScreen
import com.workoutlog.app.ui.HomeScreen
import com.workoutlog.app.ui.ProgressScreen
import com.workoutlog.app.ui.Screen
import com.workoutlog.app.ui.SessionScreen
import com.workoutlog.app.ui.theme.BeastsJourneyTheme

class MainActivity : ComponentActivity() {
    private val vm: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BeastsJourneyTheme { App(vm) }
        }
    }

    override fun onStop() {
        super.onStop()
        vm.flush()
    }
}

@Composable
private fun App(vm: AppViewModel) {
    val data by vm.data.collectAsStateWithLifecycle()
    val screen = vm.backStack.last()

    BackHandler(enabled = vm.backStack.size > 1) { vm.back() }

    when (screen) {
        Screen.Home -> HomeScreen(data, vm)
        is Screen.Editor -> EditorScreen(data.workouts.firstOrNull { it.id == screen.workoutId }, vm)
        Screen.Session -> {
            val active = data.active
            if (active == null) LaunchedEffect(Unit) { vm.back() } else SessionScreen(active, data, vm)
        }
        Screen.Progress -> ProgressScreen(data, vm)
    }
}
