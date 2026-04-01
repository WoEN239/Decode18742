package org.woen.collector

import com.acmerobotics.dashboard.config.Config
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Vec2

@Config
internal object GAME_CONFIGS {
    @JvmField
    var START_RED_CLOSE_ORIENTATION =
        Orientation(Vec2(-1.205 - 0.01 - 0.38 / 2.0, 0.965 + 0.38 / 2.0 + 0.01), Angle.ofDeg(0.0))

    @JvmField
    var START_RED_FAR_ORIENTATION =
        Orientation(Vec2(0.01 + 1.35 + 0.38 / 2.0, 0.225 + 0.38 / 2.0 + 0.01), Angle.ofDeg(0.0))

    @JvmField
    var START_BLUE_CLOSE_ORIENTATION =
        Orientation(Vec2(-1.205 - 0.01 - 0.38 / 2.0, -0.965 - 0.38 / 2.0 - 0.01), Angle.ofDeg(0.0))

    @JvmField
    var START_BLUE_FAR_ORIENTATION =
        Orientation(Vec2(0.01 + 1.35 + 0.38 / 2.0, -0.225 - 0.38 / 2.0 - 0.01), Angle.ofDeg(0.0))

    @JvmField
    var BLUE_BASKET_POSITION = Vec2(-1.6, -1.7)

    @JvmField
    var RED_BASKET_POSITION = Vec2(-1.6, 1.7)

    @JvmField
    var BLUE_FAR_BASKET_POSITION = Vec2(-1.7, -1.6)

    @JvmField
    var RED_FAR_BASKET_POSITION = Vec2(-1.7, 1.6)

    @JvmField
    var RED_PARKING_ORIENTATION = Orientation(Vec2(0.0, 0.0), Angle.ofDeg(0.0))

    @JvmField
    var BLUE_PARKING_ORIENTATION = Orientation(Vec2(0.0, 0.0), Angle.ofDeg(0.0))

    @JvmField
    var RED_OBELISK_POSITION = Vec2(-3.66 / 2.0, 1.0)

    @JvmField
    var BLUE_OBELISK_POSITION = Vec2(-3.66 / 2.0, -1.0)

    @JvmField
    var CALIBRATE_ODOMETRY_BLUE_POSITION = Vec2(-0.794, -0.791)

    @JvmField
    var CALIBRATE_ODOMETRY_RED_POSITION = Vec2(-0.794, 0.791)
}

enum class GameColor {
    RED, BLUE
}

enum class GamePosition {
    FAR, CLOSE
}

enum class StartOrientation(
    val startOrientation: Orientation,
    val basketPosition: () -> Vec2,
    val farBasketPosition: () -> Vec2,
    val parkingOrientation: Orientation,
    val gameColor: GameColor,
    val gamePosition: GamePosition,
    val odometryCalibratePosition: Vec2
) {
    RED_CLOSE(
        GAME_CONFIGS.START_RED_CLOSE_ORIENTATION,
        { GAME_CONFIGS.RED_BASKET_POSITION },
        { GAME_CONFIGS.RED_FAR_BASKET_POSITION },
        GAME_CONFIGS.RED_PARKING_ORIENTATION,
        GameColor.RED,
        GamePosition.CLOSE,
        GAME_CONFIGS.CALIBRATE_ODOMETRY_RED_POSITION
    ),
    RED_FAR(
        GAME_CONFIGS.START_RED_FAR_ORIENTATION,
        { GAME_CONFIGS.RED_BASKET_POSITION },
        { GAME_CONFIGS.RED_FAR_BASKET_POSITION },
        GAME_CONFIGS.RED_PARKING_ORIENTATION,
        GameColor.RED,
        GamePosition.FAR,
        GAME_CONFIGS.CALIBRATE_ODOMETRY_RED_POSITION
    ),
    BLUE_CLOSE(
        GAME_CONFIGS.START_BLUE_CLOSE_ORIENTATION,
        { GAME_CONFIGS.BLUE_BASKET_POSITION },
        { GAME_CONFIGS.BLUE_FAR_BASKET_POSITION },
        GAME_CONFIGS.BLUE_PARKING_ORIENTATION,
        GameColor.BLUE,
        GamePosition.CLOSE,
        GAME_CONFIGS.CALIBRATE_ODOMETRY_BLUE_POSITION
    ),
    BLUE_FAR(
        GAME_CONFIGS.START_BLUE_FAR_ORIENTATION,
        { GAME_CONFIGS.BLUE_BASKET_POSITION },
        { GAME_CONFIGS.BLUE_FAR_BASKET_POSITION },
        GAME_CONFIGS.BLUE_PARKING_ORIENTATION,
        GameColor.BLUE,
        GamePosition.FAR,
        GAME_CONFIGS.CALIBRATE_ODOMETRY_BLUE_POSITION
    )
}

object GameSettings {
    var startOrientation = StartOrientation.BLUE_CLOSE
}