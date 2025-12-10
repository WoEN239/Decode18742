package org.woen.enumerators



class IntakeResult
{
    private var _id   = FAIL_UNKNOWN
    private var _name = Name.FAIL_UNKNOWN


    constructor() { set(FAIL_UNKNOWN, Name.FAIL_UNKNOWN) }
    constructor(id:   Int,  name: Name = toName(id))  { set(id, name) }
    constructor(name: Name, id:   Int  = toInt(name)) { set(id, name) }


    enum class Name
    {
        SUCCESS_BOTTOM,
        SUCCESS_CENTER,
        SUCCESS_TURRET,
        SUCCESS_MOBILE,

        SUCCESS,
        STARTED_SUCCESSFULLY,

        FAIL_UNKNOWN,
        FAIL_STORAGE_IS_FULL,
        FAIL_HARDWARE_PROBLEM,

        FAIL_IS_CURRENTLY_BUSY,
        FAIL_PROCESS_WAS_TERMINATED,
    }


    fun set(name: Name,  id: Int  = toInt(name)) = set(id, name)
    fun set(id:   Int, name: Name = toName(id))
    {
        _id   = id
        _name = name
    }


    fun id()   = _id
    fun name() = _name


    fun didFail()    = _id > STARTED_SUCCESSFULLY
    fun didSucceed() = _id < FAIL_UNKNOWN


    companion object
    {
        const val SUCCESS_BOTTOM: Int = 0
        const val SUCCESS_CENTER: Int = 1
        const val SUCCESS_TURRET: Int = 2
        const val SUCCESS_MOBILE: Int = 3

        const val SUCCESS: Int = 4
        const val STARTED_SUCCESSFULLY: Int = 5

        const val FAIL_UNKNOWN: Int = 6
        const val FAIL_STORAGE_IS_FULL:  Int = 7
        const val FAIL_HARDWARE_PROBLEM: Int = 8

        const val FAIL_IS_CURRENTLY_BUSY:      Int = 9
        const val FAIL_PROCESS_WAS_TERMINATED: Int = 10


        fun didFail(id:   Int)  = id > STARTED_SUCCESSFULLY
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
                STARTED_SUCCESSFULLY -> Name.STARTED_SUCCESSFULLY

                FAIL_UNKNOWN -> Name.FAIL_UNKNOWN
                FAIL_STORAGE_IS_FULL  -> Name.FAIL_STORAGE_IS_FULL
                FAIL_HARDWARE_PROBLEM -> Name.FAIL_HARDWARE_PROBLEM

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
                Name.STARTED_SUCCESSFULLY -> STARTED_SUCCESSFULLY

                Name.FAIL_UNKNOWN -> FAIL_UNKNOWN
                Name.FAIL_STORAGE_IS_FULL  -> FAIL_STORAGE_IS_FULL
                Name.FAIL_HARDWARE_PROBLEM -> FAIL_HARDWARE_PROBLEM

                Name.FAIL_IS_CURRENTLY_BUSY      -> FAIL_IS_CURRENTLY_BUSY
                Name.FAIL_PROCESS_WAS_TERMINATED -> FAIL_PROCESS_WAS_TERMINATED
            }
        }
    }
}