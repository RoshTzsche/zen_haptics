package com.example.hapticball.ui.theme

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BackHand
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun TutorialOverlay(
    isDark: Boolean,
    onDismiss: (Boolean) -> Unit
) {
    var dontShowAgain by remember { mutableStateOf(false) }
    val overlayColor = if (isDark) Color.Black.copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
    val textColor = if (isDark) Color.White else Color.Black

    // Hand Animation for Long Press
    val infiniteTransition = rememberInfiniteTransition(label = "HandAnimation")
    val handScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HandScale"
    )
    val handAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "HandAlpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(overlayColor)
            .padding(24.dp)
    ) {
        // 1. Long Press Tutorial (Center)
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Pulse effect
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .scale(handScale)
                        .alpha(1f - (handScale - 1f) * 3f)
                        .background(textColor.copy(alpha = 0.2f), CircleShape)
                )
                Icon(
                    imageVector = Icons.Default.TouchApp,
                    contentDescription = "Long Press",
                    tint = textColor,
                    modifier = Modifier
                        .size(80.dp)
                        .scale(handScale)
                        .alpha(handAlpha)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "LONG PRESS BACKGROUND\nFOR PHYSICS SETTINGS",
                color = textColor,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
        }

        // 2. Sandbox Buttons Tutorial (Bottom Right)
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 140.dp, end = 16.dp),
            horizontalAlignment = Alignment.End
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "SWITCH BETWEEN\nGRAB & DRAW",
                    color = textColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.End
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.BackHand, "Grab", tint = textColor, modifier = Modifier.size(24.dp))
                    Text("GRAB", color = textColor, fontSize = 10.sp)
                }
                Text(" / ", color = textColor)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Edit, "Draw", tint = textColor, modifier = Modifier.size(24.dp))
                    Text("DRAW", color = textColor, fontSize = 10.sp)
                }
            }
        }

        // 3. Bottom Controls (Checkbox & Dismiss)
        Column(
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { dontShowAgain = !dontShowAgain }
            ) {
                Checkbox(
                    checked = dontShowAgain,
                    onCheckedChange = { dontShowAgain = it },
                    colors = CheckboxDefaults.colors(
                        checkedColor = textColor,
                        uncheckedColor = textColor.copy(alpha = 0.6f),
                        checkmarkColor = overlayColor
                    )
                )
                Text("Don't show again", color = textColor, fontSize = 14.sp)
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = { onDismiss(dontShowAgain) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = textColor,
                    contentColor = overlayColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth(0.6f)
            ) {
                Text("GET STARTED", fontWeight = FontWeight.Bold)
            }
        }
    }
}
