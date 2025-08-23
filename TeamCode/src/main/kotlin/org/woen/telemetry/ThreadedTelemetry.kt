package org.woen.telemetry

import android.content.Context
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.ValueProvider
import org.firstinspires.ftc.ftccommon.external.OnCreate

class ThreadedTelemetry {
    companion object{
        @OnCreate
        @JvmStatic
        fun create(context: Context?) {
            FtcDashboard.start(context)

            FtcDashboard.getInstance().addConfigVariable<Int>("a", "b", SimpleProvider(5))
        }
    }

    class SimpleProvider<T>(default: T): ValueProvider<T>{
        private var data = default

        override fun get() = data

        override fun set(value: T?) {
            if(value != null)
                data = value
        }
    }
}