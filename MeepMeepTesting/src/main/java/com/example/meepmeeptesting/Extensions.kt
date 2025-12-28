package com.example.meepmeeptesting

import com.acmerobotics.roadrunner.Pose2d
import com.acmerobotics.roadrunner.TrajectoryActionBuilder
import com.acmerobotics.roadrunner.Vector2d
import com.acmerobotics.roadrunner.VelConstraint

fun TrajectoryActionBuilder.meterStrafeTo(pos: Vector2d, velConstraint: VelConstraint? = null): TrajectoryActionBuilder =
    strafeTo(Vector2d(pos.x / 0.0254, pos.y / 0.0254), velConstraint)

fun TrajectoryActionBuilder.meterSplineTo(pos: Vector2d, tangent: Double, velConstraint: VelConstraint? = null) =
    splineTo(Vector2d(pos.x / 0.0254, pos.y / 0.0254), tangent, velConstraint)

fun TrajectoryActionBuilder.meterSplineToLinearHeading(pos: Pose2d, tangent: Double, velConstraint: VelConstraint? = null) =
    splineToLinearHeading(
        Pose2d(
            pos.position.x / 0.0254,
            pos.position.y / 0.0254,
            pos.heading.log()
        ), tangent, velConstraint
    )

fun TrajectoryActionBuilder.meterSplineToSplineHeading(pos: Pose2d, tangent: Double, velConstraint: VelConstraint? = null) =
    splineToSplineHeading(
        Pose2d(
            pos.position.x / 0.0254,
            pos.position.y / 0.0254,
            pos.heading.log()
        ), tangent, velConstraint
    )

fun TrajectoryActionBuilder.meterStrafeToLinearHeading(pos: Vector2d, rotation: Double, velConstraint: VelConstraint? = null) =
    strafeToLinearHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254), rotation, velConstraint)

fun TrajectoryActionBuilder.meterStrafeToSplineHeading(pos: Vector2d, rotation: Double, velConstraint: VelConstraint? = null) =
    strafeToSplineHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254), rotation, velConstraint)

fun TrajectoryActionBuilder.meterStrafeToConstantHeading(pos: Vector2d, velConstraint: VelConstraint? = null) =
    strafeToConstantHeading(Vector2d(pos.x / 0.0254, pos.y / 0.0254), velConstraint)