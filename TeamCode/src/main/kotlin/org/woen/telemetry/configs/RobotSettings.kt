package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config



object RobotSettings
{
    @Config
    internal object CONTROLS
    {
        @JvmField
        var DRIVE_TO_SHOOTING_ZONE = false

        @JvmField
        var TRY_TERMINATE_INTAKE_WHEN_SHOOTING = true

        @JvmField
        var IGNORE_DUPLICATE_SHOOTING_COMMAND  = true
    }
}