package com.example.hapticball.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.hapticball.physics.PhysicsUnits

@Composable
fun PhysicsMenu(
    title: String = "PHYSICS",
    mass: Float? = null,
    viscosity: Float? = null,
    bounce: Float? = null,
    gravity: Float? = null,
    scale: Float? = null,
    onMassChange: ((Float) -> Unit)? = null,
    onViscosityChange: ((Float) -> Unit)? = null,
    onBounceChange: ((Float) -> Unit)? = null,
    onGravityChange: ((Float) -> Unit)? = null,
    onScaleChange: ((Float) -> Unit)? = null,
    onAddBall: (() -> Unit)? = null,
    onDismiss: () -> Unit,
    extraContent: @Composable (ColumnScope.() -> Unit)? = null
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(2.dp, Color.Black, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            mass?.let { m -> onMassChange?.let { 
                MinimalSlider(label = "MASS", value = m, range = 50f..5000f, onValueChange = it, 
                    valueFormatter = { PhysicsUnits.formatMass(it) }) 
            } }
            
            viscosity?.let { v -> onViscosityChange?.let { 
                MinimalSlider(label = "VISCOSITY", value = v, range = 0f..5f, onValueChange = it) 
            } }
            
            bounce?.let { b -> onBounceChange?.let { 
                MinimalSlider(label = "BOUNCE", value = b, range = 0f..1f, onValueChange = it) 
            } }
            
            gravity?.let { g -> onGravityChange?.let { 
                MinimalSlider(label = "GRAVITY", value = g, range = 0f..20f, onValueChange = it,
                    valueFormatter = { PhysicsUnits.formatGravity(it) }) 
            } }

            scale?.let { s -> onScaleChange?.let {
                MinimalSlider(label = "WORLD SCALE", value = s, range = 100f..2000f, onValueChange = it,
                    valueFormatter = { PhysicsUnits.formatScale(it) })
            } }

            extraContent?.invoke(this)

            Spacer(modifier = Modifier.height(24.dp))

            onAddBall?.let {
                Button(
                    onClick = it,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Black, contentColor = Color.White),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("ADD BALL", fontWeight = FontWeight.Bold)
                }
            }

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)
            ) {
                Text("CLOSE", color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun MinimalSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    valueFormatter: (Float) -> String = { String.format("%.1f", it) }
) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(text = label, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Black)
            Text(text = valueFormatter(value), fontSize = 12.sp, color = Color.Black)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Color.Black,
                activeTrackColor = Color.Black,
                inactiveTrackColor = Color.LightGray
            )
        )
    }
}
