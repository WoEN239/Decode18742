package com.example.meepmeeptesting

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.Vector2d
import com.noahbres.meepmeep.MeepMeep
import com.noahbres.meepmeep.MeepMeep.Background
import com.noahbres.meepmeep.roadrunner.DefaultBotBuilder
import kotlin.math.PI

object MeepMeepTesting {
    @JvmStatic
    fun main(args: Array<String>) {
        val meepMeep = MeepMeep(700)

        val myBot =
            DefaultBotBuilder(meepMeep) // Set bot constraints: maxVel, maxAccel, maxAngVel, maxAngAccel, track width
                .setConstraints(2.0 / 0.0254, 2.0 / 0.0254 * 2.5, 12.0, 12.0, (0.38) / 0.0254)
                .build()

        val shootTime = 3.0

        myBot.runAction(
            myBot.drive.actionBuilder(Pose2d(-(1.215 + 0.38 / 2.0) / 0.0254, (-0.91 - 0.38 / 2.0) / 0.0254, -PI / 2.0))
                .meterStrafeToLinearHeading(
                    Vector2d(-0.776, -0.656),
                    -PI * 0.75
                )
                .waitSeconds(shootTime)
                .meterStrafeToLinearHeading(
                    Vector2d(-0.314, -0.716),
                    -PI / 2.0
                ).meterStrafeTo(Vector2d(-0.314, -1.15))
                .setReversed(true)
                .meterSplineTo(
                    Vector2d(-0.05, -1.35),
                    -PI / 2.0
                )
                .waitSeconds(0.8)
                .setReversed(false)
                .meterStrafeToLinearHeading(
                    Vector2d(-0.776, -0.656),
                    -PI * 0.75
                )
                .waitSeconds(shootTime)
                .meterStrafeToLinearHeading(Vector2d(0.3, -0.712), -PI / 2.0)
                .meterStrafeTo(Vector2d(0.3, -1.15))
                .meterStrafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
                .waitSeconds(shootTime)
                .meterStrafeToLinearHeading(Vector2d(0.9, -0.712), -PI / 2.0)
                .meterStrafeTo(Vector2d(0.9, -1.15))
                .meterStrafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
                .build()
        )

        meepMeep.setBackground(Background.FIELD_DECODE_OFFICIAL)
            .setDarkMode(true)
            .setBackgroundAlpha(0.95f)
            .addEntity(myBot)
            .start()
    }
}