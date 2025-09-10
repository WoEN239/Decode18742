package org.woen.modules.driveTrain

import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.woen.modules.IModule
import org.woen.threading.ThreadManager
import org.woen.threading.hardware.HardwareThreads

class Odometry: IModule {
    private val _hardwareOdometry = HardwareOdometry("", "")
    private val _threeOdometry = HardwareThreeOdometry("")

    init {
        HardwareThreads.LAZY_INSTANCE.CONTROL.addDevices(_hardwareOdometry)
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_threeOdometry)
    }

    private var _odometryJob: Job? = null

    override fun process() {
        _odometryJob = ThreadManager.LAZY_INSTANCE.globalCoroutineScope.launch {

        }
    }

    override val isBusy: Boolean
        get() = _odometryJob == null || _odometryJob!!.isCompleted

    override fun dispose() {
        _odometryJob?.cancel()
    }
}