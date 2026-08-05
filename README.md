# HapticBall

HapticBall is an interactive Android physics simulation built with Jetpack Compose. It combines real-time kinematics with an advanced, spatial haptic feedback system to create a tactile and immersive mobile experience.

![HapticBall Banner](https://via.placeholder.com/800x400.png?text=HapticBall+Simulation) *<!-- Replace with a real screenshot if available -->*

## 🚀 Features

### 🛠️ Real-time Physics Engine
*   **Verlet Integration**: High-performance constraint solver for realistic rope and ball kinematics.
*   **Anti-Tunneling**: Advanced collision detection that prevents high-velocity objects from passing through obstacles.
*   **Responsive World**: All physics math operates in a normalized 1000x1000 unit space, ensuring identical behavior across all screen sizes and aspect ratios.

### 🖐️ Advanced Spatial Haptics
*   **Material-Based Feedback**: Distinct vibration signatures for walls (solid/thuddy), balls (sharp/metallic), and obstacles (tactile).
*   **Spatial Mapping**: Collisions at the bottom of the screen provide deeper, low-frequency feedback, while top-screen impacts are sharper and lighter.
*   **Dynamic Intensity**: Vibration strength scales linearly with impact velocity for a nuanced tactile feel.
*   **Haptic Primitives**: Leverages Android 11+ Haptic Composition API with robust fallbacks for older devices.

### 🎮 Game Modes
*   **Sandbox**: A free-play environment where you can spawn balls, draw custom obstacles, and manipulate physics constants like gravity and viscosity.
*   **Levels**: 10 hand-crafted puzzle levels that increase in difficulty, featuring lethal red obstacles and persistent progression.

### 📱 Modern Android Tech Stack
*   **Jetpack Compose**: 100% declarative UI.
*   **Material 3**: Clean, modern design with adaptive light/dark theme support.
*   **Persistence**: Automatically saves your last played level and minigame state.
*   **Onboarding**: An interactive animated tutorial for new users.

## 🛠️ Installation

1.  Clone the repository:
    ```bash
    git clone https://github.com/yourusername/HapticBall.git
    ```
2.  Open the project in **Android Studio (Ladybug or newer)**.
3.  Sync Gradle and run the app on a physical device to experience the haptic feedback (emulators do not support advanced haptics).

## 🎮 How to Play

*   **Long Press**: Tap and hold the background in any mode to open the **Physics Settings** or **Level Selector**.
*   **Sandbox**: Use the floating action button to switch between **GRAB** (move balls) and **DRAW** (create obstacles).
*   **Levels**: Guide the ball to the goal while avoiding lethal red lines. Use the refresh button in the corner to restart if you get stuck.

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
