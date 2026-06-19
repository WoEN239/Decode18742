package org.woen.enumerators.phases


import org.woen.configs.Hardware



class ShootingPhase
{
    private var _name : Name
    var ballCountForPhase1 : Int = 0
    var shotBeltsVoltage: Double = Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING


    constructor(name: Name = Name.NOT_ACTIVE) { _name = name }
    constructor(shootingPhase: ShootingPhase)
    {
        _name = shootingPhase._name
        ballCountForPhase1 = shootingPhase.ballCountForPhase1
        shotBeltsVoltage = shootingPhase.shotBeltsVoltage
    }


    enum class Name
    {
        NOT_ACTIVE,

        P0_AWAITING_SORTING,

        P1_OPENING_TURRET_GATE,
        P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD,

        P2_SHOOT_BELTS_ON_TIME,
        P2_SHOOT_BELTS_ON_GAMEPAD_HOLD,
        P2_SHOOT_UNTIL_EMPTY_USING_COLORS,

        P3_OPENING_LAUNCHER,
        P4_CALIBRATING
    }


    val name get() = _name


    fun setInactive() { _name = Name.NOT_ACTIVE }
    fun startPhase0() { _name = Name.P0_AWAITING_SORTING }
    fun startPhase1(laterGamepadHold: Boolean)
    {
        _name = if (!laterGamepadHold) Name.P1_OPENING_TURRET_GATE
               else Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD
    }
    fun startPhase2(useColorSensors: Boolean)
    {
        _name =  if (isGamepadHoldPhase1())
                 Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD
            else if (useColorSensors)
                 Name.P2_SHOOT_UNTIL_EMPTY_USING_COLORS
            else Name.P2_SHOOT_BELTS_ON_TIME
    }
    fun startPhase3() { _name = Name.P3_OPENING_LAUNCHER }
    fun startPhase4() { _name = Name.P4_CALIBRATING }


    fun isInactive() = _name == Name.NOT_ACTIVE
    fun isActive()   = _name != Name.NOT_ACTIVE
    fun isNotShooting() = isInactive() || isShootingPhase4()


    fun isShootingPhase0()    = _name == Name.P0_AWAITING_SORTING
    fun isGamepadHoldPhase1() = _name == Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD


    fun isAnyPhase2() = isRegularPhase2() || isNotOnTimePhase2()
    fun isNotOnTimePhase2() = isGamepadHoldPhase2() || isUntilColorsPhase2()
    fun isRegularPhase2()     = _name == Name.P2_SHOOT_BELTS_ON_TIME
    fun isUntilColorsPhase2() = _name == Name.P2_SHOOT_UNTIL_EMPTY_USING_COLORS
    fun isGamepadHoldPhase2() = _name == Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD


    fun isShootingPhase3() = _name == Name.P3_OPENING_LAUNCHER
    fun isShootingPhase4() = _name == Name.P4_CALIBRATING
}