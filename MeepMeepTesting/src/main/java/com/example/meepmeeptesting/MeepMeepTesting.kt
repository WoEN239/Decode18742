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
                .setConstraints(1.9 / 0.0254, 4.0 / 0.0254 * 2.5, 7.0, 16.0, (0.38) / 0.0254)
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
                    0.858 / 0.0254,
                    1.558 / 0.0254, PI / 2.0
                )
            )
                .setReversed(true)
                .splineToConstantHeading(Vector2d(0.067 / 0.0254, 1.379 / 0.0254), PI / 2.0)
                .build()
        )

        meepMeep.setBackground(Background.FIELD_DECODE_OFFICIAL)
            .setDarkMode(true)
            .setBackgroundAlpha(0.95f)
            .addEntity(myBot)
            .start()
    }
}