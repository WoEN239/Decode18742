package org.woen.scoringSystem


import org.woen.collector.Collector
import org.woen.scoringSystem.misc.DynamicPattern

import org.woen.enumerators.MotorStatus
import org.woen.enumerators.ServoStatus
import org.woen.enumerators.SortingPhase
import org.woen.enumerators.ShootingPhase
import org.woen.enumerators.CalibrationPhase



class ConnectorModuleStatus(var collector: Collector)
{
    var dynamicMemoryPattern = DynamicPattern()
    var awaitingPatternFromCamera = true

    var canTriggerIntake = false


    var lazyIntakeIsActive  = false
    var storageIsRealigning = false


    var sortingPhase     = SortingPhase()
    var shootingPhase    = ShootingPhase()
    var calibrationPhase = CalibrationPhase()



    var beltsStatus = MotorStatus.IDLE

    var gateStatus   = ServoStatus()
    var pushStatus   = ServoStatus()
    var launchStatus = ServoStatus()
    var turretGateStatus = ServoStatus()
}