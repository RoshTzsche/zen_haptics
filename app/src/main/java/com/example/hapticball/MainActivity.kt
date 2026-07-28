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
import androidx.compose.material3.MaterialTheme
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
import com.example.hapticball.ui.theme.Sandbox2DScreen
import com.example.hapticball.ui.theme.ShakeScreen

enum class Minigame(val displayName: String) {
    SANDBOX("SANDBOX"),
    SHAKE("SHAKE"),
    PENDULUM("PENDULUM")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val isDark = isSystemInDarkTheme()
            val backgroundColor = if (isDark) Color.Black else Color.White
            var currentMinigame by remember { mutableStateOf(Minigame.SANDBOX) }

            Surface(modifier = Modifier.fillMaxSize(), color = backgroundColor) {
                Column(modifier = Modifier.fillMaxSize()) {
                    MinigameSelector(
                        selected = currentMinigame,
                        onSelected = { currentMinigame = it },
                        isDark = isDark
                    )
                    
                    Box(modifier = Modifier.weight(1f)) {
                        Crossfade(targetState = currentMinigame, label = "MinigameTransition") { screen ->
                            when (screen) {
                                Minigame.SANDBOX -> Sandbox2DScreen(isDark = isDark)
                                Minigame.SHAKE -> ShakeScreen(isDark = isDark)
                                Minigame.PENDULUM -> PendulumScreen(isDark = isDark)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MinigameSelector(
    selected: Minigame,
    onSelected: (Minigame) -> Unit,
    isDark: Boolean
) {
    val textColor = if (isDark) Color.White else Color.Black
    val inactiveColor = Color.Gray

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(Minigame.values()) { minigame ->
            Text(
                text = minigame.displayName,
                fontSize = if (selected == minigame) 18.sp else 14.sp,
                fontWeight = if (selected == minigame) FontWeight.Bold else FontWeight.Normal,
                color = if (selected == minigame) textColor else inactiveColor,
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .clickable { onSelected(minigame) }
            )
        }
    }
}