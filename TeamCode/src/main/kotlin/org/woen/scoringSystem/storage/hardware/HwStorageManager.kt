package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime

import org.woen.collector.RunMode
import org.woen.configs.Delay
import org.woen.scoringSystem.ConnectorModuleStatus

import org.woen.configs.RobotSettings.CONTROLS
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS

import org.woen.utils.debug.Debug
import org.woen.enumerators.Ball
import org.woen.enumerators.phases.MotorStatus



class HwSortingManager
{
    private val _hwSensors  : HwSensors
    val hwMotors  : HwMotors
    private val _cms: ConnectorModuleStatus

    var targetPushTime: Long = 0
    var timeSinceLastShotUpdateMs: Double = 0.0
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
        {
            hwMotors.stopBelts()

            if (_cms.calibrationPhase.isCalibrationPhase1())
                calibrationPhase2()
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
        =   _cms.beltsStatus == MotorStatus.FORWARD &&
            CONTROLS.USE_LAUNCHER_FOR_LAST_BALL &&
            _cms.launchStatus.isClosingOrClosed() &&
            rotatingBeltsTimer.milliseconds() >
            targetPushTime - Delay.MS.SHOOTING.FIRE_LAST_WITH_LAUNCHER
    fun streamDrumPhase3()
    {
        hwMotors.logM.logMd("StreamDrum phase 3, opening launch", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.shootingPhase.switchToNextPhase()

        hwMotors.reverseBrush()
        hwMotors.openLaunch()
    }
    fun isReadyForShootingPhase4()
        =  (_cms.shootingPhase.isShootingPhase2() ||
            _cms.shootingPhase.isShootingPhase3() &&
            _cms.launchStatus.isFinished()) &&
            _cms.beltsStatus == MotorStatus.IDLE



    fun calibrationPhase1()
    {
        hwMotors.logM.logMd("Calibration phase 1, reversing belts", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.calibrationPhase.startPhase1()
        extendableReverse(Delay.MS.PUSH.HALF)
    }
    fun calibrationPhase2()
    {
        hwMotors.logM.logMd("Calibration phase 1, closing servos", Debug.LOGIC)
        _cms.canTriggerIntake = false
        _cms.calibrationPhase.startPhase2()

        hwMotors.stopBelts()
        hwMotors.closeLaunch()
        hwMotors.closeTurretGate()
        hwMotors.closeGateWithPush()
    }
    fun calibrationPhase3()
    {
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
        =   _cms.beltsStatus == MotorStatus.IDLE &&
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
                if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
                else timeMs + targetPushTime
                    - rotatingBeltsTimer.milliseconds().toLong(), voltage)
    private fun reinstantiable(forward: Boolean, timeMs: Long, voltage: Double = 12.0)
        = startBeltsTime(forward,
                if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
                else max(timeMs, targetPushTime
                    - rotatingBeltsTimer.milliseconds().toLong()), voltage)
    private fun startBeltsTime(forward: Boolean, timeMs: Long, voltage: Double = 12.0)
    {
        hwMotors.logM.logMd("Rotate belts period: $timeMs", Debug.HW)
        targetPushTime = timeMs
        rotatingBeltsTimer.reset()
        if (forward) hwMotors.forwardBelts(voltage)
        else hwMotors.reverseBelts(voltage)
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