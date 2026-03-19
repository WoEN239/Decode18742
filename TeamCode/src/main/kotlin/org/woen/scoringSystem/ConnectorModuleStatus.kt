package org.woen.scoringSystem


import org.woen.collector.Collector
import org.woen.enumerators.MotorStatus
import org.woen.enumerators.ServoStatus
import org.woen.enumerators.SortingPhase
import org.woen.scoringSystem.misc.DynamicPattern



class ConnectorModuleStatus(var collector: Collector)
{
    var dynamicMemoryPattern = DynamicPattern()
    var awaitingPatternFromCamera = true

    var canTriggerIntake = false


    var lazyIntakeIsActive = false
    var shootingIsActive = false


    var beltsStatus = MotorStatus.IDLE

    var gateStatus = ServoStatus()
    var pushStatus = ServoStatus()
    var turretGateStatus = ServoStatus()


    var sortingPhase = SortingPhase()
}