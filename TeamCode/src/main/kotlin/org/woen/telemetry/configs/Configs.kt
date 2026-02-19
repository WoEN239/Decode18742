package org.woen.telemetry.configs


import kotlin.math.PI

import com.acmerobotics.dashboard.config.Config
import org.woen.telemetry.EventConfig
import org.woen.telemetry.ThreadedTelemetry
import org.woen.utils.regulator.RegulatorParameters
import org.woen.utils.units.Angle
import org.woen.utils.units.Orientation
import org.woen.utils.units.Triangle
import org.woen.utils.units.Vec2



/*
 *    Это один из файлов с конфигами робота.
 *    В этих файлах и только в них разрешаются ЭДЖОВСКИЕ технологии ._.
 *
 *    Конкретный тип разрешённых эджовских технологий:
 *      > Подстановка ПРЕКРАСНЕЙШИХ значений как например 33333 <
 *         (на значениях которые не требуют больших точностей)
 */



object Configs {
    @Config
    internal object BRUSH {
        @JvmField
        var BRUSH_SAFE_TIME = 0.5

        @JvmField
        var BRUSH_ERR_TIME = 0.5

        @JvmField
        var BRUSH_DEF_TIME = 1.5

        @JvmField
        var BRUSH_TARGET_CURRENT = 67.0

        @JvmField
        var BRUSH_BIG_TARGET_CURRENT = 1000.0

        @JvmField
        var REVERSE_TIME: Long = 999

        @JvmField
        var BRUSH_POWER = 7.0
    }


    @Config
    internal object SERVO_ANGLE {
        @JvmField
        var DEFAULT_SERVO_ANGLE = PI * 1.5

        @JvmField
        var DEFAULT_SERVO_V_MAX = PI * 1.5 * 15.0

        @JvmField
        var DEFAULT_SERVO_A = PI * 1.5 * 15.0

        @JvmField
        var DEFAULT_SERVO_OFFSET = 0.1
    }

    @Config
    internal object THREAD_POOL {
        @JvmField
        var THREAD_POOL_THREADS_COUNT = 5
    }

    @Config
    internal object ODOMETRY {
        @JvmField
        var START_RED_CLOSE_ORIENTATION =
            Orientation(Vec2(-1.208 - 0.01 - 0.38 / 2.0, 0.87 + 0.38 / 2.0 + 0.01), Angle(PI))

        @JvmField
        var START_RED_FAR_ORIENTATION = Orientation(Vec2(1.63, 0.39), Angle(PI))

        @JvmField
        var START_BLUE_CLOSE_ORIENTATION =
            Orientation(Vec2(-1.208 - 0.01 - 0.38 / 2.0, -0.87 - 0.38 / 2.0 - 0.01), Angle(PI))

        @JvmField
        var START_BLUE_FAR_ORIENTATION = Orientation(Vec2(1.631, -0.39), Angle(PI))

        @JvmField
        var X_ODOMETER_POSITION = -0.0995

        @JvmField
        var Y_ODOMETER_POSITION = -0.0895

        @EventConfig
        var MERGE_X_K = ThreadedTelemetry.EventValueProvider(0.7)

        @EventConfig
        var MERGE_Y_K = ThreadedTelemetry.EventValueProvider(0.7)

        @EventConfig
        var MERGE_H_K = ThreadedTelemetry.EventValueProvider(0.7)

        @JvmField
        var TIMEOUT_REQUEST_TIME = 0.1
    }

    @Config
    internal object DRIVE_TRAIN {
        @JvmField
        var DRIVE_SIDE_REGULATOR_PARAMS = RegulatorParameters(kF = 11.0, kP = 8.0)

        @JvmField
        var DRIVE_FORWARD_REGULATOR_PARAMS = RegulatorParameters(kF = 8.0, kP = 8.0)

        @JvmField
        var DRIVE_ROTATE_REGULATOR_PARAMS = RegulatorParameters(kF = 2.0, kP = 1.2)

        @JvmField
        var MAX_DRIVE_VELOCITY = 2.0

        @JvmField
        var MAX_DRIVE_ANGLE_VELOCITY = 7.0

        @JvmField
        var POW_MOVE_ENABLED = true

        @JvmField
        var ROBOT_SIZE = Vec2(0.38, 0.38)

        @JvmField
        var SHOOT_SHORT_TRIANGLE = Triangle(Vec2(-1.83, 1.83), Vec2(0.0, 0.0), Vec2(-1.83, -1.83))

        @JvmField
        var SHOOT_LONG_TRIANGLE = Triangle(Vec2(1.83, 0.61), Vec2(1.22, 0.0), Vec2(1.83, -0.61))

        @JvmField
        var H_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 7.5, kD = 0.3, limitU = MAX_DRIVE_ANGLE_VELOCITY)

        @JvmField
        var X_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 4.0, kD = 0.1, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var Y_REGULATOR_PARAMETERS =
            RegulatorParameters(kP = 4.0, kD = 0.1, limitU = MAX_DRIVE_VELOCITY)

        @JvmField
        var SHOOTING_P = 4.0

        @JvmField
        var H_SENS = 0.3

        @JvmField
        var POS_SENS = 0.1

        @JvmField
        var TARGET_TIMER = 0.1

        @JvmField
        var RED_PARKING_ORIENTATION =
            Orientation(Vec2(0.948, -0.803), Angle.ofDeg(-180.0))

        @JvmField
        var BLUE_PARKING_ORIENTATION = Orientation(Vec2(0.948, 0.803), Angle.ofDeg(180.0))

        @JvmField
        var X_DEATH_ZONE = 0.05

        @JvmField
        var Y_DEATH_ZONE = 0.05

