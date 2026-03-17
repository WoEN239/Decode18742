package org.woen.configs


import org.woen.enumerators.RequestResult



object Alias
{
    object Request
    {
        val SUCCESS_NOW_EMPTY = RequestResult.Name.SUCCESS_IS_NOW_EMPTY
        val SUCCESS = RequestResult.Name.SUCCESS

        val FAIL_UNKNOWN  = RequestResult.Name.FAIL_UNKNOWN
        val FAIL_IS_EMPTY = RequestResult.Name.FAIL_IS_EMPTY
        val ILLEGAL_ARGUMENT  = RequestResult.Name.FAIL_ILLEGAL_ARGUMENT
        val COLOR_NOT_PRESENT = RequestResult.Name.FAIL_COLOR_NOT_PRESENT
        val NOT_ENOUGH_COLORS = RequestResult.Name.FAIL_NOT_ENOUGH_COLORS
        val COULD_NOT_DETECT_PATTERN = RequestResult.Name.FAIL_COULD_NOT_DETECT_PATTERN


        val SEARCH_ORDER = RobotSettings.ROBOT.PREFERRED_REQUEST_SLOT_SEARCHING_ORDER
    }


    val INTAKE_INPUT_ORDER = RobotSettings.ROBOT.PREFERRED_INTAKE_SLOT_SEARCHING_ORDER

    const val MAX_BALL_COUNT     = 3
    const val STORAGE_SLOT_COUNT = 4
}