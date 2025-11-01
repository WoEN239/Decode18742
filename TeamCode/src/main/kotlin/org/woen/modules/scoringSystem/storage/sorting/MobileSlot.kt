package org.woen.modules.scoringSystem.storage.sorting


import woen239.enumerators.ServoGate

import org.woen.threading.hardware.HardwareThreads
import org.woen.modules.scoringSystem.storage.sorting.hardware.HwMobileSlot



class MobileSlot
{
    private val _sortingGateState = ServoGate()
    private val _sortingPushState = ServoGate()
    private val _sortingFallState = ServoGate()

    private lateinit var _hwMobileSlot: HwMobileSlot  //  DO NOT JOIN ASSIGNMENT



    fun openGate()
    {
        _hwMobileSlot.openGate()
    }
    fun closeGate()
    {
        _hwMobileSlot.closeGate()
    }
    fun openPush()
    {
        _hwMobileSlot.openPush()
    }
    fun closePush()
    {
        _hwMobileSlot.closePush()
    }
    fun openFall()
    {
        _hwMobileSlot.openFall()
    }
    fun closeFall()
    {
        _hwMobileSlot.closeFall()
    }



    fun linkHardware()
    {
        _hwMobileSlot = HwMobileSlot()
        HardwareThreads.LAZY_INSTANCE.EXPANSION.addDevices(_hwMobileSlot)
    }
    fun calibrateHardware()
    {
        _sortingGateState.Set(ServoGate.CLOSED, ServoGate.Name.CLOSED)
        _sortingPushState.Set(ServoGate.CLOSED, ServoGate.Name.CLOSED)
        _sortingFallState.Set(ServoGate.CLOSED, ServoGate.Name.CLOSED)

        _hwMobileSlot.fullCalibrate()
    }

    fun initHardware()
    {
        linkHardware()
        calibrateHardware()
    }



    init
    {
        _sortingGateState.Set(ServoGate.UNDEFINED, ServoGate.Name.UNDEFINED)
        _sortingPushState.Set(ServoGate.UNDEFINED, ServoGate.Name.UNDEFINED)
        _sortingFallState.Set(ServoGate.UNDEFINED, ServoGate.Name.UNDEFINED)
    }
}