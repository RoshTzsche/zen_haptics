package com.example.hapticball.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.edit
import com.example.hapticball.hardware.HapticEngine
import com.example.hapticball.physics.BallState
import com.example.hapticball.physics.KinematicsEngine
import com.example.hapticball.physics.PuzzleLevels
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun PuzzleScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val sharedPref = remember { context.getSharedPreferences("puzzle_prefs", Context.MODE_PRIVATE) }

    var currentLevelIdx by remember { 
        mutableIntStateOf(sharedPref.getInt("current_level_idx", 0)) 
    }
    var maxUnlockedLevel by remember { 
        mutableIntStateOf(sharedPref.getInt("max_unlocked_level", 0)) 
    }
    
    var showLevelSelector by remember { mutableStateOf(false) }

    val level = PuzzleLevels.levels[currentLevelIdx]

    var ball by remember(currentLevelIdx) {
        mutableStateOf(BallState(id = 0, posX = level.startPos.x, posY = level.startPos.y))
    }

    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(currentLevelIdx) {
        sharedPref.edit { putInt("current_level_idx", currentLevelIdx) }
    }

    LaunchedEffect(maxUnlockedLevel) {
        sharedPref.edit { putInt("max_unlocked_level", maxUnlockedLevel) }
    }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    accelX = -it.values[0]
                    accelY = it.values[1]
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = { showLevelSelector = true })
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        val worldWidth = 1000f
        val scale = widthPx / worldWidth
        val worldHeight = heightPx / scale

        LaunchedEffect(worldWidth, worldHeight, currentLevelIdx) {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                if (dt > 0f && dt < 0.1f) {
                    val gravityWorld = 9.81f * 1000f // 1000 units per meter fixed for puzzle
                    var nextBall = KinematicsEngine.updatePosition(
                        ball, accelX, accelY, 500f, 0.5f, gravityWorld, dt
                    )
                    
                    nextBall = KinematicsEngine.resolveWallCollisions(nextBall, worldWidth, worldHeight, 0.5f) { impact, nx, ny, type ->
                        HapticEngine.triggerCollision(context, impact * scale, nx, ny, type)
                    }
                    
                    var hitLethal = false
                    nextBall = KinematicsEngine.resolveLineCollisions(nextBall, level.obstacles, 0.5f) { impact, lethal, nx, ny, type ->
                        HapticEngine.triggerCollision(context, impact * scale, nx, ny, type)
                        if (lethal) hitLethal = true
                    }

                    if (hitLethal) {
                        ball = BallState(id = 0, posX = level.startPos.x, posY = level.startPos.y)
                        delay(100L)
                        continue
                    }

                    // Win Condition
                    val distToGoal = sqrt(
                        (nextBall.posX - level.goalPos.x) * (nextBall.posX - level.goalPos.x) +
                        (nextBall.posY - level.goalPos.y) * (nextBall.posY - level.goalPos.y)
                    )
                    if (distToGoal < level.goalRadius) {
                        HapticEngine.triggerCollision(context, 2000f) // Victory pulse
                        if (currentLevelIdx < PuzzleLevels.levels.size - 1) {
                            if (currentLevelIdx == maxUnlockedLevel) {
                                maxUnlockedLevel++
                            }
                            currentLevelIdx++
                        } else {
                            currentLevelIdx = 0
                        }
                        break
                    }
                    
                    ball = nextBall
                }
                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = if (isDark) Color.White else Color.Black
            
            // Draw Goal
            drawCircle(
                color = color.copy(alpha = 0.2f),
                radius = level.goalRadius * scale,
                center = level.goalPos * scale
            )
            drawCircle(
                color = color,
                radius = level.goalRadius * scale,
                center = level.goalPos * scale,
                style = Stroke(width = 4f * scale)
            )

            // Draw Obstacles
            level.obstacles.forEach { line ->
                drawLine(
                    color = if (line.isLethal) Color.Red else color,
                    start = Offset(line.x1 * scale, line.y1 * scale),
                    end = Offset(line.x2 * scale, line.y2 * scale),
                    strokeWidth = 16f * scale
                )
            }

            // Draw Ball
            val centerPx = Offset(ball.posX * scale, ball.posY * scale)
            val radiusPx = ball.radius * scale
            drawCircle(
                color = color.copy(alpha = 0.1f),
                radius = radiusPx,
                center = centerPx
            )
            drawCircle(
                color = color,
                radius = radiusPx,
                center = centerPx,
                style = Stroke(width = 4f * scale)
            )
        }

        // Level UI
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(32.dp)) {
            Text(
                text = "LEVEL ${level.id}: ${level.name}",
                color = if (isDark) Color.White else Color.Black,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Restart Button
        IconButton(
            onClick = {
                ball = BallState(id = 0, posX = level.startPos.x, posY = level.startPos.y)
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Default.Refresh,
                contentDescription = "Restart Level",
                tint = if (isDark) Color.White else Color.Black
            )
        }

        if (showLevelSelector) {
            LevelSelectorDialog(
                currentLevelIdx = currentLevelIdx,
                maxUnlockedLevel = maxUnlockedLevel,
                isDark = isDark,
                onLevelSelected = { 
                    currentLevelIdx = it
                    showLevelSelector = false
                },
                onDismiss = { showLevelSelector = false }
            )
        }
    }
}

@Composable
fun LevelSelectorDialog(
    currentLevelIdx: Int,
    maxUnlockedLevel: Int,
    isDark: Boolean,
    onLevelSelected: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedIdx by remember { mutableIntStateOf(currentLevelIdx) }
    
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isDark) Color.DarkGray else Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, if (isDark) Color.White else Color.Black, RoundedCornerShape(16.dp))
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "SELECT LEVEL",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else Color.Black
            )
            
            Spacer(modifier = Modifier.height(32.dp))
            
            val level = PuzzleLevels.levels[selectedIdx]
            val isLocked = selectedIdx > maxUnlockedLevel
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "LEVEL ${level.id}",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isLocked) Color.Gray else (if (isDark) Color.White else Color.Black)
                )
                Text(
                    text = if (isLocked) "LOCKED" else level.name,
                    fontSize = 14.sp,
                    color = if (isLocked) Color.Red else (if (isDark) Color.LightGray else Color.DarkGray)
                )
                
                if (isLocked) {
                    Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.Red,
                        modifier = Modifier.size(48.dp).padding(8.dp)
                    )
                } else {
                    Spacer(modifier = Modifier.height(48.dp))
                }
            }

            Slider(
                value = selectedIdx.toFloat(),
                onValueChange = { 
                    val newIdx = it.roundToInt()
                    if (newIdx != selectedIdx) {
                        HapticEngine.triggerTick(context)
                        selectedIdx = newIdx
                    }
                },
                valueRange = 0f..(PuzzleLevels.levels.size - 1).toFloat(),
                steps = PuzzleLevels.levels.size - 2,
                colors = SliderDefaults.colors(
                    thumbColor = if (isDark) Color.White else Color.Black,
                    activeTrackColor = if (isDark) Color.White else Color.Black,
                    inactiveTrackColor = Color.Gray
                ),
                modifier = Modifier.padding(vertical = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TextButton(onClick = onDismiss) {
                    Text("CANCEL", color = Color.Gray)
                }
                Button(
                    onClick = { onLevelSelected(selectedIdx) },
                    enabled = !isLocked,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) Color.White else Color.Black,
                        contentColor = if (isDark) Color.Black else Color.White
                    )
                ) {
                    Text("CONFIRM")
                }
            }
        }
    }
}
