package org.woen.scoringSystem.storage.hardware


import com.qualcomm.hardware.rev.RevColorSensorV3
import kotlin.math.max
import kotlin.math.min

import org.woen.configs.Hardware
import org.woen.enumerators.Ball
import org.woen.enumerators.StorageSlot
import org.woen.scoringSystem.ConnectorModuleStatus



class HwSensors
{
    private var _cms: ConnectorModuleStatus

    private var _colorB: RevColorSensorV3
    private var _colorC: RevColorSensorV3
    private var _colorT: RevColorSensorV3



    constructor(cms: ConnectorModuleStatus) {
        _cms = cms

        _colorB = _cms.collector.hardwareMap.get(
            Hardware.DEVICE_NAMES.COLOR_SENSOR_BOTTOM)
                as RevColorSensorV3
        _colorC = _cms.collector.hardwareMap.get(
            Hardware.DEVICE_NAMES.COLOR_SENSOR_CENTER)
                as RevColorSensorV3
        _colorT = _cms.collector.hardwareMap.get(
            Hardware.DEVICE_NAMES.COLOR_SENSOR_TURRET)
                as RevColorSensorV3

        _cms.collector.startEvent += {
            _colorB.gain = 4.0f
            _colorC.gain = 4.0f
            _colorT.gain = 4.0f
        }

//        _cms.collector.telemetry.onTelemetrySend += {
//            it.addData("color sensor updates", String.format("%.1f", _updatesCounter.currentUPS))
//            it.addLine(
//                "left = ${String.format("%.1f", _leftR)} ${
//                    String.format(
//                        "%.1f",
//                        _leftG
//                    )
//                } ${String.format("%.1f", _leftB)}"
//            )
//            it.addLine(
//                "right = ${String.format("%.1f", _rightR)} ${
//                    String.format(
//                        "%.1f",
//                        _rightG
//                    )
//                } ${String.format("%.1f", _rightB)}"
//            )
//            it.addLine("hueRight ${String.format("%.1f", _rightH)}")
//            it.addLine("hueLeft ${String.format("%.1f", _leftH)}")
//        }
    }

