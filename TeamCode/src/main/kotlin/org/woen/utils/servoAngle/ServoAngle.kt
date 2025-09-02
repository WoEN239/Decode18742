package org.woen.utils.servoAngle

import com.qualcomm.robotcore.hardware.Servo


class ServoAngle(private val _servo: Servo, val maxAngle: Double) : Servo {
    override fun getManufacturer() = _servo.manufacturer

    override fun getDeviceName() = _servo.deviceName

    override fun getConnectionInfo() = _servo.connectionInfo

    override fun getVersion() = _servo.version

    override fun resetDeviceConfigurationForOpMode() = _servo.resetDeviceConfigurationForOpMode()

    override fun close() = _servo.close()

    override fun getController() = _servo.controller

    override fun getPortNumber() = _servo.portNumber

    override fun setDirection(direction: Servo.Direction?) {
        _servo.direction = direction
    }

    override fun getDirection() = _servo.direction

    override fun setPosition(position: Double) {
        _servo.position = position
    }

    override fun getPosition() = _servo.position

    var angle
        set(value){
            _servo.position = value / maxAngle
        }
        get() = _servo.position * maxAngle

    override fun scaleRange(min: Double, max: Double) = _servo.scaleRange(min, max)
}