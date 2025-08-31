package org.woen.hardware

import android.content.Context
import com.qualcomm.hardware.lynx.LynxDcMotorController
import com.qualcomm.hardware.lynx.LynxModule

class OptimizedDcMotorController(context: Context, module: LynxModule) :
    LynxDcMotorController(context, module) {

}