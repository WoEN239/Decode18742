package org.woen.hardware

import com.qualcomm.hardware.lynx.LynxController
import com.qualcomm.hardware.lynx.LynxDcMotorController
import com.qualcomm.hardware.lynx.LynxModule
import com.qualcomm.robotcore.hardware.DcMotorImpl
import com.qualcomm.robotcore.hardware.HardwareDevice
import com.qualcomm.robotcore.hardware.HardwareMap
import com.qualcomm.robotcore.hardware.configuration.ConfigurationTypeManager
import com.qualcomm.robotcore.hardware.configuration.typecontainers.MotorConfigurationType
import org.woen.ReflectionUtils


object SubstitutionHardware {
    fun substitution(hardwareMap: HardwareMap){
        val devicesNames = ReflectionUtils.getFieldValue<Map<HardwareDevice, Set<String>>>(hardwareMap, "deviceNames")

        if(devicesNames == null)
            return

        for(device in hardwareMap.getAll(LynxController::class.java)){
            val deiceName = devicesNames[device]?.iterator()?.next()

            if(device == null)
                continue

            val module = ReflectionUtils.getFieldValue<LynxModule>(device, "module")

            if(module.isParent)
                continue

            if(device is LynxDcMotorController && device !is OptimizedDcMotorController){
                val optimizedModule = OptimizedDcMotorController(hardwareMap.appContext, module)

                hardwareMap.remove(deiceName, device)
                hardwareMap.dcMotorController.remove(deiceName)

                hardwareMap.dcMotorController.put(deiceName, optimizedModule)
                hardwareMap.put(deiceName, optimizedModule)

                ReflectionUtils.setFieldValue(optimizedModule, module, "module")

                for(motor in hardwareMap.getAll(DcMotorImpl::class.java)){
                    if(motor.controller == device){
                        val motorName = devicesNames[motor]?.iterator()?.next()

                        if(motorName == null)
                            continue

                        val motorType = MotorConfigurationType(
                            ConfigurationTypeManager::class.java,
                            motor.getMotorType().xmlTag,
                            ConfigurationTypeManager.ClassSource.APK
                        )
                        motorType.ticksPerRev = motor.getMotorType().ticksPerRev
                        motorType.gearing = motor.getMotorType().gearing
                        motorType.maxRPM = motor.getMotorType().maxRPM
                        motorType.orientation = motor.getMotorType().orientation
                        motorType.achieveableMaxRPMFraction = motor.getMotorType().achieveableMaxRPMFraction

                        val optimizedMotor = OptimizedDcMotor(optimizedModule, motor.portNumber, motor.direction, motorType)

                        hardwareMap.remove(motorName, motor)
                        hardwareMap.dcMotor.remove(motorName)

                        hardwareMap.dcMotor.put(motorName, optimizedMotor)
                        hardwareMap.put(motorName, optimizedMotor)
                    }
                }
            }
        }
    }
}