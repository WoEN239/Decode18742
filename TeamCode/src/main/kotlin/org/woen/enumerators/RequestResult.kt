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
        SUCCESS_IS_NOW_EMPTY,

        FAIL_UNKNOWN,
        FAIL_IS_EMPTY,
        FAIL_ILLEGAL_ARGUMENT,

        FAIL_COLOR_NOT_PRESENT,
        FAIL_NOT_ENOUGH_COLORS,

        FAIL_HARDWARE_PROBLEM,
        FAIL_SOFTWARE_STORAGE_DESYNC,
        FAIL_COULD_NOT_DETECT_PATTERN
    }



    fun set(name: Name, id:   Int  = toInt(name)) = set(id, name)
    fun set(id:   Int,  name: Name = toName(id))
    {
        _id   = id
        _name = name
    }


    val id   get() = _id
    val name get() = _name



    companion object
    {
        const val SUCCESS: Int = 0
        const val SUCCESS_IS_NOW_EMPTY: Int = 1

        const val FAIL_UNKNOWN:  Int = 2
        const val FAIL_IS_EMPTY: Int = 3
        const val FAIL_ILLEGAL_ARGUMENT: Int = 4

        const val FAIL_COLOR_NOT_PRESENT: Int = 5
        const val FAIL_NOT_ENOUGH_COLORS: Int = 6

        const val FAIL_HARDWARE_PROBLEM:         Int = 7
        const val FAIL_SOFTWARE_STORAGE_DESYNC:  Int = 8
        const val FAIL_COULD_NOT_DETECT_PATTERN: Int = 9


        fun didFail(id:   Int)  = id > SUCCESS_IS_NOW_EMPTY
        fun didFail(name: Name) = didFail(toInt(name))

        fun didSucceed(id:   Int)  = id < FAIL_UNKNOWN
        fun didSucceed(name: Name) = didSucceed(toInt(name))


        fun toName(id: Int):  Name
        {
            return when (id)
            {
                SUCCESS -> Name.SUCCESS
                SUCCESS_IS_NOW_EMPTY -> Name.SUCCESS_IS_NOW_EMPTY

                FAIL_UNKNOWN  -> Name.FAIL_UNKNOWN
                FAIL_IS_EMPTY -> Name.FAIL_IS_EMPTY
                FAIL_ILLEGAL_ARGUMENT -> Name.FAIL_ILLEGAL_ARGUMENT

                FAIL_COLOR_NOT_PRESENT -> Name.FAIL_COLOR_NOT_PRESENT
                FAIL_NOT_ENOUGH_COLORS -> Name.FAIL_NOT_ENOUGH_COLORS

                FAIL_HARDWARE_PROBLEM         -> Name.FAIL_HARDWARE_PROBLEM
                FAIL_SOFTWARE_STORAGE_DESYNC  -> Name.FAIL_SOFTWARE_STORAGE_DESYNC
                else -> Name.FAIL_COULD_NOT_DETECT_PATTERN
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
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
            }
        }
    }
}