        @JvmField
        var H_DEATH_ZONE = 0.05
    }

    @Config
    internal object ROAD_RUNNER {
        @JvmField
        var ROAD_RUNNER_POS_X_P = 1.9

        @JvmField
        var ROAD_RUNNER_POS_Y_P = 1.9

        @JvmField
        var ROAD_RUNNER_POS_H_P = 5.0

        @JvmField
        var ROAD_RUNNER_VEL_X_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_Y_P = 0.0

        @JvmField
        var ROAD_RUNNER_VEL_H_P = 0.0

        @JvmField
        var ROAD_RUNNER_TRANSLATE_VELOCITY = 1.9

        @JvmField
        var ROAD_RUNNER_ROTATE_VELOCITY = 7.0

        @JvmField
        var ROAD_RUNNER_ROTATE_ACCEL = 14.0

        @JvmField
        var ROAD_RUNNER_MIN_TRANSLATION_ACCEL = -10.0

        @JvmField
        var ROAD_RUNNER_MAX_TRANSLATION_ACCEL = 10.0
    }

    @Config
    internal object CAMERA {
        @JvmField
        var CAMERA_ENABLE = true

        @JvmField
        var CAMERA_TURRET_POS = Vec2(0.0, 0.164)
    }

    @Config
    internal object TURRET {
        @JvmField
        var PULLEY_RADIUS = 0.0425

        @JvmField
        var PULLEY_TICKS_IN_REVOLUTION = 28.0

        @JvmField
        var PULLEY_REGULATOR =
            RegulatorParameters(kF = 0.0055, kP = 0.08, kI = 0.01 /*, limitU = 12.0*/)

        @JvmField
        var REGULATOR_SENS = 0.5

        @JvmField
        var TARGET_TIMER = 0.1

        @JvmField
        var BLUE_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, -3.66 / 2.0 + 0.2)

        @JvmField
        var RED_BASKET_POSITION = Vec2(-3.66 / 2.0 + 0.2, 3.66 / 2.0 - 0.2)

        @JvmField
        var MIN_TURRET_ANGLE_SERVO = 1.0 - 0.93

        @JvmField
        var MAX_TURRET_ANGLE_SERVO = 1.0 - 0.15

        @JvmField
        var MIN_TURRET_ANGLE = PI / 2.0 - Math.toRadians(45.0)

        @JvmField
        var MAX_TURRET_ANGLE = PI / 2.0 - Math.toRadians(25.0)

        @JvmField
        var TURRET_CENTER_POS = Vec2(0.0, -0.035)

        @JvmField
        var ZERO_ROTATE_POS = Math.toRadians(167.81818181818184)

        @JvmField
        var ROTATE_SERVO_RATIO = (30.0 / 18.0) * (60.0 / 120.0)

        @JvmField
        var ROTATE_SERVO_REGULATOR =
            RegulatorParameters(kP = 0.85, kD = 0.015, limitU = 1.0)

        @JvmField
        var MAX_ROTATE = Math.toRadians(343.0) - ZERO_ROTATE_POS

        @JvmField
        var MIN_ROTATE = Math.toRadians(-12.0) - ZERO_ROTATE_POS

        @JvmField
        var SHOOTING_RED_ORIENTATION =
            Orientation(Vec2(-0.630, 0.387), Angle.ofDeg(129.0))

        @JvmField
        var SHOOTING_BLUE_ORIENTATION =
            Orientation(Vec2(-0.630, -0.387), Angle.ofDeg(-129.0))

        @JvmField
        var OBELISK_POSITION = Vec2(-3.66 / 2.0, 0.0)

        @JvmField
        var GRAVITY_G = 9.80665

        @JvmField
        var SCORE_HEIGHT = 1.12

        @JvmField
        var TURRET_HEIGHT = 0.33

        @JvmField
        var SCORE_ANGLE = Math.toRadians(-20.0)

        @JvmField
        var PULLEY_RATION = 30.0 / 40.0

        @JvmField
        var PULLEY_U = 0.47
    }



    @Config
    internal object SIMPLE_STORAGE {
        @JvmField
        var REVERS_TIME = 0.2

        @JvmField
        var BELTS_FULL_CURRENT = 8.9

        @JvmField
        var BELTS_FULL_TIMER = 0.1

        @JvmField
        var BELTS_POWER = 11.0

        @JvmField
        var BELTS_SHOOT_POWER = 11.0

        @JvmField
        var SHOOTING_TIME = 0.5

        @JvmField
        var PUSH_TIME = 0.5

        @JvmField
        var COLOR_THRESHOLD = 60.0
    }

    @Config
    internal object BATTERY {
        @JvmField
        var LOW_VOLTAGE = 9.5

        @JvmField
        var LOW_VOLTAGE_TRIGGER_TIME = 0.2
    }

    @Config
    internal object LIGHT {

        @JvmField
        var BLUE_R_POWER = 0.0

        @JvmField
        var BLUE_G_POWER = 1.0

        @JvmField
        var BLUE_B_POWER = 1.0

        @JvmField
        var GREEN_R_POWER = 0.0

        @JvmField
        var GREEN_G_POWER = 1.0

        @JvmField
        var GREEN_B_POWER = 0.0

        @JvmField
        var ORANGE_R_POWER = 1.0

        @JvmField
        var ORANGE_G_POWER = 0.07

        @JvmField
        var ORANGE_B_POWER = 0.0


        @JvmField
        var RED_R_POWER = 1.0

        @JvmField
        var RED_G_POWER = 0.0

        @JvmField
        var RED_B_POWER = 0.0


        @JvmField
        var RED_FLASHING_SPEED = 4.0
    }
}