package org.woen.modules.barrel

import org.woen.modules.turret.Pattern
import org.woen.utils.process.Process


data class RequestResultEvent(
    var isSuccessful: Boolean = false,
    val process: org.woen.utils.process.Process = Process()
)
data class ShotWasFiredEvent(
    var isSuccessful: Boolean = false,
    val process: org.woen.utils.process.Process = Process()
)

class BarrelEvents
{

}