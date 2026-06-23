package org.woen.enumerators.phases


import org.woen.configs.Hardware



class ShootingPhase
{
    private var _name: Name
    var ballCountForPhase1: Int = 0
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
        P4_CALIBRATE_REVERSING_BELTS,
        P5_CALIBRATE_CLOSING_ALL_SERVOS,
        P6_REALIGNING_FORWARDS
    }

    val name get() = _name


    fun setInactive() { _name = Name.NOT_ACTIVE }
    fun startP0() { _name = Name.P0_AWAITING_SORTING }
    fun startP1(laterGamepadHold: Boolean)
    {
        _name = if (!laterGamepadHold) Name.P1_OPENING_TURRET_GATE
               else Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD
    }
    fun startP2(useColorSensors: Boolean)
    {
        _name =  if (isHoldP1())
                 Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD
            else if (useColorSensors)
                 Name.P2_SHOOT_UNTIL_EMPTY_USING_COLORS
            else Name.P2_SHOOT_BELTS_ON_TIME
    }
    fun startP3() { _name = Name.P3_OPENING_LAUNCHER }
    fun startCalibrateP4() { _name = Name.P4_CALIBRATE_REVERSING_BELTS }
    fun startCalibrateP5() { _name = Name.P5_CALIBRATE_CLOSING_ALL_SERVOS }
    fun startCalibrateP6() { _name = Name.P6_REALIGNING_FORWARDS }


    fun isActive()   = _name != Name.NOT_ACTIVE
    fun isInactive() = _name == Name.NOT_ACTIVE
    fun isNotShooting() = isInactive() || isCalibratingAfterShot()


    fun isWaitingP0() = _name == Name.P0_AWAITING_SORTING
    fun isHoldP1()    = _name == Name.P1_OPENING_TURRET_GATE_LATER_GAMEPAD_HOLD


    fun isHoldP2()    = _name == Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD
    fun isRegularP2() = _name == Name.P2_SHOOT_BELTS_ON_TIME
    fun isUntilColorsP2() = _name == Name.P2_SHOOT_UNTIL_EMPTY_USING_COLORS
    fun isNotOnTimeP2() = isHoldP2() || isUntilColorsP2()


    fun isCalibrationP4() = _name == Name.P4_CALIBRATE_REVERSING_BELTS
    fun isCalibrationP5() = _name == Name.P5_CALIBRATE_CLOSING_ALL_SERVOS
    fun isCalibratingAfterShot() = isCalibrationP4() || isCalibrationP5()
            || _name == Name.P6_REALIGNING_FORWARDS
    fun isSensitivePhase() = _name == Name.P3_OPENING_LAUNCHER || isCalibrationP5()
}