package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.collector.RunMode
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS

import org.woen.utils.debug.Debug
import org.woen.enumerators.Ball
import org.woen.enumerators.MotorStatus



class HwSortingManager
{
    private val _hwSensors  : HwSensors
    val hwMotors  : HwMotors
    private val _cms: ConnectorModuleStatus

    var targetPushTime: Long = 0
    val rotatingBeltsTimer = ElapsedTime()



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms
        hwMotors   = HwMotors(cms)
        _hwSensors = HwSensors(cms)
    }



    fun update()
    {
        hwMotors.updateServos()

        if ((_cms.beltsStatus == MotorStatus.FORWARD ||
             _cms.beltsStatus == MotorStatus.REVERSE) &&
            rotatingBeltsTimer.milliseconds() > targetPushTime)
            hwMotors.stopBelts()
    }

    fun updateColors(): Ball.Name
        = if (_cms.canTriggerIntake && (
                _cms.collector.runMode == RunMode.AUTO
                && !AUTONOMOUS.IGNORE_COLOR_SENSORS
            ) || (
                _cms.collector.runMode == RunMode.MANUAL
                && !TELEOP.IGNORE_COLOR_SENSORS))
            _hwSensors.update()
        else Ball.Name.NONE



    fun startCalibration()
    {
        hwMotors.closeTurretGate()
        hwMotors.closeGateWithPush()
        hwMotors.stopBelts()
    }
    val isCalibrationFinished get()
        =   _cms.beltsStatus == MotorStatus.IDLE &&
            _cms.gateStatus.isFinished() &&
            _cms.pushStatus.isFinished() &&
            _cms.turretGateStatus.isFinished()



    fun extendableForward    (timeMs: Long) = extendable    (true, timeMs)
    fun reinstantiableForward(timeMs: Long) = reinstantiable(true, timeMs)

    fun extendableReverse    (timeMs: Long) = extendable    (false, timeMs)
    fun reinstantiableReverse(timeMs: Long) = reinstantiable(false, timeMs)


    private fun extendable    (forward: Boolean, timeMs: Long)
            = startBeltsTime(forward, if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
    else timeMs + targetPushTime - rotatingBeltsTimer.milliseconds().toLong())
    private fun reinstantiable(forward: Boolean, timeMs: Long)
        = startBeltsTime(forward, if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
                else max(timeMs, targetPushTime
                    - rotatingBeltsTimer.milliseconds().toLong()))
    private fun startBeltsTime(forward: Boolean, timeMs: Long)
    {
        hwMotors.logM.logMd("Rotate belts period: $timeMs", Debug.HW)
        targetPushTime = timeMs
        rotatingBeltsTimer.reset()
        if (forward) hwMotors.forwardBelts()
        else hwMotors.reverseBelts()
    }


//    fun rotateMobileSlot()
//    {
//        hwMotors.logM.logMd("ROTATING MOBILE SLOT", Debug.HW_HIGH)
//
//        hwMotors.closeTurretGate()
//
//        slowStartBelts()
//        delay(Delay.MS.REALIGNMENT.SORTING_FORWARD)
//        reverseBeltsTime(Delay.MS.REALIGNMENT.SORTING_REVERSE)
//
//        hwMotors.openGate()
//        hwMotors.openPush()
//
//        forwardBeltsTime(Delay.MS.HW_REQUEST_FREQUENCY * 8)
//
//        closeGateWithPush
//        forwardBeltsTime(Delay.MS.REALIGNMENT.SORTING_FORWARD)
//    }
}