    fun update()
    {
        if (_cms.colorResults.updateTargetsBCT.bottom)
        {
            val rgbB = _colorB.normalizedColors
            val rB = rgbB.red   * Hardware.SENSORS.MAXIMUM_READING
            val gB = rgbB.blue  * Hardware.SENSORS.MAXIMUM_READING
            val bB = rgbB.green * Hardware.SENSORS.MAXIMUM_READING

            val  isGreen = (gB - max(rB, bB)) > Hardware.SENSORS.GREEN_THRESHOLD_BOTTOM
            if (!isGreen) {
                val maxB = max(max(rB, gB), bB)
                val minB = min(min(rB, gB), bB)
                val difB = maxB - minB

                val hB = when (maxB) {
                    rB   -> (      (gB - bB) / difB)
                    gB   -> (2.0 + (bB - rB) / difB)
                    else -> (4.0 + (rB - gB) / difB)
                }

                val isPurple = hB in (Hardware.SENSORS.MIN_PURPLE_H_BOTTOM
                        ..Hardware.SENSORS.MAX_PURPLE_H_BOTTOM)
                if (!isPurple) {
                    val isNothing =
                        rB < Hardware.SENSORS.UNKNOWN_MIN_R_BOTTOM &&
                        gB < Hardware.SENSORS.UNKNOWN_MIN_G_BOTTOM &&
                        bB < Hardware.SENSORS.UNKNOWN_MIN_B_BOTTOM

                    if (isNothing)
                         _cms.colorResults.parsedResults[StorageSlot.BOTTOM] = Ball.Name.NONE
                    else _cms.colorResults.parsedResults[StorageSlot.BOTTOM] = Ball.Name.UNKNOWN_COLOR
                }   else _cms.colorResults.parsedResults[StorageSlot.BOTTOM] = Ball.Name.PURPLE
            }       else _cms.colorResults.parsedResults[StorageSlot.BOTTOM] = Ball.Name.GREEN
        }           else _cms.colorResults.parsedResults[StorageSlot.BOTTOM] = Ball.Name.NOT_UPDATED





        if (_cms.colorResults.updateTargetsBCT.center)
        {
            val rgbC = _colorC.normalizedColors
            val rC = rgbC.red   * Hardware.SENSORS.MAXIMUM_READING
            val gC = rgbC.blue  * Hardware.SENSORS.MAXIMUM_READING
            val bC = rgbC.green * Hardware.SENSORS.MAXIMUM_READING

            val  isGreen = (gC - max(rC, bC)) > Hardware.SENSORS.GREEN_THRESHOLD_CENTER
            if (!isGreen) {
                val maxC = max(max(rC, gC), bC)
                val minC = min(min(rC, gC), bC)
                val difC = maxC - minC

                val hC = when (maxC)
                {
                    rC   -> (      (gC - bC) / difC)
                    gC   -> (2.0 + (bC - rC) / difC)
                    else -> (4.0 + (rC - gC) / difC)
                }

                val  isPurple = hC in (Hardware.SENSORS.MIN_PURPLE_H_CENTER
                        ..Hardware.SENSORS.MAX_PURPLE_H_CENTER)
                if (!isPurple) {
                    val isNothing =
                        rC < Hardware.SENSORS.UNKNOWN_MIN_R_CENTER &&
                        gC < Hardware.SENSORS.UNKNOWN_MIN_G_CENTER &&
                        bC < Hardware.SENSORS.UNKNOWN_MIN_B_CENTER

                    if (isNothing)
                         _cms.colorResults.parsedResults[StorageSlot.CENTER] = Ball.Name.NONE
                    else _cms.colorResults.parsedResults[StorageSlot.CENTER] = Ball.Name.UNKNOWN_COLOR
                }   else _cms.colorResults.parsedResults[StorageSlot.CENTER] = Ball.Name.PURPLE
            }       else _cms.colorResults.parsedResults[StorageSlot.CENTER] = Ball.Name.GREEN
        }           else _cms.colorResults.parsedResults[StorageSlot.CENTER] = Ball.Name.NOT_UPDATED





        if (_cms.colorResults.updateTargetsBCT.center) {
            val rgbT = _colorC.normalizedColors
            val rT = rgbT.red   * Hardware.SENSORS.MAXIMUM_READING
            val gT = rgbT.blue  * Hardware.SENSORS.MAXIMUM_READING
            val bT = rgbT.green * Hardware.SENSORS.MAXIMUM_READING

            val  isGreen = (gT - max(rT, bT)) > Hardware.SENSORS.GREEN_THRESHOLD_TURRET
            if (!isGreen) {
                val maxT = max(max(rT, gT), bT)
                val minT = min(min(rT, gT), bT)
                val difT = maxT - minT

                val hT = when (maxT) {
                    rT   -> (      (gT - bT) / difT)
                    gT   -> (2.0 + (bT - rT) / difT)
                    else -> (4.0 + (rT - gT) / difT)
                }

                val  isPurple = hT in (Hardware.SENSORS.MIN_PURPLE_H_TURRET
                        ..Hardware.SENSORS.MAX_PURPLE_H_TURRET)
                if (!isPurple) {
                    val isNothing =
                        rT < Hardware.SENSORS.UNKNOWN_MIN_R_TURRET &&
                        gT < Hardware.SENSORS.UNKNOWN_MIN_G_TURRET &&
                        bT < Hardware.SENSORS.UNKNOWN_MIN_B_TURRET

                    if (isNothing)
                         _cms.colorResults.parsedResults[StorageSlot.TURRET] = Ball.Name.NONE
                    else _cms.colorResults.parsedResults[StorageSlot.TURRET] = Ball.Name.UNKNOWN_COLOR
                }   else _cms.colorResults.parsedResults[StorageSlot.TURRET] = Ball.Name.PURPLE
            }       else _cms.colorResults.parsedResults[StorageSlot.TURRET] = Ball.Name.GREEN
        }           else _cms.colorResults.parsedResults[StorageSlot.TURRET] = Ball.Name.NOT_UPDATED
    }
}