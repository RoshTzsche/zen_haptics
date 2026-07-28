package com.example.hapticball.physics

import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

data class BallState(
    val id: Int,
    var posX: Float = 500f,
    var posY: Float = 500f,
    var velX: Float = 0f,
    var velY: Float = 0f,
    val radius: Float = 40f,
    val isDragging: Boolean = false
)

data class PendulumState(
    var angle: Float = 0.5f, // In radians
    var angularVelocity: Float = 0f,
    var length: Float = 400f,
    val isDragging: Boolean = false
)

data class RopeNode(
    var x: Float,
    var y: Float,
    var oldX: Float,
    var oldY: Float,
    val isPinned: Boolean = false,
    val isDragging: Boolean = false
)

object KinematicsEngine {
    /**
     * Updates a flexible rope using Verlet integration.
     * @param gravityPx Gravity in pixels/s^2 (e.g. 9.81 * scale)
     */
    fun updateVerletRope(
        nodes: List<RopeNode>,
        accelX: Float,
        accelY: Float,
        gravityPx: Float,
        viscosity: Float,
        segmentLength: Float,
        dt: Float,
        iterations: Int = 10
    ): List<RopeNode> {
        val nextNodes = nodes.map { it.copy() }

        // 1. Verlet Integration (Movement)
        val friction = 1f - (viscosity * 0.1f).coerceIn(0f, 0.1f)

        for (node in nextNodes) {
            if (!node.isPinned && !node.isDragging) {
                val vx = (node.x - node.oldX) * friction
                val vy = (node.y - node.oldY) * friction

                node.oldX = node.x
                node.oldY = node.y

                // Accelerometer forces + Gravity (accel is already in m/s^2 usually, but we scale it)
                // Assuming accel values from sensor are m/s^2, we multiply by scale factor 
                // However, we'll keep the relative 50f scaling for "feel" but tied to the main gravity
                val scaleFactor = gravityPx / 9.81f
                
                node.x += vx + (accelX * scaleFactor) * dt * dt
                node.y += vy + (gravityPx + accelY * scaleFactor) * dt * dt
            } else if (node.isDragging || node.isPinned) {
                node.oldX = node.x
                node.oldY = node.y
            }
        }

        // 2. Constraints (Elasticity/Distance)
        repeat(iterations) {
            for (i in 0 until nextNodes.size - 1) {
                val n1 = nextNodes[i]
                val n2 = nextNodes[i + 1]

                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist == 0f) continue
                
                val diff = (dist - segmentLength) / dist
                val offsetX = dx * 0.5f * diff
                val offsetY = dy * 0.5f * diff

                if (!n1.isPinned && !n1.isDragging) {
                    n1.x += offsetX
                    n1.y += offsetY
                }
                if (!n2.isPinned && !n2.isDragging) {
                    n2.x -= offsetX
                    n2.y -= offsetY
                }
            }
        }

