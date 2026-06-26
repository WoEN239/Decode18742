package org.woen.tests


import com.acmerobotics.dashboard.FtcDashboard
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode

import org.woen.collector.RunMode
import org.woen.collector.Collector
import org.woen.enumerators.StorageSlot
import org.woen.scoringSystem.ConnectorModuleStatus
import org.woen.scoringSystem.storage.hardware.HwSensors



@TeleOp
class ColorSensorTest: LinearOpMode()
{
    override fun runOpMode()
    {
        val collector = Collector(this, RunMode.MANUAL)

        val cms = ConnectorModuleStatus(collector)
        val hwSensors = HwSensors(cms)

        cms.collector.telemetry.addLine("Detected:   " +
                "[${cms.colorResults.parsedResults[StorageSlot.BOTTOM].name}]  " +
                "[${cms.colorResults.parsedResults[StorageSlot.CENTER].name}]  " +
                "[${cms.colorResults.parsedResults[StorageSlot.TURRET].name}]")

        waitForStart()
        resetRuntime()

        collector.startEvent.invoke()

        while (opModeIsActive())
        {
            hwSensors.update()
            FtcDashboard.getInstance().telemetry.update()

            collector.updateEvent.invoke()
        }
    }
}