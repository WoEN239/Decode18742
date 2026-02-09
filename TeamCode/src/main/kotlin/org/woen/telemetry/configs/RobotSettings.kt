package org.woen.telemetry.configs

import com.acmerobotics.dashboard.config.Config



object RobotSettings
{
    @Config
    internal object CONTROLS
    {
        @JvmField
        var TRY_TERMINATE_INTAKE_WHEN_SHOOTING = true

        @JvmField
        var IGNORE_DUPLICATE_SHOOTING_COMMAND  = true
    }
}