package org.woen.scoringSystem


import org.woen.collector.Collector
import org.woen.scoringSystem.misc.DynamicPattern
import org.woen.scoringSystem.storage.hardware.MotorStatus
import org.woen.scoringSystem.storage.hardware.ServoStatus


class ConnectorModuleStatus(var collector: Collector)
{
    var dynamicMemoryPattern = DynamicPattern()

    var canTriggerIntake = false



    var prevBeltsStatus = MotorStatus.IDLE
    var beltsStatus = MotorStatus.IDLE
    var brushStatus = MotorStatus.IDLE

    var gateStatus  = ServoStatus.CLOSING
    var pushStatus  = ServoStatus.CLOSING
    var turretGate  = ServoStatus.CLOSING
}