package org.woen.tests

import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.Config
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode
import com.qualcomm.robotcore.eventloop.opmode.TeleOp
import com.qualcomm.robotcore.hardware.DcMotorEx
import com.qualcomm.robotcore.hardware.DcMotorSimple
import com.qualcomm.robotcore.hardware.VoltageSensor
import com.qualcomm.robotcore.util.ElapsedTime
import org.woen.telemetry.Configs
import org.woen.utils.exponentialFilter.ExponentialFilter
import org.woen.utils.regulator.Regulator
import kotlin.math.PI

@Config
internal object PULLEY_TEST_CONFIG {
    @JvmField
    var TARGET = 0.0
}

@TeleOp
class PulleyTest : LinearOpMode() {
    override fun runOpMode() {
        val battery = hardwareMap.get(VoltageSensor::class.java, "Control Hub")
        val pulleyMotor = hardwareMap.get("pulleyMotor") as DcMotorEx

        pulleyMotor.direction = DcMotorSimple.Direction.REVERSE

        val regulator = Regulator(Configs.TURRET.PULLEY_REGULATOR)
        val velocityFilter = ExponentialFilter(Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.get())

        Configs.TURRET.PULLEY_VELOCITY_FILTER_COEF.onSet += {
            velocityFilter.coef = it
        }

        var oldMotorPosition = 0.0
        var motorVelocity = 0.0

        val deltaTime = ElapsedTime()

        waitForStart()
        resetRuntime()

        regulator.start()
        deltaTime.reset()

        while (opModeIsActive()) {
            val target =
                (PULLEY_TEST_CONFIG.TARGET * Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION) / (2.0 * PI * Configs.TURRET.PULLEY_RADIUS)

            val currentMotorPosition = pulleyMotor.currentPosition.toDouble()

            val rawVelocity = (currentMotorPosition - oldMotorPosition) / deltaTime.seconds()

            deltaTime.reset()

            motorVelocity =
                velocityFilter.updateRaw(motorVelocity, rawVelocity - motorVelocity)

            oldMotorPosition = currentMotorPosition

            pulleyMotor.power = regulator.update(
                target - motorVelocity,
                target
            ) / battery.voltage

            val telem = FtcDashboard.getInstance().telemetry

            telem.addData(
                "target",
                (target * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION
            )
            telem.addData(
                "current",
                (motorVelocity * 2.0 * PI * Configs.TURRET.PULLEY_RADIUS) / Configs.TURRET.PULLEY_TICKS_IN_REVOLUTION
            )
            telem.addData("ticks velocity", motorVelocity)

            telem.update()
        }
    }
}