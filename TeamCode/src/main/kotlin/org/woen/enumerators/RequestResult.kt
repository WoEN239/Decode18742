package org.woen.enumerators



class RequestResult
{
    private var _id   = FAIL_UNKNOWN
    private var _name = Name.FAIL_UNKNOWN


    constructor() { set(FAIL_UNKNOWN, Name.FAIL_UNKNOWN) }
    constructor(id:   Int,  name: Name = toName(id))  { set(id, name) }
    constructor(name: Name, id:   Int  = toInt(name)) { set(id, name) }



    enum class Name
    {
        SUCCESS,
        SUCCESS_BOTTOM,
        SUCCESS_CENTER,
        SUCCESS_TURRET,
        SUCCESS_MOBILE,
        SUCCESS_IS_NOW_EMPTY,

        FAIL_UNKNOWN,
        FAIL_IS_EMPTY,
        FAIL_ILLEGAL_ARGUMENT,

        FAIL_COLOR_NOT_PRESENT,
        FAIL_NOT_ENOUGH_COLORS,

        FAIL_HARDWARE_PROBLEM,
        FAIL_SOFTWARE_STORAGE_DESYNC,
        FAIL_COULD_NOT_DETECT_PATTERN,

        FAIL_IS_CURRENTLY_BUSY,
        FAIL_PROCESS_WAS_TERMINATED
    }



    fun set(name: Name, id:   Int  = toInt(name)) = set(id, name)
    fun set(id:   Int,  name: Name = toName(id))
    {
        _id   = id
        _name = name
    }


    fun id()   = _id
    fun name() = _name


    fun didFail()       = _id > SUCCESS_IS_NOW_EMPTY
    fun didSucceed()    = _id < FAIL_UNKNOWN

    fun wasTerminated() = _id == FAIL_PROCESS_WAS_TERMINATED


    companion object
    {
        const val SUCCESS_BOTTOM: Int = 0
        const val SUCCESS_CENTER: Int = 1
        const val SUCCESS_TURRET: Int = 2
        const val SUCCESS_MOBILE: Int = 3

        const val SUCCESS: Int = 4
        const val SUCCESS_IS_NOW_EMPTY: Int = 5

        const val FAIL_UNKNOWN:  Int = 6
        const val FAIL_IS_EMPTY: Int = 7
        const val FAIL_ILLEGAL_ARGUMENT: Int = 8

        const val FAIL_COLOR_NOT_PRESENT: Int = 9
        const val FAIL_NOT_ENOUGH_COLORS: Int = 10

        const val FAIL_HARDWARE_PROBLEM:         Int = 11
        const val FAIL_SOFTWARE_STORAGE_DESYNC:  Int = 12
        const val FAIL_COULD_NOT_DETECT_PATTERN: Int = 13

        const val FAIL_IS_CURRENTLY_BUSY:      Int = 14
        const val FAIL_PROCESS_WAS_TERMINATED: Int = 15


        fun wasTerminated(requestResult: Int)  = requestResult == FAIL_PROCESS_WAS_TERMINATED
        fun wasTerminated(requestResult: Name) = wasTerminated(toInt(requestResult))

        fun didFail(id:   Int)  = id > SUCCESS_IS_NOW_EMPTY
        fun didFail(name: Name) = didFail(toInt(name))

        fun didSucceed(id:   Int)  = id < FAIL_UNKNOWN
        fun didSucceed(name: Name) = didSucceed(toInt(name))


        fun toName(id: Int):  Name
        {
            return when (id)
            {
                SUCCESS_BOTTOM -> Name.SUCCESS_BOTTOM
                SUCCESS_CENTER -> Name.SUCCESS_CENTER
                SUCCESS_TURRET -> Name.SUCCESS_TURRET
                SUCCESS_MOBILE -> Name.SUCCESS_MOBILE

                SUCCESS -> Name.SUCCESS
                SUCCESS_IS_NOW_EMPTY -> Name.SUCCESS_IS_NOW_EMPTY

                FAIL_UNKNOWN  -> Name.FAIL_UNKNOWN
                FAIL_IS_EMPTY -> Name.FAIL_IS_EMPTY
                FAIL_ILLEGAL_ARGUMENT -> Name.FAIL_ILLEGAL_ARGUMENT

                FAIL_COLOR_NOT_PRESENT -> Name.FAIL_COLOR_NOT_PRESENT
                FAIL_NOT_ENOUGH_COLORS -> Name.FAIL_NOT_ENOUGH_COLORS

                FAIL_HARDWARE_PROBLEM         -> Name.FAIL_HARDWARE_PROBLEM
                FAIL_SOFTWARE_STORAGE_DESYNC  -> Name.FAIL_SOFTWARE_STORAGE_DESYNC
                FAIL_COULD_NOT_DETECT_PATTERN -> Name.FAIL_COULD_NOT_DETECT_PATTERN

                FAIL_IS_CURRENTLY_BUSY -> Name.FAIL_IS_CURRENTLY_BUSY
                else -> Name.FAIL_PROCESS_WAS_TERMINATED
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.SUCCESS_BOTTOM -> SUCCESS_BOTTOM
                Name.SUCCESS_CENTER -> SUCCESS_CENTER
                Name.SUCCESS_TURRET -> SUCCESS_TURRET
                Name.SUCCESS_MOBILE -> SUCCESS_MOBILE

                Name.SUCCESS -> SUCCESS
                Name.SUCCESS_IS_NOW_EMPTY -> SUCCESS_IS_NOW_EMPTY

                Name.FAIL_UNKNOWN  -> FAIL_UNKNOWN
                Name.FAIL_IS_EMPTY -> FAIL_IS_EMPTY
                Name.FAIL_ILLEGAL_ARGUMENT -> FAIL_ILLEGAL_ARGUMENT

                Name.FAIL_COLOR_NOT_PRESENT -> FAIL_COLOR_NOT_PRESENT
                Name.FAIL_NOT_ENOUGH_COLORS -> FAIL_NOT_ENOUGH_COLORS

                Name.FAIL_HARDWARE_PROBLEM         -> FAIL_HARDWARE_PROBLEM
                Name.FAIL_SOFTWARE_STORAGE_DESYNC  -> FAIL_SOFTWARE_STORAGE_DESYNC
                Name.FAIL_COULD_NOT_DETECT_PATTERN -> FAIL_COULD_NOT_DETECT_PATTERN

                Name.FAIL_IS_CURRENTLY_BUSY      -> FAIL_IS_CURRENTLY_BUSY
                Name.FAIL_PROCESS_WAS_TERMINATED -> FAIL_PROCESS_WAS_TERMINATED
            }
        }
    }
}