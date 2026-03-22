package org.woen.enumerators.phases

import org.woen.configs.Hardware


class ShootingPhase
{
    private var _name : Name
    var ballCountForPhase1 : Int = 0
    var shotBeltsVoltage: Double = Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING


    constructor(name: Name = Name.NOT_ACTIVE) { _name = name }


    enum class Name
    {
        NOT_ACTIVE,

        P0_AWAITING_SORTING,

        P1_OPENING_TURRET_GATE,
        P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD,

        P2_SHOOT_BELTS_ON_TIME,
        P2_SHOOT_BELTS_ON_GAMEPAD_HOLD,

        P3_OPENING_LAUNCHER,
        P4_CALIBRATING
    }


    val name get() = _name


    fun setInactive()
    {
        _name = Name.NOT_ACTIVE
    }
    fun startPhase0()
    {
        _name = Name.P0_AWAITING_SORTING
    }
    fun startPhase1(laterGamepadHold: Boolean)
    {
        _name = if (!laterGamepadHold) Name.P1_OPENING_TURRET_GATE
               else Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD
    }
    fun startPhase4()
    {
        _name = Name.P4_CALIBRATING
    }
    fun switchToNextPhase()
    {
        _name = when (_name)
        {
            Name.NOT_ACTIVE             -> Name.P0_AWAITING_SORTING
            Name.P0_AWAITING_SORTING    -> Name.P1_OPENING_TURRET_GATE

            Name.P1_OPENING_TURRET_GATE -> Name.P2_SHOOT_BELTS_ON_TIME
            Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD
                -> Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD

            Name.P2_SHOOT_BELTS_ON_TIME         -> Name.P3_OPENING_LAUNCHER
            Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD -> Name.P3_OPENING_LAUNCHER

            Name.P3_OPENING_LAUNCHER -> Name.P4_CALIBRATING
            Name.P4_CALIBRATING      -> Name.NOT_ACTIVE
        }
    }


    fun isInactive() = _name == Name.NOT_ACTIVE
    fun isActive()   = !isInactive()

    fun isShootingPhase0() = _name == Name.P0_AWAITING_SORTING

    fun isRegularPhase2()  = _name == Name.P2_SHOOT_BELTS_ON_TIME
    fun isGamepadHoldPhase2() = _name == Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD
    fun isAnyShootingPhase2() = isRegularPhase2() || isGamepadHoldPhase2()
    fun isShootingPhase3() = _name == Name.P3_OPENING_LAUNCHER
    fun isShootingPhase4() = _name == Name.P4_CALIBRATING
}