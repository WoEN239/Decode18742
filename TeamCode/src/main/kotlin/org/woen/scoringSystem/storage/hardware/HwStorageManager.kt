package org.woen.scoringSystem.storage.hardware


import kotlin.math.max
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.collector.RunMode

import org.woen.utils.debug.Debug
import org.woen.configs.DebugSettings
import org.woen.configs.RobotSettings.TELEOP
import org.woen.configs.RobotSettings.AUTONOMOUS
import org.woen.enumerators.Ball
import org.woen.utils.debug.LogManager

import org.woen.scoringSystem.ConnectorModuleStatus



enum class MotorStatus
{
    IDLE,
    FORWARD,
    REVERSE,
    LAZY_FORWARD,
    LAZY_REVERSE
}
enum class ServoStatus
{
    CLOSED,
    CLOSING,

    OPENED,
    OPENING,
}



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
        _hwSensors = HwSensors(cms)
        hwMotors = HwMotors(cms)
    }



    fun update()
    {
        if ((_cms.beltsStatus == MotorStatus.FORWARD ||
             _cms.beltsStatus == MotorStatus.REVERSE) &&
            rotatingBeltsTimer.milliseconds() > targetPushTime)
            hwMotors.stopBelts()

//        if (_cms.gateStatus ==)
    }

    fun updateColors(): Ball.Name
    {
        return if  (_cms.canTriggerIntake && (
                        (_cms.collector.runMode == RunMode.AUTO
                        && !AUTONOMOUS.IGNORE_COLOR_SENSORS)
                    ) || (
                        _cms.collector.runMode == RunMode.MANUAL
                        && !TELEOP.IGNORE_COLOR_SENSORS))
            _hwSensors.update()
        else Ball.Name.NONE
    }



    fun fullCalibrate()
    {
        hwMotors.closeTurretGate()
        hwMotors.closeGateWithPush()
    }


    fun reinstantiableForwardBeltsTime(timeMs: Long)
    {
        val pushTime: Long =
            if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
            else max(timeMs,
                    targetPushTime
                        - rotatingBeltsTimer
                            .milliseconds().toLong())

        hwMotors.logM.logMd("Rotate belts period: $pushTime", Debug.HW)
        targetPushTime = pushTime
        rotatingBeltsTimer.reset()

        hwMotors.startBelts()
    }
    fun extendableForwardBeltsTime(timeMs: Long)
    {
        val pushTime: Long =
            if (_cms.beltsStatus != MotorStatus.FORWARD) timeMs
            else timeMs + targetPushTime
                 - rotatingBeltsTimer
                    .milliseconds().toLong()

        hwMotors.logM.logMd("Rotate belts period: $pushTime", Debug.HW)
        targetPushTime = pushTime
        rotatingBeltsTimer.reset()
        hwMotors.startBelts()
    }


    fun rotateMobileSlot()
    {
        hwMotors.logM.logMd("ROTATING MOBILE SLOT", Debug.HW_HIGH)

        hwMotors.closeTurretGate()

//        slowStartBelts()
//        delay(Delay.MS.REALIGNMENT.SORTING_FORWARD)
//        reverseBeltsTime(Delay.MS.REALIGNMENT.SORTING_REVERSE)

        hwMotors.openGate()
        hwMotors.openPush()

//        forwardBeltsTime(Delay.MS.HW_REQUEST_FREQUENCY * 8)

//        closeGateWithPush
//        forwardBeltsTime(Delay.MS.REALIGNMENT.SORTING_FORWARD)
    }
}