package com.example.meepmeeptesting

import com.acmerobotics.roadrunner.AngularVelConstraint
import com.acmerobotics.roadrunner.MinVelConstraint
import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.TrajectoryActionBuilder
import com.acmerobotics.roadrunner.TranslationalVelConstraint
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
                .setConstraints(1.9 / 0.0254, 4.0 / 0.0254 * 2.5, 7.0, 14.0, (0.38) / 0.0254)
                .build()

        val eatVelConstraint = MinVelConstraint(
            listOf(
                TranslationalVelConstraint(0.8 / 0.0254),
                AngularVelConstraint(7.0)
            )
        )

        val shootTime = 0.7

//        fun TrajectoryActionBuilder.circle() =
//            setTangent(0.4)
//                .meterSplineToLinearHeading(Pose2d(0.2, -1.4, -PI * 0.7), -PI * 0.5)
//            .waitSeconds(0.8)
//                .setTangent(PI / 2.0)
//                .meterSplineToLinearHeading(Pose2d(-0.776, -0.656, -PI * 0.75), -PI * 0.9)
//            .waitSeconds(shootTime)

        myBot.runAction(
            myBot.drive.actionBuilder(
                Pose2d(
                    -(1.215 + 0.38 / 2.0) / 0.0254,
                    (-0.91 - 0.38 / 2.0) / 0.0254,
                    -PI / 2.0
                )
            )
                .meterStrafeTo(Vector2d(-0.314, -1.4))
                .waitSeconds(5.0)
                .setReversed(true)
                .meterSplineToLinearHeading(Pose2d(0.0, -1.4, PI), -PI / 2.0)
                .waitSeconds(50.0)
                .meterStrafeTo(Vector2d(-0.314, -1.4))
//                .meterStrafeToLinearHeading(
//                    Vector2d(-0.776, -0.656),
//                    -PI * 0.75
//                )
//                .waitSeconds(shootTime)
//                .meterStrafeToLinearHeading(Vector2d(0.3, -0.79), -PI / 2.0)
//                .meterStrafeTo(Vector2d(0.3, -1.15), eatVelConstraint)
//                .meterStrafeTo(Vector2d(0.1, -1.35))
//                .waitSeconds(0.8)
//                .meterStrafeTo(Vector2d(0.1, -0.9))
//                .setTangent(PI / 2.0)
//                .meterSplineToLinearHeading(Pose2d(-0.776, -0.656, -PI * 0.75), -PI * 0.9)
//                .waitSeconds(shootTime)
//                .circle()
//                .circle()
//                .circle()
//                .circle()
//                .setTangent(0.0)
//                .meterStrafeToLinearHeading(
//                    Vector2d(-0.314, -0.79),
//                    -PI / 2.0
//                ).meterStrafeTo(Vector2d(-0.314, -1.15), eatVelConstraint)
//                .meterStrafeToLinearHeading(
//                    Vector2d(-0.776, -0.656),
//                    -PI * 0.75
//                )
//                .waitSeconds(shootTime)
//                .meterStrafeToLinearHeading(Vector2d(0.9, -0.79), -PI / 2.0)
//                .meterStrafeTo(Vector2d(0.9, -1.15), eatVelConstraint)
//                .meterStrafeToLinearHeading(Vector2d(-0.776, -0.656), -PI * 0.75)
//                .waitSeconds(shootTime)
//                .meterStrafeTo(Vector2d(-1.2, -0.656))
                .build()
        )

        meepMeep.setBackground(Background.FIELD_DECODE_OFFICIAL)
            .setDarkMode(true)
            .setBackgroundAlpha(0.95f)
            .addEntity(myBot)
            .start()
    }
}