package com.example.hapticball.physics

import com.example.hapticball.hardware.CollisionType
import kotlin.math.abs
import kotlin.math.sin
import kotlin.math.sqrt

data class BallState(
    val id: Int,
    var posX: Float = 500f, // World units (0-1000)
    var posY: Float = 500f,
    var velX: Float = 0f,
    var velY: Float = 0f,
    val radius: Float = 40f,
    val isDragging: Boolean = false
)

data class LineObstacle(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val isLethal: Boolean = false
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
    fun updateVerletRope(
        nodes: List<RopeNode>,
        accelX: Float,
        accelY: Float,
        gravityWorld: Float,
        viscosity: Float,
        segmentLengthWorld: Float,
        dt: Float,
        iterations: Int = 10
    ): List<RopeNode> {
        val nextNodes = nodes.map { it.copy() }
        val friction = 1f - (viscosity * 0.1f).coerceIn(0f, 0.1f)

        for (node in nextNodes) {
            if (!node.isPinned && !node.isDragging) {
                val vx = (node.x - node.oldX) * friction
                val vy = (node.y - node.oldY) * friction

                node.oldX = node.x
                node.oldY = node.y

                val scaleFactor = gravityWorld / 9.81f
                node.x += vx + (accelX * scaleFactor) * dt * dt
                node.y += vy + (gravityWorld + accelY * scaleFactor) * dt * dt
            } else if (node.isDragging || node.isPinned) {
                node.oldX = node.x
                node.oldY = node.y
            }
        }

        repeat(iterations) {
            for (i in 0 until nextNodes.size - 1) {
                val n1 = nextNodes[i]
                val n2 = nextNodes[i + 1]

                val dx = n2.x - n1.x
                val dy = n2.y - n1.y
                val dist = sqrt(dx * dx + dy * dy)
                if (dist == 0f) continue
                
                val diff = (dist - segmentLengthWorld) / dist
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

    fun updatePosition(
        ball: BallState,
        accelX: Float,
        accelY: Float,
        massInGrams: Float,
        viscosity: Float,
        gravityWorld: Float,
        dt: Float
    ): BallState {
        if (ball.isDragging) return ball

        val newBall = ball.copy()
        val massFactor = 500f / massInGrams
        val frictionX = (viscosity * massFactor) * ball.velX
        val frictionY = (viscosity * massFactor) * ball.velY

        val scaleFactor = gravityWorld / 9.81f
        // Using accel directly as the primary force
        val totalAx = (accelX * scaleFactor) - frictionX
        val totalAy = (accelY * scaleFactor) - frictionY

        newBall.velX += totalAx * dt
        newBall.velY += totalAy * dt

        newBall.posX += newBall.velX * dt
        newBall.posY += newBall.velY * dt
        
        return newBall
    }

    fun updatePendulum(
        state: PendulumState,
        gravityWorld: Float,
        viscosity: Float,
        dt: Float
    ): PendulumState {
        if (state.isDragging) return state
        val L = state.length
        val angularAcceleration = -(gravityWorld / L) * sin(state.angle) - (viscosity * state.angularVelocity)
        val newAngularVelocity = state.angularVelocity + angularAcceleration * dt
        val newAngle = state.angle + newAngularVelocity * dt
        return state.copy(angle = newAngle, angularVelocity = newAngularVelocity)
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
        onCollision: (impactVelocity: Float, normalizedX: Float, normalizedY: Float, type: CollisionType) -> Unit
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
            onCollision(impactVel, newBall.posX / width, newBall.posY / height, CollisionType.WALL)
        }
        return newBall
    }

    fun resolveLineCollisions(
        ball: BallState,
        lines: List<LineObstacle>,
        bounce: Float,
        lineThickness: Float = 20f,
        onCollision: (impactVelocity: Float, isLethal: Boolean, normalizedX: Float, normalizedY: Float, type: CollisionType) -> Unit
    ): BallState {
        var newBall = ball.copy()
        for (line in lines) {
            val dx = line.x2 - line.x1
            val dy = line.y2 - line.y1
            val lenSq = dx * dx + dy * dy
            if (lenSq == 0f) continue
            
            val t = ((newBall.posX - line.x1) * dx + (newBall.posY - line.y1) * dy) / lenSq
            val tClamped = t.coerceIn(0f, 1f)
            
            val closestX = line.x1 + tClamped * dx
            val closestY = line.y1 + tClamped * dy
            
            val distDx = newBall.posX - closestX
            val distDy = newBall.posY - closestY
            val distSq = distDx * distDx + distDy * distDy
            
            val combinedRadius = newBall.radius + lineThickness / 2f
            if (distSq < combinedRadius * combinedRadius) {
                val dist = sqrt(distSq)
                val nx = if (dist == 0f) 0f else distDx / dist
                val ny = if (dist == 0f) 1f else distDy / dist
                
                newBall.posX = closestX + nx * combinedRadius
                newBall.posY = closestY + ny * combinedRadius
                
                val dot = newBall.velX * nx + newBall.velY * ny
                if (dot < 0) {
                    newBall.velX -= 2 * dot * nx * bounce
                    newBall.velY -= 2 * dot * ny * bounce
                    
                    val type = if (line.isLethal) CollisionType.LETHAL else CollisionType.OBSTACLE
                    onCollision(abs(dot), line.isLethal, newBall.posX / 1000f, newBall.posY / 1000f, type)
                }
            }
        }
        return newBall
    }

    fun resolveBallCollisions(
        balls: List<BallState>,
        onCollision: (impactVelocity: Float, normalizedX: Float, normalizedY: Float, type: CollisionType) -> Unit
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
                    onCollision(abs(velAlongNormal) * 100f, newBalls[i].posX / 1000f, newBalls[i].posY / 1000f, CollisionType.BALL)
                }
            }
        }
        return newBalls
    }
}
