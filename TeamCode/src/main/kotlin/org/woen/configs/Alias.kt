package org.woen.configs


import org.woen.enumerators.IntakeResult
import org.woen.enumerators.RequestResult



object Alias
{
    object Intake
    {
        val FAIL_IS_BUSY = IntakeResult.Name.FAIL_IS_CURRENTLY_BUSY
        val FAIL_IS_FULL = IntakeResult.Name.FAIL_STORAGE_IS_FULL
        val SUCCESS      = IntakeResult.Name.SUCCESS


        val INPUT_ORDER = RobotSettings.ROBOT.PREFERRED_INTAKE_SLOT_SEARCHING_ORDER
    }
    object Request
    {
        val SUCCESS_NOW_EMPTY = RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        val SUCCESS           = RequestResult.Name.SUCCESS

        val FAIL_UNKNOWN  = RequestResult.Name.FAIL_UNKNOWN
        val FAIL_IS_EMPTY = RequestResult.Name.FAIL_IS_EMPTY
        val FAIL_IS_BUSY  = RequestResult.Name.FAIL_IS_CURRENTLY_BUSY
        val ILLEGAL_ARGUMENT  = RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
        val COLOR_NOT_PRESENT = RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        val NOT_ENOUGH_COLORS = RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
        val COULD_NOT_DETECT_PATTERN = RequestResult.Name.FAIL_COULD_NOT_DETECT_PATTERN


        val SEARCH_ORDER = RobotSettings.ROBOT.PREFERRED_REQUEST_SLOT_SEARCHING_ORDER



        val MOBILE_SLOT = RequestResult.Name.SUCCESS_MOBILE
        val BOTTOM_SLOT = RequestResult.Name.SUCCESS_BOTTOM
        val CENTER_SLOT = RequestResult.Name.SUCCESS_CENTER
        val TURRET_SLOT = RequestResult(
            RequestResult.Companion.SUCCESS_TURRET,
            RequestResult.Name.SUCCESS_TURRET)



        val F_SUCCESS = RequestResult(
            RequestResult.Companion.SUCCESS,
            RequestResult.Name.SUCCESS)
        val F_COLOR_NOT_PRESENT = RequestResult(
            RequestResult.Companion.FAIL_COLOR_NOT_PRESENT,
            RequestResult.Name.FAIL_COLOR_NOT_PRESENT)

        val F_ILLEGAL_ARGUMENT = RequestResult(
            RequestResult.Companion.FAIL_ILLEGAL_ARGUMENT,
            RequestResult.Name.FAIL_ILLEGAL_ARGUMENT)

        val F_IS_EMPTY = RequestResult(
            RequestResult.Companion.FAIL_IS_EMPTY,
            RequestResult.Name.FAIL_IS_EMPTY)



        fun didFail      (name: RequestResult.Name) = RequestResult.Companion.didFail(name)
        fun didSucceed   (name: RequestResult.Name) = RequestResult.Companion.didSucceed(name)
        fun wasTerminated(name: RequestResult.Name) = RequestResult.Companion.wasTerminated(name)
    }



    const val MAX_BALL_COUNT     = 3
    const val STORAGE_SLOT_COUNT = 4
}