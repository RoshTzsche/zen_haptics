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
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.hapticball.hardware.HapticEngine
import com.example.hapticball.physics.KinematicsEngine
import com.example.hapticball.physics.RopeNode
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun PendulumScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val segmentCount = 15
    val segmentLength = 40f

    var ropeNodes by remember {
        mutableStateOf(List(segmentCount) { i ->
            RopeNode(
                x = 500f,
                y = 200f + i * segmentLength,
                oldX = 500f,
                oldY = 200f + i * segmentLength,
                isPinned = i == 0
            )
        })
    }

    var attractorPoints by remember { mutableStateOf(listOf<Offset>()) }
    var showAttractor by remember { mutableStateOf(true) }
    var gravity by remember { mutableFloatStateOf(1.0f) }
    var viscosity by remember { mutableFloatStateOf(0.1f) }
    var showMenu by remember { mutableStateOf(false) }

    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }

    // SensorManager for tilt
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
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        // Find closest node to drag
                        var closestIdx = -1
                        var minDist = Float.MAX_VALUE
                        ropeNodes.forEachIndexed { i, node ->
                            val d = sqrt((offset.x - node.x) * (offset.x - node.x) + (offset.y - node.y) * (offset.y - node.y))
                            if (d < minDist && d < 100f) {
                                minDist = d
                                closestIdx = i
                            }
                        }
                        if (closestIdx != -1) {
                            ropeNodes = ropeNodes.mapIndexed { i, node ->
                                if (i == closestIdx) node.copy(isDragging = true) else node
                            }
                        }
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        ropeNodes = ropeNodes.map { node ->
                            if (node.isDragging) {
                                node.copy(
                                    x = node.x + dragAmount.x,
                                    y = node.y + dragAmount.y
                                )
                            } else node
                        }
                    },
                    onDragEnd = {
                        ropeNodes = ropeNodes.map { it.copy(isDragging = false) }
                    }
                )
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
                    val nextNodes = KinematicsEngine.updateVerletRope(
                        ropeNodes, accelX, accelY, gravity, viscosity, segmentLength, dt
                    )
                    
                    // Collision check for the bob (last node)
                    val bob = nextNodes.last()
                    val radius = 40f
                    if (bob.x - radius < 0 || bob.x + radius > widthPx || bob.y + radius > heightPx || bob.y - radius < 0) {
                        HapticEngine.triggerCollision(context, 300f)
                    }

                    if (showAttractor) {
                        // Plot attractor for bob
                        // (Roughly using bob's horizontal displacement from pivot as "theta" 
                        // and bob's velocity as "omega" for the visualization)
                        val pivot = nextNodes.first()
                        val dx = bob.x - pivot.x
                        val vx = (bob.x - bob.oldX) / dt
                        
                        val centerX = widthPx / 2
                        val centerY = heightPx / 2 + 300f
                        val point = Offset(centerX + dx, centerY + vx * 0.1f)
                        attractorPoints = (attractorPoints + point).takeLast(1000)
                    } else {
                        attractorPoints = emptyList()
                    }
                    
                    ropeNodes = nextNodes
                }
                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize()) {
            val color = if (isDark) Color.White else Color.Black
            
            // Draw Attractor
            if (showAttractor) {
                attractorPoints.forEachIndexed { index, point ->
                    val alpha = index.toFloat() / attractorPoints.size
                    drawCircle(color = color.copy(alpha = alpha * 0.4f), radius = 2f, center = point)
                }
            }

            // Draw Rope as Path
            val path = Path().apply {
                moveTo(ropeNodes.first().x, ropeNodes.first().y)
                for (i in 1 until ropeNodes.size) {
                    lineTo(ropeNodes[i].x, ropeNodes[i].y)
                }
            }
            drawPath(path = path, color = color, style = Stroke(width = 4f))

            // Draw Pivot
            drawCircle(color = color, radius = 12f, center = Offset(ropeNodes.first().x, ropeNodes.first().y))
            
            // Draw Bob
            val bob = ropeNodes.last()
            drawCircle(color = color, radius = 40f, center = Offset(bob.x, bob.y), style = Stroke(width = 5f))
        }

        if (showMenu) {
            PhysicsMenu(
                title = "PENDULUM",
                gravity = gravity,
                viscosity = viscosity,
                onGravityChange = { gravity = it },
                onViscosityChange = { viscosity = it },
                extraContent = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = showAttractor,
                            onCheckedChange = { showAttractor = it },
                            colors = CheckboxDefaults.colors(checkedColor = Color.Black, checkmarkColor = Color.White)
                        )
                        Text("SHOW ATTRACTOR", color = Color.Black, fontSize = 14.sp)
                    }
                },
                onDismiss = { showMenu = false }
            )
        }
    }
}
