package org.woen.modules.scoringSystem.storage


import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult
import org.woen.hotRun.HotRun

import org.woen.telemetry.LogManager
import org.woen.telemetry.configs.Configs
import org.woen.telemetry.configs.RobotSettings.ROBOT

import org.woen.threading.ThreadManager
import org.woen.threading.ThreadedEventBus
import org.woen.threading.ThreadedGamepad



object Alias
{
    object Intake
    {
        val FAIL_IS_BUSY = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        val FAIL_IS_FULL = IntakeResult.Name.FAIL_STORAGE_IS_FULL
        val SUCCESS = IntakeResult.Name.SUCCESS


        val INPUT_ORDER = ROBOT.PREFERRED_INTAKE_SLOT_SEARCHING_ORDER
    }
    object Request
    {
        val SUCCESS_NOW_EMPTY = RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        val SUCCESS = RequestResult.Name.SUCCESS

        val TERMINATED = RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED

        val FAIL_UNKNOWN  = RequestResult.Name.FAIL_UNKNOWN
        val FAIL_IS_EMPTY = RequestResult.Name.FAIL_IS_EMPTY
        val FAIL_IS_BUSY  = RequestResult.Name.FAIL_IS_CURRENTLY_BUSY
        val ILLEGAL_ARGUMENT  = RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
        val COLOR_NOT_PRESENT = RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        val NOT_ENOUGH_COLORS = RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
        val COULD_NOT_DETECT_PATTERN = RequestResult.Name.FAIL_COULD_NOT_DETECT_PATTERN


        val SEARCH_ORDER = ROBOT.PREFERRED_REQUEST_SLOT_SEARCHING_ORDER



        val MOBILE_SLOT = RequestResult.Name.SUCCESS_MOBILE
        val BOTTOM_SLOT = RequestResult.Name.SUCCESS_BOTTOM
        val CENTER_SLOT = RequestResult.Name.SUCCESS_CENTER
        val TURRET_SLOT = RequestResult(
            RequestResult.SUCCESS_TURRET,
            RequestResult.Name.SUCCESS_TURRET)



        val F_SUCCESS = RequestResult(
            RequestResult.SUCCESS,
            RequestResult.Name.SUCCESS)
        val F_TERMINATED = RequestResult(
            RequestResult.FAIL_PROCESS_WAS_TERMINATED,
            RequestResult.Name.FAIL_PROCESS_WAS_TERMINATED)
        val F_COLOR_NOT_PRESENT = RequestResult(
            RequestResult.FAIL_COLOR_NOT_PRESENT,
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT)

        val F_ILLEGAL_ARGUMENT = RequestResult(
            RequestResult.FAIL_ILLEGAL_ARGUMENT,
            RequestResult.Name.FAIL_ILLEGAL_ARGUMENT)

        val F_IS_EMPTY = RequestResult(
            RequestResult.FAIL_IS_EMPTY,
            RequestResult.Name.FAIL_IS_EMPTY)


        val IsReadyEvent = StorageRequestIsReadyEvent()


        fun didFail   (name: RequestResult.Name) = RequestResult.didFail(name)
        fun didSucceed(name: RequestResult.Name) = RequestResult.didSucceed(name)
        fun wasTerminated(name: RequestResult.Name) = RequestResult.wasTerminated(name)
    }

    object Delay
    {
        val PART_PUSH = Configs.DELAY.PART_BALL_PUSHING_MS
        val FULL_PUSH = Configs.DELAY.FULL_BALL_PUSHING_MS
        val HALF_PUSH = Configs.DELAY.FULL_BALL_PUSHING_MS / 2
    }


    val LogM get() = LogManager
    val HotRunLI   get() = HotRun.LAZY_INSTANCE
    val GamepadLI  get() = ThreadedGamepad.LAZY_INSTANCE
    val EventBusLI get() = ThreadedEventBus.LAZY_INSTANCE
    val SmartCoroutineLI get() = ThreadManager.LAZY_INSTANCE.globalCoroutineScope



    const val NOTHING            = 0
    const val MAX_BALL_COUNT     = 3
    const val STORAGE_SLOT_COUNT = 4
}