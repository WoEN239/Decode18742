package org.woen.utils.motor

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class MotorOnly(private val _motor: DcMotorEx): DcMotorEx {
    init {
        _motor.direction = Direction.FORWARD
        _motor.mode = DcMotor.RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = DcMotor.RunMode.RUN_WITHOUT_ENCODER
    }

    private var _direction = Direction.FORWARD

    override fun getManufacturer() = _motor.manufacturer

    override fun getDeviceName() = _motor.deviceName

    override fun getConnectionInfo() = _motor.connectionInfo

    override fun getVersion() = _motor.version

    override fun resetDeviceConfigurationForOpMode() {
        _motor.resetDeviceConfigurationForOpMode()
    }

    override fun close() {
        _motor.close()
    }

    override fun setDirection(direction: Direction?){
        _direction = direction!!
    }

    override fun getDirection() = _direction

    override fun setPower(power: Double) {
        if(_direction == Direction.FORWARD) {
            _motor.power = power

            return
        }

        _motor.power = -power
    }

    override fun getPower(): Double {
        if(_direction == Direction.FORWARD)
            return _motor.power

        return -_motor.power
    }

    override fun getMotorType() = _motor.motorType

    override fun setMotorType(motorType: MotorConfigurationType?) {
        _motor.motorType = motorType
    }

    override fun getController() = _motor.controller

    override fun getPortNumber() = _motor.portNumber

    override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior?) {
        _motor.zeroPowerBehavior = zeroPowerBehavior
    }

    override fun getZeroPowerBehavior() = _motor.zeroPowerBehavior

    @Deprecated("Deprecated in Java")
    override fun setPowerFloat() {
        _motor.setPowerFloat()
    }

    override fun getPowerFloat() = _motor.powerFloat

    override fun setTargetPosition(position: Int) {
        throw Exception("motorOnly not support targetPosition")
    }

    override fun getTargetPosition(): Int {
        throw Exception("motorOnly not support targetPosition")
    }

    override fun isBusy() = _motor.isBusy

    override fun getCurrentPosition(): Int {
        throw Exception("motorOnly not support currentPosition")
    }

    override fun setMode(mode: DcMotor.RunMode?) {
        throw Exception("motorOnly not support mode")
    }

    override fun getMode(): DcMotor.RunMode{
        throw Exception("motorOnly not support mode")
    }

    override fun setMotorEnable() {
        throw Exception("motorOnly not support motorEnable")
    }

    override fun setMotorDisable() {
        throw Exception("motorOnly not support motorEnable")
    }

    override fun isMotorEnabled(): Boolean {
        throw Exception("motorOnly not support motorEnable")
    }

    override fun setVelocity(angularRate: Double) {
        throw Exception("motorOnly not support velocity")
    }

    override fun setVelocity(angularRate: Double, unit: AngleUnit?) {
        throw Exception("motorOnly not support velocity")
    }

    override fun getVelocity(): Double {
        throw Exception("motorOnly not support velocity")
    }

    override fun getVelocity(unit: AngleUnit?): Double {
        throw Exception("motorOnly not support velocity")
    }

    override fun setPIDCoefficients(mode: DcMotor.RunMode?, pidCoefficients: PIDCoefficients?) {
        throw Exception("motorOnly not support pid")
    }

    override fun setPIDFCoefficients(mode: DcMotor.RunMode?, pidfCoefficients: PIDFCoefficients?) {
        throw Exception("motorOnly not support pid")
    }

    override fun setVelocityPIDFCoefficients(p: Double, i: Double, d: Double, f: Double) {
        throw Exception("motorOnly not support pid")
    }

    override fun setPositionPIDFCoefficients(p: Double) {
        throw Exception("motorOnly not support pid")
    }

    override fun getPIDCoefficients(mode: DcMotor.RunMode?): PIDCoefficients {
        throw Exception("motorOnly not support pid")
    }

    override fun getPIDFCoefficients(mode: DcMotor.RunMode?): PIDFCoefficients {
        throw Exception("motorOnly not support pid")
    }

    override fun setTargetPositionTolerance(tolerance: Int) {
        throw Exception("motorOnly not support targetPositionTolerance")
    }

    override fun getTargetPositionTolerance(): Int {
        throw Exception("motorOnly not support targetPositionTolerance")
    }

    override fun getCurrent(unit: CurrentUnit?) = _motor.getCurrent(unit)

    override fun getCurrentAlert(unit: CurrentUnit?) = _motor.getCurrentAlert(unit)

    override fun setCurrentAlert(current: Double, unit: CurrentUnit?) {
        _motor.setCurrentAlert(current, unit)
    }

    override fun isOverCurrent() = _motor.isOverCurrent
}