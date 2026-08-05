package com.example.hapticball

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hapticball.ui.theme.PendulumScreen
import com.example.hapticball.ui.theme.PuzzleScreen
import com.example.hapticball.ui.theme.Sandbox2DScreen
import com.example.hapticball.ui.theme.TutorialOverlay

import androidx.core.content.edit

enum class Minigame(val displayName: String) {
    SANDBOX("SANDBOX"),
    PENDULUM("PENDULUM"),
    PUZZLE("LEVELS")
}

enum class ThemeMode {
    LIGHT, DARK, AUTO
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val sharedPref = getPreferences(MODE_PRIVATE)
        val savedMinigame = sharedPref.getString("last_minigame", Minigame.SANDBOX.name)
        val initialMinigame = try { Minigame.valueOf(savedMinigame!!) } catch(_: Exception) { Minigame.SANDBOX }
        val showTutorialInitial = sharedPref.getBoolean("show_tutorial", true)

        setContent {
            var themeMode by remember { mutableStateOf(ThemeMode.AUTO) }
            val systemDark = isSystemInDarkTheme()
            val isDark = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.AUTO -> systemDark
            }
            
            val backgroundColor = if (isDark) Color.Black else Color.White
            var currentMinigame by remember { mutableStateOf(initialMinigame) }
            var showTutorial by remember { mutableStateOf(showTutorialInitial) }

            LaunchedEffect(currentMinigame) {
                sharedPref.edit {
                    putString("last_minigame", currentMinigame.name)
                }
            }

            Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Minigame screen fills the entire background
                    Box(modifier = Modifier.fillMaxSize()) {
                        Crossfade(targetState = currentMinigame, label = "MinigameTransition") { screen ->
                            when (screen) {
                                Minigame.SANDBOX -> Sandbox2DScreen(isDark = isDark)
                                Minigame.PENDULUM -> PendulumScreen(isDark = isDark)
                                Minigame.PUZZLE -> PuzzleScreen(isDark = isDark)
                            }
                        }
                    }

                    // UI Overlays on top
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MinigameSelector(
                            selected = currentMinigame,
                            onSelected = { currentMinigame = it },
                            isDark = isDark
                        )

                        ThemeToggle(
                            mode = themeMode,
                            onModeChange = { themeMode = it },
                            isDark = isDark
                        )
                    }

                    if (showTutorial) {
                        TutorialOverlay(isDark = isDark) { dontShowAgain ->
                            showTutorial = false
                            if (dontShowAgain) {
                                sharedPref.edit { putBoolean("show_tutorial", false) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ThemeToggle(
    mode: ThemeMode,
    onModeChange: (ThemeMode) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val icon = when (mode) {
        ThemeMode.LIGHT -> Icons.Default.LightMode
        ThemeMode.DARK -> Icons.Default.DarkMode
        ThemeMode.AUTO -> Icons.Default.BrightnessAuto
    }
    
    IconButton(
        onClick = {
            val next = when (mode) {
                ThemeMode.LIGHT -> ThemeMode.DARK
                ThemeMode.DARK -> ThemeMode.AUTO
                ThemeMode.AUTO -> ThemeMode.LIGHT
            }
            onModeChange(next)
        },
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Toggle Theme",
            tint = if (isDark) Color.White else Color.Black
        )
    }
}

@Composable
fun MinigameSelector(
    selected: Minigame,
    onSelected: (Minigame) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier,
) {
    val textColor = if (isDark) Color.White else Color.Black
    val inactiveColor = Color.Gray

    LazyRow(
        modifier = modifier.wrapContentWidth(),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(Minigame.entries.filter { it != Minigame.PENDULUM }) { minigame ->
            Text(
                text = minigame.displayName,
                fontSize = if (selected == minigame) 24.sp else 18.sp,
                fontWeight = if (selected == minigame) FontWeight.Bold else FontWeight.Normal,
                color = if (selected == minigame) textColor else inactiveColor,
                modifier = Modifier
                    .padding(end = 16.dp, top = 4.dp, bottom = 4.dp)
                    .clickable { onSelected(minigame) }
            )
        }
    }
}
