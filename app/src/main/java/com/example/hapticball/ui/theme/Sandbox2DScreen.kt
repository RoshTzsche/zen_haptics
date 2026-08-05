package com.example.hapticball.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hapticball.hardware.HapticEngine
import com.example.hapticball.physics.BallState
import com.example.hapticball.physics.KinematicsEngine
import com.example.hapticball.physics.LineObstacle
import com.example.hapticball.physics.PhysicsUnits
import kotlinx.coroutines.delay
import kotlin.math.sqrt

enum class InteractionMode {
    GRAB, DRAW
}

@Composable
fun Sandbox2DScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var mass by remember { mutableFloatStateOf(500f) }
    var viscosity by remember { mutableFloatStateOf(0.5f) }
    var bounce by remember { mutableFloatStateOf(0.7f) }
    var gravity by remember { mutableFloatStateOf(PhysicsUnits.EARTH_GRAVITY) }
    var unitsPerMeter by remember { mutableFloatStateOf(1000f) }
    var showMenu by remember { mutableStateOf(false) }
    
    var interactionMode by remember { mutableStateOf(InteractionMode.GRAB) }

    var balls by remember { mutableStateOf(listOf(BallState(id = 0, posX = 500f, posY = 500f))) }
    var nextId by remember { mutableIntStateOf(1) }
    var obstacles by remember { mutableStateOf(listOf<LineObstacle>()) }
    
    var currentObstacleStartWorld by remember { mutableStateOf<Offset?>(null) }
    var currentObstacleEndWorld by remember { mutableStateOf<Offset?>(null) }

    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }
    
    val velocityTracker = remember { VelocityTracker() }

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
                detectTapGestures(onLongPress = { showMenu = true })
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        
        val worldWidth = 1000f
        val scale = widthPx / worldWidth
        val worldHeight = heightPx / scale

        fun Offset.toWorld() = Offset(x / scale, y / scale)
        fun Offset.toPx() = Offset(x * scale, y * scale)

        val interactionModifier = Modifier.pointerInput(interactionMode) {
            detectDragGestures(
                onDragStart = { offset ->
                    val worldOffset = offset.toWorld()
                    if (interactionMode == InteractionMode.GRAB) {
                        val clickedBall = balls.find { ball ->
                            val dx = worldOffset.x - ball.posX
                            val dy = worldOffset.y - ball.posY
                            sqrt(dx * dx + dy * dy) < ball.radius * 2.5f
                        }
                        if (clickedBall != null) {
                            balls = balls.map { if (it.id == clickedBall.id) it.copy(isDragging = true, velX = 0f, velY = 0f) else it }
                        }
                    } else {
                        currentObstacleStartWorld = worldOffset
                        currentObstacleEndWorld = worldOffset
                    }
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    velocityTracker.addPosition(change.uptimeMillis, change.position)
                    
                    if (interactionMode == InteractionMode.GRAB) {
                        balls = balls.map { ball ->
                            if (ball.isDragging) {
                                ball.copy(
                                    posX = ball.posX + dragAmount.x / scale,
                                    posY = ball.posY + dragAmount.y / scale
                                )
                            } else ball
                        }
                    } else {
                        currentObstacleEndWorld = change.position.toWorld()
                    }
                },
                onDragEnd = {
                    if (interactionMode == InteractionMode.GRAB) {
                        val draggedBall = balls.find { it.isDragging }
                        if (draggedBall != null) {
                            val velocity = velocityTracker.calculateVelocity()
                            balls = balls.map { 
                                if (it.id == draggedBall.id) {
                                    it.copy(isDragging = false, velX = velocity.x / scale, velY = velocity.y / scale)
                                } else it 
                            }
                            velocityTracker.resetTracking()
                        }
                    } else if (currentObstacleStartWorld != null && currentObstacleEndWorld != null) {
                        obstacles = obstacles + LineObstacle(
                            currentObstacleStartWorld!!.x, currentObstacleStartWorld!!.y,
                            currentObstacleEndWorld!!.x, currentObstacleEndWorld!!.y
                        )
                        currentObstacleStartWorld = null
                        currentObstacleEndWorld = null
                    }
                },
                onDragCancel = {
                    balls = balls.map { it.copy(isDragging = false) }
                    currentObstacleStartWorld = null
                    currentObstacleEndWorld = null
                    velocityTracker.resetTracking()
                }
            )
        }

        LaunchedEffect(worldWidth, worldHeight, gravity, unitsPerMeter, mass, viscosity, bounce) {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                if (dt > 0f && dt < 0.1f) {
                    val gravityWorld = gravity * unitsPerMeter
                    var nextBalls = balls.map { ball ->
                        var b = KinematicsEngine.updatePosition(
                            ball, accelX, accelY, mass, viscosity, gravityWorld, dt
                        )
                        b = KinematicsEngine.resolveWallCollisions(b, worldWidth, worldHeight, bounce) { impact, nx, ny, type ->
                            HapticEngine.triggerCollision(context, impact * scale, nx, ny, type)
                        }
                        b = KinematicsEngine.resolveLineCollisions(b, obstacles, bounce) { impact, _, nx, ny, type ->
                            HapticEngine.triggerCollision(context, impact * scale, nx, ny, type)
                        }
                        b
                    }
                    nextBalls = KinematicsEngine.resolveBallCollisions(nextBalls) { impact, nx, ny, type ->
                        HapticEngine.triggerCollision(context, impact * scale, nx, ny, type)
                    }
                    balls = nextBalls
                }
                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().then(interactionModifier)) {
            val strokeColor = if (isDark) Color.White else Color.Black
            val fillColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            
            obstacles.forEach { line ->
                drawLine(
                    color = strokeColor, 
                    start = Offset(line.x1 * scale, line.y1 * scale), 
                    end = Offset(line.x2 * scale, line.y2 * scale), 
                    strokeWidth = 16f
                )
            }
            
            if (currentObstacleStartWorld != null && currentObstacleEndWorld != null) {
                drawLine(
                    color = strokeColor.copy(alpha = 0.5f), 
                    start = currentObstacleStartWorld!!.toPx(), 
                    end = currentObstacleEndWorld!!.toPx(), 
                    strokeWidth = 4f
                )
            }

            balls.forEach { ball ->
                val center = Offset(ball.posX * scale, ball.posY * scale)
                val radiusPx = ball.radius * scale
                drawCircle(color = fillColor, radius = radiusPx, center = center)
                drawCircle(color = strokeColor, radius = radiusPx, center = center, style = Stroke(width = 4f))
                
                if (ball.isDragging) {
                    drawCircle(color = strokeColor, radius = radiusPx + 10f, center = center, style = Stroke(width = 2f))
                }
            }
        }

        // Ball Counter
        Text(
            text = "BALLS: ${balls.size}/8",
            color = if (isDark) Color.White else Color.Black,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
        )

        // Interaction Mode Toggle
        IconButton(
            onClick = {
                interactionMode = if (interactionMode == InteractionMode.GRAB) InteractionMode.DRAW else InteractionMode.GRAB
            },
            modifier = Modifier.align(Alignment.BottomEnd).padding(bottom = 80.dp, end = 16.dp)
        ) {
            Icon(
                imageVector = if (interactionMode == InteractionMode.GRAB) Icons.Default.BackHand else Icons.Default.Edit,
                contentDescription = "Switch Interaction Mode",
                tint = if (isDark) Color.White else Color.Black,
                modifier = Modifier.size(32.dp)
            )
        }

        if (showMenu) {
            PhysicsMenu(
                mass = mass,
                viscosity = viscosity,
                bounce = bounce,
                gravity = gravity,
                scale = unitsPerMeter,
                onMassChange = { mass = it },
                onViscosityChange = { viscosity = it },
                onBounceChange = { bounce = it },
                onGravityChange = { gravity = it },
                onScaleChange = { unitsPerMeter = it },
                onAddBall = if (balls.size < 8) {
                    {
                        balls = balls + BallState(id = nextId, posX = worldWidth/2, posY = worldHeight/2)
                        nextId++
                    }
                } else null,
                extraContent = {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        androidx.compose.material3.TextButton(onClick = { if (balls.isNotEmpty()) balls = balls.dropLast(1) }) {
                            Text("REMOVE BALL", color = Color.Red, fontSize = 12.sp)
                        }
                        androidx.compose.material3.TextButton(onClick = { obstacles = emptyList() }) {
                            Text("RESET OBSTACLES", color = Color.Red, fontSize = 12.sp)
                        }
                    }
                },
                onDismiss = { showMenu = false }
            )
        }
    }
}
