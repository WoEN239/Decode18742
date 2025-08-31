package org.woen.hardware

import com.qualcomm.robotcore.hardware.DcMotorController
import com.qualcomm.robotcore.hardware.DcMotorImplEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType

class OptimizedDcMotor(
    controller: DcMotorController,
    portNumber: Int,
    direction: DcMotorSimple.Direction,
    motorType: MotorConfigurationType
) : DcMotorImplEx(controller, portNumber, direction, motorType) {

}