package org.woen.modules.scoringSystem.storage.sorting.hardware



class AutoIntakeTracker
{
    val intakeFoundBySensorId = arrayOf (SensorsId.NOTHING, SensorsId.NOTHING)

    fun intakeIsFree()
          = intakeFoundBySensorId[0] == SensorsId.NOTHING &&
            intakeFoundBySensorId[1] == SensorsId.NOTHING

    fun safeAddSensorId(id: SensorsId)
    {
        if (intakeFoundBySensorId[0] != id && intakeFoundBySensorId[1] != id)
        {
            if (intakeFoundBySensorId[0] == SensorsId.NOTHING)
                intakeFoundBySensorId[0] = id
            else if (intakeFoundBySensorId[1] == SensorsId.NOTHING)
                     intakeFoundBySensorId[1] = id
        }
    }

    fun safeRemoveSensorId(id: SensorsId)
    {
        if (intakeFoundBySensorId[0] == id)
            intakeFoundBySensorId[0] = SensorsId.NOTHING
        if (intakeFoundBySensorId[1] == id)
            intakeFoundBySensorId[1] = SensorsId.NOTHING
    }
}