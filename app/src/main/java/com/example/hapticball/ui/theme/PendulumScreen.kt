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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
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
import com.example.hapticball.physics.KinematicsEngine
import com.example.hapticball.physics.PhysicsUnits
import com.example.hapticball.physics.RopeNode
import kotlinx.coroutines.delay
import kotlin.math.sqrt

@Composable
fun PendulumScreen(isDark: Boolean) {
    val context = LocalContext.current
    val density = LocalDensity.current

    val segmentCount = 15
    val segmentLengthWorld = 40f

    var ropeNodes by remember {
        mutableStateOf(List(segmentCount) { i ->
            RopeNode(
                x = 500f,
                y = 200f + i * segmentLengthWorld,
                oldX = 500f,
                oldY = 200f + i * segmentLengthWorld,
                isPinned = i == 0
            )
        })
    }

    var attractorPointsWorld by remember { mutableStateOf(listOf<Offset>()) }
    var showAttractor by remember { mutableStateOf(true) }
    
    var gravity by remember { mutableFloatStateOf(PhysicsUnits.EARTH_GRAVITY) }
    var viscosity by remember { mutableFloatStateOf(0.1f) }
    var unitsPerMeter by remember { mutableFloatStateOf(1000f) }
    var showMenu by remember { mutableStateOf(false) }

    var accelX by remember { mutableFloatStateOf(0f) }
    var accelY by remember { mutableFloatStateOf(0f) }

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

        val interactionModifier = Modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val worldOffset = offset.toWorld()
                    var closestIdx = -1
                    var minDist = Float.MAX_VALUE
                    ropeNodes.forEachIndexed { i, node ->
                        val d = sqrt((worldOffset.x - node.x) * (worldOffset.x - node.x) + (worldOffset.y - node.y) * (worldOffset.y - node.y))
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
                                x = node.x + dragAmount.x / scale,
                                y = node.y + dragAmount.y / scale
                            )
                        } else node
                    }
                },
                onDragEnd = {
                    ropeNodes = ropeNodes.map { it.copy(isDragging = false) }
                }
            )
        }

        LaunchedEffect(worldWidth, worldHeight, gravity, unitsPerMeter, viscosity) {
            var lastTime = System.nanoTime()
            while (true) {
                val currentTime = System.nanoTime()
                val dt = (currentTime - lastTime) / 1_000_000_000f
                lastTime = currentTime

                if (dt > 0f && dt < 0.1f) {
                    val gravityWorld = gravity * unitsPerMeter
                    val nextNodes = KinematicsEngine.updateVerletRope(
                        ropeNodes, accelX, accelY, gravityWorld, viscosity, segmentLengthWorld, dt,
                        iterations = 10
                    )
                    
                    val bob = nextNodes.last()
                    
                    if (showAttractor) {
                        val pivot = nextNodes.first()
                        val dx = bob.x - pivot.x
                        val vx = (bob.x - bob.oldX) / dt
                        
                        val centerWorldX = worldWidth / 2
                        val centerWorldY = worldHeight / 2 + 300f
                        val pointWorld = Offset(centerWorldX + dx, centerWorldY + vx * 0.1f)
                        attractorPointsWorld = (attractorPointsWorld + pointWorld).takeLast(1000)
                    } else {
                        attractorPointsWorld = emptyList()
                    }
                    
                    ropeNodes = nextNodes
                }
                delay(16L)
            }
        }

        Canvas(modifier = Modifier.fillMaxSize().then(interactionModifier)) {
            val color = if (isDark) Color.White else Color.Black
            
            if (showAttractor) {
                attractorPointsWorld.forEachIndexed { index, pointWorld ->
                    val alpha = index.toFloat() / attractorPointsWorld.size
                    drawCircle(color = color.copy(alpha = alpha * 0.4f), radius = 2f * scale, center = pointWorld.toPx())
                }
            }

            val path = Path().apply {
                moveTo(ropeNodes.first().x * scale, ropeNodes.first().y * scale)
                for (i in 1 until ropeNodes.size) {
                    lineTo(ropeNodes[i].x * scale, ropeNodes[i].y * scale)
                }
            }
            drawPath(path = path, color = color, style = Stroke(width = 4f * scale))
            drawCircle(color = color, radius = 12f * scale, center = Offset(ropeNodes.first().x * scale, ropeNodes.first().y * scale))
            val bob = ropeNodes.last()
            drawCircle(color = color, radius = 40f * scale, center = Offset(bob.x * scale, bob.y * scale), style = Stroke(width = 5f * scale))
        }

        // Refresh Button for Attractor
        if (showAttractor) {
            IconButton(
                onClick = { attractorPointsWorld = emptyList() },
                modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Attractor",
                    tint = if (isDark) Color.White else Color.Black
                )
            }
        }

        if (showMenu) {
            PhysicsMenu(
                title = "PENDULUM",
                gravity = gravity,
                viscosity = viscosity,
                scale = unitsPerMeter,
                onGravityChange = { gravity = it },
                onViscosityChange = { viscosity = it },
                onScaleChange = { unitsPerMeter = it },
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
