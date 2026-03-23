package org.woen.scoringSystem


import org.woen.collector.Collector
import org.woen.scoringSystem.misc.DynamicPattern

import org.woen.enumerators.status.MotorStatus
import org.woen.enumerators.status.ServoStatus
import org.woen.enumerators.phases.SortingPhase
import org.woen.enumerators.phases.ShootingPhase
import org.woen.enumerators.phases.CalibrationPhase



class ConnectorModuleStatus(var collector: Collector)
{
    var dynamicMemoryPattern = DynamicPattern()
    var awaitingPatternFromCamera = true

    var canTriggerIntake = true


    var lazyIntakeIsActive  = false


    var sortingPhase     = SortingPhase()
    var shootingPhase    = ShootingPhase()
    var calibrationPhase = CalibrationPhase()



    var beltsStatus = MotorStatus()
    var brushStatus = MotorStatus()

    var gateStatus   = ServoStatus()
    var pushStatus   = ServoStatus()
    var launchStatus = ServoStatus()
    var turretGateStatus = ServoStatus()
}