package org.woen.modules.scoringSystem.barrel

import org.woen.utils.process.Process


data class RequestResultEvent(
    var isSuccessful: Boolean = false,
    val process: Process = Process()
)
data class ShotWasFiredEvent(
    var isSuccessful: Boolean = false,
    val process: Process = Process()
)

class BarrelEvents
{

}