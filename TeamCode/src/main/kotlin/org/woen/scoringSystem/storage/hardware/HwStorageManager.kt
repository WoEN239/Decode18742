package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.collector.RunMode
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.enumerators.Ball
import org.woen.utils.debug.Debug

import org.woen.configs.Delay
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS



class HwSortingManager
{
    private val _hwSensors  : HwSensors
    val hwMotors  : HwMotors
    private val _cms: ConnectorModuleStatus

    var targetPushTime: Long = 0
    var targetBrushTime: Long = 0
    var timeSinceLastShotUpdateMs: Double = 0.0
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
        hwMotors.updateServos()

        if ((_cms.beltsStatus.isOnTime()) &&
            rotatingBeltsTimer.milliseconds() > targetPushTime)
        {
            hwMotors.logM.logMd("Stopping belts on time", Debug.LOGIC)
            hwMotors.stopBelts()

            if (_cms.calibrationPhase.isCalibrationPhase1())
                calibrationPhase2()
        }

        if (_cms.brushStatus.isOnTime() &&
            rotatingBrushTimer.milliseconds() > targetBrushTime)
        {
            hwMotors.logM.logMd("Stopping brush on time", Debug.LOGIC)
            hwMotors.stopBrush()
        }
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



    fun wasShotFired()
            =   rotatingBeltsTimer.milliseconds() - timeSinceLastShotUpdateMs >
            Delay.MS.SHOOTING.CONSIDER_SHOT_FIRED
    fun isReadyForShootingPhase3()
        =   _cms.beltsStatus.isForwardOnTime() &&
            _cms.launchStatus.isClosingOrClosed() &&
            rotatingBeltsTimer.milliseconds() >
            targetPushTime - Delay.MS.SHOOTING.FIRE_LAST_WITH_LAUNCHER
    fun streamDrumPhase3()
    {
        hwMotors.logM.logMd("StreamDrum phase 3, opening launch", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.shootingPhase.startPhase3()

        hwMotors.reverseBrush(onTime = false)
        hwMotors.openLaunch()
        hwMotors.forwardBelts(onTime = false)
    }
    fun isReadyForShootingPhase4()
        =   (_cms.shootingPhase.isAnyShootingPhase2() &&
             _cms.beltsStatus.isIdle()
            ) || (
            _cms.shootingPhase.isShootingPhase3() &&
            _cms.launchStatus.isOpened())



    fun calibrationPhase1()
    {
        hwMotors.logM.logMd("Calibration phase 1, reversing belts", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.calibrationPhase.startPhase1()
        extendableReverse(Delay.MS.PUSH.HALF)
    }
    fun calibrationPhase2()
    {
        hwMotors.logM.logMd("Calibration phase 2, closing servos", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.calibrationPhase.startPhase2()

        hwMotors.reverseBelts(onTime = false)
        hwMotors.closeLaunch()
        hwMotors.closeTurretGate()
        hwMotors.closeGateWithPush()
    }
    fun calibrationPhase3()
    {
        hwMotors.logM.logMd("Calibration phase 3, forward realignment", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.calibrationPhase.startPhase3()
        extendableForward(Delay.MS.PUSH.FULL)
    }

    fun closedAllServos()
        =   _cms.gateStatus.isClosed() &&
            _cms.pushStatus.isClosed() &&
            _cms.launchStatus.isClosed() &&
            _cms.turretGateStatus.isClosed()
    fun isHardwareIdle()
        =   _cms.beltsStatus.notOnTime() &&
            _cms.gateStatus.isFinished() &&
            _cms.pushStatus.isFinished() &&
            _cms.launchStatus.isFinished() &&
            _cms.turretGateStatus.isFinished()





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
        hwMotors.logM.logMd("Rotate belts period: $timeMs", Debug.HW)
        targetPushTime = timeMs
        rotatingBeltsTimer.reset()
        if (forward) hwMotors.forwardBelts(onTime = true, voltage)
        else hwMotors.reverseBelts(onTime = true, voltage)
    }

    fun startBrushTime(forward: Boolean, timeMs: Long)
    {
        hwMotors.logM.logMd("Rotate brush period: $timeMs", Debug.HW)
        targetBrushTime = timeMs
        rotatingBrushTimer.reset()
        if (forward) hwMotors.forwardBrush(onTime = true)
        else hwMotors.reverseBrush(onTime = true)
    }
}