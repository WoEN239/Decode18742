package org.woen.scoringSystem


import org.woen.collector.Collector
import org.woen.scoringSystem.misc.DynamicPattern



class ConnectorModuleStatus(var collector: Collector)
{
    var dynamicMemoryPattern = DynamicPattern()

    var canTriggerIntake = false
}