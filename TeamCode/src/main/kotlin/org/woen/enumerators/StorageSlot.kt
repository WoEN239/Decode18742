package org.woen.enumerators



class StorageSlot
{
    private var _name = Name.UNDEFINED
    private var _id   = UNDEFINED


    constructor() { set(UNDEFINED, Name.UNDEFINED) }
    constructor(id:   Int,  name: Name = toName(id))  { set(id, name) }
    constructor(name: Name, id:   Int  = toInt(name)) { set(id, name) }


    enum class Name
    {
        BOTTOM,
        CENTER,
        TURRET,
        MOBILE,
        UNDEFINED
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
        const val BOTTOM:    Int = 0
        const val CENTER:    Int = 1
        const val TURRET:    Int = 2
        const val MOBILE:    Int = 3
        const val UNDEFINED: Int = 5

        fun toName(id: Int):  Name
        {
            return when (id)
            {
                BOTTOM -> Name.BOTTOM
                CENTER -> Name.CENTER
                TURRET -> Name.TURRET
                MOBILE -> Name.MOBILE
                else   -> Name.UNDEFINED
            }
        }
        fun toInt(name: Name): Int
        {
            return when (name)
            {
                Name.BOTTOM    -> BOTTOM
                Name.CENTER    -> CENTER
                Name.TURRET    -> TURRET
                Name.MOBILE    -> MOBILE
                Name.UNDEFINED -> UNDEFINED
            }
        }
    }
}