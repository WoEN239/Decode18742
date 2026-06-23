package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.collector.RunMode
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.utils.debug.Debug

import org.woen.configs.DelayMS
import org.woen.configs.Hardware
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.enumerators.phases.ShootingPhase



class HwSortingManager
{
    private val _hwSensors  : HwSensors
    val hwMotors  : HwMotors
    private val _cms: ConnectorModuleStatus

    var targetPushTime: Long = 0
    var targetBrushTime: Long = 0
    var lastUpdateTimestampMS: Double = 0.0
    val rotatingBeltsTimer = ElapsedTime()
    val rotatingBrushTimer = ElapsedTime()



    constructor(cms: ConnectorModuleStatus)
    {
        _cms = cms
        hwMotors   = HwMotors(cms)
        _hwSensors = HwSensors(cms)
    }



    fun update()
    {
        if ((_cms.beltsStatus.isOnTime()) &&
            rotatingBeltsTimer.milliseconds() > targetPushTime)
        {
            hwMotors.logM.logMd("Stopping belts on time", Debug.LOGIC)

            if (_cms.shootingPhase.isCalibrationP4()) calibrationP5()
            else hwMotors.stopBelts()
        }

        if (_cms.brushStatus.isOnTime() &&
            rotatingBrushTimer.milliseconds() > targetBrushTime)
        {
            hwMotors.logM.logMd("Stopping brush on time", Debug.LOGIC)
            hwMotors.stopBrush()
        }

        hwMotors.updateServos()
    }
    fun tryFastUpdateColors() { if (canUpdateColors()) _hwSensors.update() }
    fun canUpdateColors()
        = (_cms.collector.runMode == RunMode.AUTO   && !AUTONOMOUS.IGNORE_COLOR_SENSORS) ||
          (_cms.collector.runMode == RunMode.MANUAL && !TELEOP.IGNORE_COLOR_SENSORS)


    fun isReadyForShootingPhase3()
        =   _cms.beltsStatus.isIdle() || (
            _cms.beltsStatus.isForwardOnTime() &&
            _cms.launchStatus.isClosingOrClosed() &&
            rotatingBeltsTimer.milliseconds() >
            targetPushTime - (if (_cms.shootingPhase.shotBeltsVoltage
                == Hardware.MOTOR.BELTS_FOR_FAST_SHOOTING)
                     DelayMS.SHOOTING.FAST_LAST_WITH_LAUNCHER
                else DelayMS.SHOOTING.SLOW_LAST_WITH_LAUNCHER))
    fun isReadyForShootingPhase4()
        = when (_cms.shootingPhase.name)
        {
            ShootingPhase.Name.P2_SHOOT_BELTS_ON_TIME,
            ShootingPhase.Name.P2_SHOOT_BELTS_ON_GAMEPAD_HOLD
                 -> _cms.beltsStatus.isIdle()
            ShootingPhase.Name.P2_SHOOT_UNTIL_EMPTY_USING_COLORS
                 -> _cms.colorResults.isEmptyBySensors()
            ShootingPhase.Name.P3_OPENING_LAUNCHER
                 -> _cms.launchStatus.isOpened()
            else -> false
        }



    fun calibrationP4()
    {
        extendableReverse(DelayMS.PUSH.HALF)

        hwMotors.logM.logMd("Calibration P4, reversing belts", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.shootingPhase.startCalibrateP4()
    }
    fun calibrationP5()
    {
        hwMotors.reverseBelts(onTime = false)
        hwMotors.closeLaunch()
        hwMotors.closeTurretGate()
//        hwMotors.closeGateWithPush()

        hwMotors.logM.logMd("Calibration P5, closing servos", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.shootingPhase.startCalibrateP5()
    }
    fun calibrationP6()
    {
        extendableForward(DelayMS.PUSH.HALF)

        hwMotors.logM.logMd("Calibration P3, forward realignment", Debug.LOGIC)
        _cms.colorResults.reactivateColorTargetsForIntake()
        _cms.canTriggerIntake = canUpdateColors()
        _cms.shootingPhase.startCalibrateP6()
    }
    fun closedShootingServos()
        =  _cms.launchStatus.isClosed()
        && _cms.turretGateStatus.isClosed()





    fun extendableForward    (timeMs: Long, voltage: Double = 12.0)
        = extendable    (true, timeMs, voltage)
    fun reinstantiableForward(timeMs: Long, voltage: Double = 12.0)
        = reinstantiable(true, timeMs, voltage)

    fun extendableReverse    (timeMs: Long, voltage: Double = 12.0)
        = extendable    (false, timeMs, voltage)
    fun reinstantiableReverse(timeMs: Long, voltage: Double = 12.0)
        = reinstantiable(false, timeMs, voltage)


    private fun extendable    (forward: Boolean, timeMs: Long, voltage: Double = 12.0)
            = startBeltsTime(forward,
                if (!((forward && _cms.beltsStatus.isForwardOnTime())
                           || (!forward && _cms.beltsStatus.isReverseOnTime()))) timeMs
                else timeMs + targetPushTime
                    - rotatingBeltsTimer.milliseconds().toLong(), voltage)
    private fun reinstantiable(forward: Boolean, timeMs: Long, voltage: Double = 12.0)
            = startBeltsTime(forward,
                if (!((forward && _cms.beltsStatus.isForwardOnTime())
                           || (!forward && _cms.beltsStatus.isReverseOnTime()))) timeMs
                else max(timeMs, targetPushTime
                    - rotatingBeltsTimer.milliseconds().toLong()), voltage)

    fun startBeltsTime(forward: Boolean, timeMs: Long, voltage: Double = 12.0)
    {
        if (forward) hwMotors.forwardBelts(onTime = true, voltage)
        else hwMotors.reverseBelts(onTime = true, voltage)
        hwMotors.logM.logMd("Rotate belts period: $timeMs", Debug.HW)
        rotatingBeltsTimer.reset()
        targetPushTime = timeMs
    }
    fun startBrushTime(forward: Boolean, timeMs: Long)
    {
        if (forward) hwMotors.forwardBrush(onTime = true)
        else hwMotors.reverseBrush(onTime = true)
        hwMotors.logM.logMd("Rotate brush period: $timeMs", Debug.HW)
        rotatingBrushTimer.reset()
        targetBrushTime = timeMs
    }
}