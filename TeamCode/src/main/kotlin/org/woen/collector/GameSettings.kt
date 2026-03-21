package org.woen.collector

import com.acmerobotics.dashboard.config.Config
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

@Config
internal object GAME_CONFIGS {
    @JvmField
    var START_RED_CLOSE_ORIENTATION = Orientation(Vec2(-1.205 - 0.01 - 0.38 / 2.0, 0.965 + 0.38 / 2.0 + 0.01), Angle.ofDeg(0.0))

    @JvmField
    var START_RED_FAR_ORIENTATION = Orientation(Vec2(1.631, 0.39), Angle.ofDeg(180.0))

    @JvmField
    var START_BLUE_CLOSE_ORIENTATION = Orientation(Vec2(-1.205 - 0.01 - 0.38 / 2.0, -0.965 - 0.38 / 2.0 - 0.01), Angle.ofDeg(0.0))

    @JvmField
    var START_BLUE_FAR_ORIENTATION = Orientation(Vec2(1.631, -0.39), Angle.ofDeg(180.0))

    @JvmField
    var BLUE_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, -3.66 / 2.0 + 0.2)

    @JvmField
    var RED_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, 3.66 / 2.0 - 0.2)

    @JvmField
    var RED_PARKING_ORIENTATION = Orientation(Vec2(0.0, 0.0), Angle.ofDeg(0.0))

    @JvmField
    var BLUE_PARKING_ORIENTATION = Orientation(Vec2(0.0, 0.0), Angle.ofDeg(0.0))

    @JvmField
    var OBELISK_POSITION = Vec2(-3.66 / 2.0, 0.0)
}

enum class GameColor {
    RED, BLUE
}

enum class GamePosition {
    FAR, CLOSE
}

enum class StartOrientation(
    val startOrientation: Orientation,
    val basketPosition: Vec2,
    val parkingOrientation: Orientation,
    val gameColor: GameColor,
    val gamePosition: GamePosition
) {
    RED_CLOSE(
        GAME_CONFIGS.START_RED_CLOSE_ORIENTATION, GAME_CONFIGS.RED_BASKET_POSITION,
        GAME_CONFIGS.RED_PARKING_ORIENTATION, GameColor.RED, GamePosition.CLOSE
    ),
    RED_FAR(
        GAME_CONFIGS.START_RED_FAR_ORIENTATION, GAME_CONFIGS.RED_BASKET_POSITION,
        GAME_CONFIGS.RED_PARKING_ORIENTATION, GameColor.RED, GamePosition.FAR
    ),
    BLUE_CLOSE(
        GAME_CONFIGS.START_BLUE_CLOSE_ORIENTATION, GAME_CONFIGS.BLUE_BASKET_POSITION,
        GAME_CONFIGS.BLUE_PARKING_ORIENTATION, GameColor.BLUE, GamePosition.CLOSE
    ),
    BLUE_FAR(
        GAME_CONFIGS.START_BLUE_FAR_ORIENTATION, GAME_CONFIGS.BLUE_BASKET_POSITION,
        GAME_CONFIGS.BLUE_PARKING_ORIENTATION, GameColor.BLUE, GamePosition.FAR
    )
}

object GameSettings {
    var startOrientation = StartOrientation.BLUE_CLOSE
}