        return nextNodes
    }

    /**
     * Updates a single ball's position.
     * @param gravityPx Gravity in pixels/s^2
     */
    fun updatePosition(
        ball: BallState,
        accelX: Float,
        accelY: Float,
        massInGrams: Float,
        viscosity: Float,
        gravityPx: Float,
        dt: Float
    ): BallState {
        if (ball.isDragging) return ball

        val newBall = ball.copy()
        
        // Use mass to adjust friction (heavier objects resist viscous drag more)
        // Normalized around a 500g ball
        val massFactor = 500f / massInGrams
        val frictionX = (viscosity * massFactor) * ball.velX
        val frictionY = (viscosity * massFactor) * ball.velY

        val scaleFactor = gravityPx / 9.81f
        val totalAx = (accelX * scaleFactor) - frictionX
        val totalAy = (gravityPx + accelY * scaleFactor) - frictionY

        newBall.velX += totalAx * dt
        newBall.velY += totalAy * dt

        newBall.posX += newBall.velX * dt
        newBall.posY += newBall.velY * dt
        
        return newBall
    }

    /**
     * Updates a simple pendulum. Not used in the flexible version but kept for API completeness.
     */
    fun updatePendulum(
        state: PendulumState,
        gravityPx: Float,
        viscosity: Float,
        dt: Float
    ): PendulumState {
        if (state.isDragging) return state

        val L = state.length
        
        // Acceleration = -(g/L) * sin(theta) - damping
        val angularAcceleration = -(gravityPx / L) * sin(state.angle) - (viscosity * state.angularVelocity)
        
        val newAngularVelocity = state.angularVelocity + angularAcceleration * dt
        val newAngle = state.angle + newAngularVelocity * dt
        
        return state.copy(
            angle = newAngle,
            angularVelocity = newAngularVelocity
        )
    }

    fun detectShake(
        currentAccel: FloatArray,
        lastAccel: FloatArray?,
        threshold: Float = 15f
    ): Boolean {
        if (lastAccel == null) return false
        
        val dx = abs(currentAccel[0] - lastAccel[0])
        val dy = abs(currentAccel[1] - lastAccel[1])
        val dz = abs(currentAccel[2] - lastAccel[2])
        
        return (dx + dy + dz) > threshold
    }

    fun resolveWallCollisions(
        ball: BallState,
        width: Float,
        height: Float,
        bounce: Float,
        onCollision: (impactVelocity: Float) -> Unit
    ): BallState {
        val newBall = ball.copy()
        var collided = false
        var impactVel = 0f

        if (newBall.posX - newBall.radius < 0f) {
            newBall.posX = newBall.radius
            impactVel = abs(newBall.velX)
            newBall.velX = -newBall.velX * bounce
            collided = true
        } else if (newBall.posX + newBall.radius > width) {
            newBall.posX = width - newBall.radius
            impactVel = abs(newBall.velX)
            newBall.velX = -newBall.velX * bounce
            collided = true
        }

        if (newBall.posY - newBall.radius < 0f) {
            newBall.posY = newBall.radius
            impactVel = maxOf(impactVel, abs(newBall.velY))
            newBall.velY = -newBall.velY * bounce
            collided = true
        } else if (newBall.posY + newBall.radius > height) {
            newBall.posY = height - newBall.radius
            impactVel = maxOf(impactVel, abs(newBall.velY))
            newBall.velY = -newBall.velY * bounce
            collided = true
        }

        if (collided) {
            onCollision(impactVel)
        }
        
        return newBall
    }

    fun resolveBallCollisions(
        balls: List<BallState>,
        onCollision: (impactVelocity: Float) -> Unit
    ): List<BallState> {
        val newBalls = balls.map { it.copy() }.toMutableList()
        
        for (i in 0 until newBalls.size) {
            for (j in i + 1 until newBalls.size) {
                val b1 = newBalls[i]
                val b2 = newBalls[j]
                
                val dx = b2.posX - b1.posX
                val dy = b2.posY - b1.posY
                val distance = sqrt(dx * dx + dy * dy)
                val minDistance = b1.radius + b2.radius
                
                if (distance < minDistance && distance > 0) {
                    val nx = dx / distance
                    val ny = dy / distance
                    
                    val rvx = b2.velX - b1.velX
                    val rvy = b2.velY - b1.velY
                    
                    val velAlongNormal = rvx * nx + rvy * ny
                    
                    if (velAlongNormal > 0) continue
                    
                    val e = 0.8f
                    val jImpulse = -(1 + e) * velAlongNormal
                    val impulseX = jImpulse * nx / 2f
                    val impulseY = jImpulse * ny / 2f
                    
                    if (!b1.isDragging) {
                        newBalls[i].velX -= impulseX
                        newBalls[i].velY -= impulseY
                    }
                    if (!b2.isDragging) {
                        newBalls[j].velX += impulseX
                        newBalls[j].velY += impulseY
                    }
                    
                    val overlap = minDistance - distance
                    val separationX = nx * overlap / 2f
                    val separationY = ny * overlap / 2f
                    
                    if (!b1.isDragging) {
                        newBalls[i].posX -= separationX
                        newBalls[i].posY -= separationY
                    }
                    if (!b2.isDragging) {
                        newBalls[j].posX += separationX
                        newBalls[j].posY += separationY
                    }
                    
                    onCollision(abs(velAlongNormal) * 100f)
                }
            }
        }
        return newBalls
    }
}
