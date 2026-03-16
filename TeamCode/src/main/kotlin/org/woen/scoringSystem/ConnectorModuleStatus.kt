package org.woen.scoringSystem


import org.woen.scoringSystem.misc.DynamicPattern
import java.util.concurrent.atomic.AtomicBoolean



class ConnectorModuleStatus
{
    val dynamicMemoryPattern = DynamicPattern()

    val canTriggerIntake = AtomicBoolean(false)

}