package com.example.hapticball.ui.theme

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.compose.foundation.Canvas
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
import kotlin.random.Random

@Composable
fun ShakeScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var balls by remember { mutableStateOf(List(5) { id -> BallState(id = id, radius = 30f + Random.nextFloat() * 20f) }) }
    var lastAccel: FloatArray? by remember { mutableStateOf(null) }
    var shakeThreshold by remember { mutableFloatStateOf(20f) }
    var showMenu by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent?) {
                event?.let {
                    val currentAccel = it.values.clone()
                    if (KinematicsEngine.detectShake(currentAccel, lastAccel, threshold = shakeThreshold)) {
                        // React to shake
                        balls = balls.map { ball ->
                            ball.copy(
                                velX = (Random.nextFloat() - 0.5f) * 5000f,
                                velY = (Random.nextFloat() - 0.5f) * 5000f
                            )
                        }
                        HapticEngine.triggerCollision(context, 1000f)
                    }
                    lastAccel = currentAccel
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
        }
        sensorManager.registerListener(listener, accelerometer, SensorManager.SENSOR_DELAY_GAME)
        onDispose { sensorManager.unregisterListener(listener) }
    }

    BoxWithConstraints(modifier = Modifier
        .fillMaxSize()
        .pointerInput(Unit) {
            detectTapGestures(onLongPress = { showMenu = true })
        }
    ) {
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }

        LaunchedEffect(widthPx, heightPx) {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                if (dt > 0f && dt < 0.1f) {
                    var nextBalls = balls.map { ball ->
                        KinematicsEngine.updatePosition(ball, 0f, 0f, 1f, 0.2f, 1f, dt)
                    }
                    nextBalls = nextBalls.map { ball ->
                        KinematicsEngine.resolveWallCollisions(ball, widthPx, heightPx, 0.9f) { impact ->
                            HapticEngine.triggerCollision(context, impact)
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
            balls.forEach { ball ->
                drawCircle(
                    color = strokeColor,
                    radius = ball.radius,
                    center = Offset(ball.posX, ball.posY),
                    style = Stroke(width = 4f)
                )
            }
        }

        if (showMenu) {
            PhysicsMenu(
                title = "SHAKE SETTINGS",
                extraContent = {
                    MinimalSlider(
                        label = "SHAKE SENSITIVITY",
                        value = shakeThreshold,
                        range = 5f..50f,
                        onValueChange = { shakeThreshold = it }
                    )
                },
                onDismiss = { showMenu = false }
            )
        }
    }
}
