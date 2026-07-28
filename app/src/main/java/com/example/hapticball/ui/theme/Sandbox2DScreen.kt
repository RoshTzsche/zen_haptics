package com.example.hapticball.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import com.example.hapticball.hardware.HapticEngine
import com.example.hapticball.physics.BallState
import com.example.hapticball.physics.KinematicsEngine
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun Sandbox2DScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current

    // Physics parameters
    var mass by remember { mutableFloatStateOf(1.0f) }
    var viscosity by remember { mutableFloatStateOf(0.5f) }
    var bounce by remember { mutableFloatStateOf(0.7f) }
    var gravityScale by remember { mutableFloatStateOf(1.0f) }
    var showMenu by remember { mutableStateOf(false) }

    // Multi-ball state
    var balls by remember { mutableStateOf(listOf(BallState(id = 0))) }
    var nextId by remember { mutableIntStateOf(1) }
    
    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }

    // SensorManager for tilt
    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    accelX = -it.values[0] * 30f
                    accelY = it.values[1] * 30f
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find a ball to drag
                        balls = balls.map { ball ->
                            val dx = offset.x - ball.posX
                            val dy = offset.y - ball.posY
                            if (sqrt(dx * dx + dy * dy) < ball.radius * 2) {
                                ball.copy(isDragging = true, velX = 0f, velY = 0f)
                            } else ball
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        balls = balls.map { ball ->
                            if (ball.isDragging) {
                                ball.copy(
                                    posX = ball.posX + dragAmount.x,
                                    posY = ball.posY + dragAmount.y,
                                    // Calculate immediate velocity from drag
                                    velX = dragAmount.x * 50f, 
                                    velY = dragAmount.y * 50f
                                )
                            } else ball
                        }
                    },
                    onDragEnd = {
                        balls = balls.map { it.copy(isDragging = false) }
                    }
                )
            }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        // Physics Loop
        LaunchedEffect(widthPx, heightPx) {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                if (dt > 0f && dt < 0.1f) {
                    var nextBalls = balls.map { ball ->
                        KinematicsEngine.updatePosition(
                            ball, accelX, accelY, mass, viscosity, gravityScale, dt
                        )
                    }
                    
                    nextBalls = nextBalls.map { ball ->
                        KinematicsEngine.resolveWallCollisions(
                            ball, widthPx, heightPx, bounce
                        ) { impactVelocity ->
                            HapticEngine.triggerCollision(context, impactVelocity)
                        }
                    }
                    
                    nextBalls = KinematicsEngine.resolveBallCollisions(nextBalls) { impact ->
                        HapticEngine.triggerCollision(context, impact)
                    }
                    
                    balls = nextBalls
                }
                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeColor = if (isDark) Color.White else Color.Black
            val fillColor = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.1f)
            
            balls.forEach { ball ->
                // Draw fill
                drawCircle(
                    color = fillColor,
                    radius = ball.radius,
                    center = Offset(ball.posX, ball.posY)
                )
                // Draw outline
                drawCircle(
                    color = strokeColor,
                    radius = ball.radius,
                    center = Offset(ball.posX, ball.posY),
                    style = Stroke(width = 4f)
                )
                
                // If dragging, draw a subtle indicator
                if (ball.isDragging) {
                    drawCircle(
                        color = strokeColor,
                        radius = ball.radius + 10f,
                        center = Offset(ball.posX, ball.posY),
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        if (showMenu) {
            PhysicsMenu(
                mass = mass,
                viscosity = viscosity,
                bounce = bounce,
                gravity = gravityScale,
                onMassChange = { mass = it },
                onViscosityChange = { viscosity = it },
                onBounceChange = { bounce = it },
                onGravityChange = { gravityScale = it },
                onAddBall = {
                    balls = balls + BallState(id = nextId, posX = widthPx/2, posY = heightPx/2)
                    nextId++
                },
                onDismiss = { showMenu = false }
            )
        }
    }
}
