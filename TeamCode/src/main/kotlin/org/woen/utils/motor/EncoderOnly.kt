package org.woen.utils.motor

import com.qualcomm.robotcore.hardware.DcMotor
import com.qualcomm.robotcore.hardware.DcMotor.RunMode
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple.Direction
import com.qualcomm.robotcore.hardware.PIDCoefficients
import com.qualcomm.robotcore.hardware.PIDFCoefficients
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.CurrentUnit

class EncoderOnly : DcMotorEx {
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

    override fun setDirection(direction: Direction?) {
        _direction = direction!!
    }

    fun resetEncoder() {
        _motor.mode = RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = RunMode.RUN_WITHOUT_ENCODER
    }

    override fun getDirection() = _direction

    override fun setPower(power: Double) {
        throw Exception("encoder only not support setPower")
    }

    override fun getPower(): Double {
        throw Exception("encoder only not support setPower")
    }

    override fun getMotorType() = _motor.motorType

    override fun setMotorType(motorType: MotorConfigurationType?) {
        _motor.motorType = motorType
    }

    override fun getController() = _motor.controller

    override fun getPortNumber() = _motor.portNumber

    override fun setZeroPowerBehavior(zeroPowerBehavior: DcMotor.ZeroPowerBehavior?) {
        throw Exception("encoderOnly not support zeroPowerBehavior")
    }

    override fun getZeroPowerBehavior(): DcMotor.ZeroPowerBehavior {
        throw Exception("encoderOnly not support zeroPowerBehavior")
    }

    override fun setPowerFloat() {
        throw Exception("encoderOnly not support powerFloat")
    }

    override fun getPowerFloat(): Boolean {
        throw Exception("encoderOnly not support powerFloat")
    }

    override fun setTargetPosition(position: Int) {
        throw Exception("encoderOnly not support targetPosition")
    }

    override fun getTargetPosition(): Int {
        throw Exception("encoderOnly not support targetPosition")
    }

    override fun isBusy() = _motor.isBusy

    override fun getCurrentPosition(): Int {
        if (_direction == Direction.FORWARD)
            return _motor.currentPosition

        return -_motor.currentPosition
    }

    override fun setMode(mode: RunMode?) {
        throw Exception("encoderOnly not support mode")
    }

    override fun getMode(): RunMode {
        throw Exception("encoderOnly not support mode")
    }

    override fun setMotorEnable() {
        throw Exception("encoderOnly not support motorEnable")
    }

    override fun setMotorDisable() {
        throw Exception("encoderOnly not support motorEnable")
    }

    override fun isMotorEnabled(): Boolean {
        throw Exception("encoderOnly not support motorEnable")
    }

    override fun setVelocity(angularRate: Double) {
        throw Exception("encoderOnly not support velocity")
    }

    override fun setVelocity(angularRate: Double, unit: AngleUnit?) {
        throw Exception("encoderOnly not support velocity")
    }

    override fun getVelocity(): Double {
        if (_direction == Direction.FORWARD)
            return _motor.velocity

        return -_motor.velocity
    }

    override fun getVelocity(unit: AngleUnit?): Double {
        if (_direction == Direction.FORWARD)
            return _motor.getVelocity(unit)

        return -_motor.getVelocity(unit)
    }

    override fun setPIDCoefficients(mode: RunMode?, pidCoefficients: PIDCoefficients?) {
        throw Exception("encoderOnly not support pid")
    }

    override fun setPIDFCoefficients(mode: RunMode?, pidfCoefficients: PIDFCoefficients?) {
        throw Exception("encoderOnly not support pid")
    }

    override fun setVelocityPIDFCoefficients(p: Double, i: Double, d: Double, f: Double) {
        throw Exception("encoderOnly not support pid")
    }

    override fun setPositionPIDFCoefficients(p: Double) {
        throw Exception("encoderOnly not support pid")
    }

    override fun getPIDCoefficients(mode: RunMode?): PIDCoefficients {
        throw Exception("encoderOnly not support pid")
    }

    override fun getPIDFCoefficients(mode: RunMode?): PIDFCoefficients {
        throw Exception("encoderOnly not support pid")
    }

    override fun setTargetPositionTolerance(tolerance: Int) {
        throw Exception("encoderOnly not support targetPositionTolerance")
    }

    override fun getTargetPositionTolerance(): Int {
        throw Exception("encoderOnly not support targetPositionTolerance")
    }

    override fun getCurrent(unit: CurrentUnit?): Double {
        throw Exception("encoderOnly not support current")
    }

    override fun getCurrentAlert(unit: CurrentUnit?): Double {
        throw Exception("encoderOnly not support currentAlert")
    }

    override fun setCurrentAlert(current: Double, unit: CurrentUnit?) {
        throw Exception("encoderOnly not support currentAlert")
    }

    override fun isOverCurrent(): Boolean {
        throw Exception("encoderOnly not support current")
    }

    private val _motor: DcMotorEx

    constructor(motor: DcMotorEx) {
        _motor = motor

        _motor.direction = Direction.FORWARD
        _motor.mode = RunMode.STOP_AND_RESET_ENCODER
        _motor.mode = RunMode.RUN_WITHOUT_ENCODER
    }
}