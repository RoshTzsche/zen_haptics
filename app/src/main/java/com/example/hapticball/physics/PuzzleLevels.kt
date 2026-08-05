package com.example.hapticball.physics

import androidx.compose.ui.geometry.Offset

data class PuzzleLevel(
    val id: Int,
    val name: String,
    val startPos: Offset,
    val goalPos: Offset,
    val goalRadius: Float = 60f,
    val obstacles: List<LineObstacle>
)

object PuzzleLevels {
    val levels = listOf(
        PuzzleLevel(
            id = 1,
            name = "THE BEGINNING",
            startPos = Offset(200f, 200f),
            goalPos = Offset(800f, 1500f),
            obstacles = listOf(
                LineObstacle(0f, 1000f, 700f, 1000f)
            )
        ),
        PuzzleLevel(
            id = 2,
            name = "THE ZIGZAG",
            startPos = Offset(100f, 200f),
            goalPos = Offset(900f, 1800f),
            obstacles = listOf(
                LineObstacle(0f, 600f, 800f, 600f),
                LineObstacle(200f, 1200f, 1100f, 1200f)
            )
        ),
        PuzzleLevel(
            id = 3,
            name = "THE CORRIDOR",
            startPos = Offset(500f, 100f),
            goalPos = Offset(500f, 1900f),
            obstacles = listOf(
                LineObstacle(300f, 0f, 300f, 1700f),
                LineObstacle(700f, 200f, 700f, 2000f)
            )
        ),
        PuzzleLevel(
            id = 4,
            name = "LETHAL EDGE",
            startPos = Offset(500f, 200f),
            goalPos = Offset(500f, 1700f),
            obstacles = listOf(
                LineObstacle(0f, 1000f, 1000f, 1000f, isLethal = true),
                LineObstacle(200f, 900f, 200f, 1100f),
                LineObstacle(800f, 900f, 800f, 1100f)
            )
        ),
        PuzzleLevel(
            id = 5,
            name = "DANGER ZONE",
            startPos = Offset(100f, 100f),
            goalPos = Offset(900f, 1800f),
            obstacles = listOf(
                LineObstacle(300f, 0f, 300f, 600f, isLethal = true),
                LineObstacle(700f, 400f, 700f, 1200f, isLethal = true),
                LineObstacle(300f, 1000f, 300f, 2000f, isLethal = true)
            )
        ),
        PuzzleLevel(
            id = 6,
            name = "NARROW PATH",
            startPos = Offset(500f, 100f),
            goalPos = Offset(500f, 1900f),
            obstacles = listOf(
                LineObstacle(400f, 0f, 400f, 2000f, isLethal = true),
                LineObstacle(600f, 0f, 600f, 2000f, isLethal = true),
                LineObstacle(400f, 1000f, 500f, 1000f),
                LineObstacle(500f, 1500f, 600f, 1500f)
            )
        ),
        PuzzleLevel(
            id = 7,
            name = "THE MAZE",
            startPos = Offset(100f, 100f),
            goalPos = Offset(500f, 1000f),
            obstacles = listOf(
                LineObstacle(0f, 300f, 800f, 300f),
                LineObstacle(200f, 600f, 1000f, 600f, isLethal = true),
                LineObstacle(0f, 900f, 400f, 900f),
                LineObstacle(600f, 900f, 1000f, 900f),
                LineObstacle(500f, 600f, 500f, 900f, isLethal = true)
            )
        ),
        PuzzleLevel(
            id = 8,
            name = "CROSSFIRE",
            startPos = Offset(500f, 100f),
            goalPos = Offset(500f, 1800f),
            obstacles = listOf(
                LineObstacle(0f, 500f, 450f, 500f, isLethal = true),
                LineObstacle(550f, 500f, 1000f, 500f, isLethal = true),
                LineObstacle(0f, 1000f, 1000f, 1000f),
                LineObstacle(0f, 1500f, 450f, 1500f, isLethal = true),
                LineObstacle(550f, 1500f, 1000f, 1500f, isLethal = true)
            )
        ),
        PuzzleLevel(
            id = 9,
            name = "SQUEEZE",
            startPos = Offset(500f, 100f),
            goalPos = Offset(500f, 1900f),
            obstacles = listOf(
                LineObstacle(0f, 400f, 480f, 800f, isLethal = true),
                LineObstacle(1000f, 400f, 520f, 800f, isLethal = true),
                LineObstacle(0f, 1600f, 480f, 1200f, isLethal = true),
                LineObstacle(1000f, 1600f, 520f, 1200f, isLethal = true)
            )
        ),
        PuzzleLevel(
            id = 10,
            name = "THE GAUNTLET",
            startPos = Offset(100f, 100f),
            goalPos = Offset(900f, 1900f),
            obstacles = listOf(
                LineObstacle(0f, 500f, 1000f, 500f, isLethal = true),
                LineObstacle(0f, 1000f, 1000f, 1000f, isLethal = true),
                LineObstacle(0f, 1500f, 1000f, 1500f, isLethal = true),
                LineObstacle(450f, 400f, 550f, 600f),
                LineObstacle(450f, 900f, 550f, 1100f),
                LineObstacle(450f, 1400f, 550f, 1600f)
            )
        )
    )
}
