package com.example.meepmeeptesting

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.TrajectoryActionBuilder
import com.acmerobotics.roadrunner.Vector2d

fun TrajectoryActionBuilder.meterStrafeTo(pos: Vector2d): TrajectoryActionBuilder =
    strafeTo(Vector2d(pos.x / 0.0254, pos.y / 0.0254))

fun TrajectoryActionBuilder.meterSplineTo(pos: Vector2d, tangent: Double) =
    splineTo(Vector2d(pos.x / 0.0254, pos.y / 0.0254), tangent)

fun TrajectoryActionBuilder.meterSplineToLinearHeading(pos: Pose2d, tangent: Double) =
    splineToLinearHeading(
        Pose2d(
            pos.position.x / 0.0254,
            pos.position.y / 0.0254,
            pos.heading.log()
        ), tangent
    )

fun TrajectoryActionBuilder.meterSplineToSplineHeading(pos: Pose2d, tangent: Double) =
    splineToSplineHeading(
        Pose2d(
            pos.position.x / 0.0254,
            pos.position.y / 0.0254,
            pos.heading.log()
        ), tangent
    )

fun TrajectoryActionBuilder.meterStrafeToLinearHeading(pos: Vector2d, rotation: Double) =
    strafeToLinearHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254), rotation)

fun TrajectoryActionBuilder.meterStrafeToSplineHeading(pos: Vector2d, rotation: Double) =
    strafeToSplineHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254), rotation)

fun TrajectoryActionBuilder.meterStrafeToConstantHeading(pos: Vector2d) =
    strafeToConstantHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254))