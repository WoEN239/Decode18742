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
        ROGER_STARTING_SHOOTING,
        FINISHED_IS_NOW_EMPTY,

        FAIL_UNKNOWN,
        FAIL_IS_EMPTY,
        FAIL_ILLEGAL_ARGUMENT,

        FAIL_COLORS_NOT_PRESENT,
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
        const val ROGER_STARTING_SHOOTING: Int = 0
        const val FINISHED_IS_NOW_EMPTY: Int = 1

        const val FAIL_UNKNOWN:  Int = 2
        const val FAIL_IS_EMPTY: Int = 3
        const val FAIL_ILLEGAL_ARGUMENT: Int = 4

        const val FAIL_COLORS_NOT_PRESENT: Int = 5
        const val FAIL_COULD_NOT_DETECT_PATTERN: Int = 9


        fun didFail(id:   Int)  = id > FINISHED_IS_NOW_EMPTY
        fun didFail(name: Name) = didFail(toInt(name))

        fun didSucceed(id:   Int)  = id < FAIL_UNKNOWN
        fun didSucceed(name: Name) = didSucceed(toInt(name))


        fun toName(id: Int):  Name
        {
            return when (id)
            {
                ROGER_STARTING_SHOOTING -> Name.ROGER_STARTING_SHOOTING
                FINISHED_IS_NOW_EMPTY   -> Name.FINISHED_IS_NOW_EMPTY

                FAIL_UNKNOWN  -> Name.FAIL_UNKNOWN
                FAIL_IS_EMPTY -> Name.FAIL_IS_EMPTY
                FAIL_ILLEGAL_ARGUMENT -> Name.FAIL_ILLEGAL_ARGUMENT

                FAIL_COLORS_NOT_PRESENT -> Name.FAIL_COLORS_NOT_PRESENT
                else -> Name.FAIL_COULD_NOT_DETECT_PATTERN
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.ROGER_STARTING_SHOOTING -> ROGER_STARTING_SHOOTING
                Name.FINISHED_IS_NOW_EMPTY   -> FINISHED_IS_NOW_EMPTY

                Name.FAIL_UNKNOWN  -> FAIL_UNKNOWN
                Name.FAIL_IS_EMPTY -> FAIL_IS_EMPTY
                Name.FAIL_ILLEGAL_ARGUMENT -> FAIL_ILLEGAL_ARGUMENT

                Name.FAIL_COLORS_NOT_PRESENT -> FAIL_COLORS_NOT_PRESENT
                Name.FAIL_COULD_NOT_DETECT_PATTERN -> FAIL_COULD_NOT_DETECT_PATTERN
            }
        }
    }
}