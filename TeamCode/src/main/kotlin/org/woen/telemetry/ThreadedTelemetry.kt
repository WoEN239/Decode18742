package org.woen.telemetry

import android.content.Context
import com.acmerobotics.dashboard.FtcDashboard
import com.acmerobotics.dashboard.config.ValueProvider
import org.firstinspires.ftc.ftccommon.external.OnCreate
import java.util.concurrent.atomic.AtomicReference
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.findAnnotation

annotation class ThreadedConfig(val category: String, val name: String = "")

class ThreadedTelemetry {
    companion object {
        @OnCreate
        @JvmStatic
        fun create(context: Context?) {
            FtcDashboard.start(context)

            val configs = ThreadedConfigs::class.declaredMemberProperties.filter {
                it.returnType.classifier == AtomicProvider::class &&
                        it.findAnnotation<ThreadedConfig>() != null
            }

            for (i in configs) {
                val value = i.get(ThreadedConfigs) as AtomicProvider<*>
                val annotation = i.annotations[0] as ThreadedConfig

                FtcDashboard.getInstance().addConfigVariable(
                    annotation.category,
                    annotation.name.ifEmpty { i.name }, value
                )
            }
        }
    }

    class AtomicProvider<T>(default: T) : ValueProvider<T> {
        private var data = AtomicReference(default)

        override fun get(): T = data.get()

        override fun set(value: T?) {
            if (value != null)
                data.set(value)
        }
    }
}