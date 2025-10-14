package org.woen.modules.storage

import barrel.enumerators.Ball
import barrel.enumerators.RunStatus

class Storage
{
    private var _runStatus = RunStatus();
    private var _storageCells: StorageCells = StorageCells();
    //!  private HardwareStorage hwStorage





    fun Start()
    {
        if (_runStatus.Name() != RunStatus.Name.PAUSE)
            _runStatus.Set(RunStatus.Name.ACTIVE, RunStatus.ACTIVE);
    }
    init
    {
        TODO("Add hardware initialisation logic")

        //_hwStorage = HardwareStorage(deviceName, direction);
        //_hwStorage.init(hwMap);
        //HardwareThreads.getLAZY_INSTANCE().getEXPANSION().addDevices(_hwStorage);
    }
}