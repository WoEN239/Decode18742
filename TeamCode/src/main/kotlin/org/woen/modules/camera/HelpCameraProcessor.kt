package org.woen.modules.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import org.firstinspires.ftc.robotcore.external.function.Consumer
import org.firstinspires.ftc.robotcore.external.function.Continuation
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration
import org.firstinspires.ftc.vision.VisionProcessor
import org.opencv.android.Utils
import org.opencv.core.Core.ROTATE_90_CLOCKWISE
import org.opencv.core.Core.rotate
import org.opencv.core.Mat
import java.util.concurrent.atomic.AtomicReference

class HelpCameraProcessor: VisionProcessor, CameraStreamSource {
    private var _lastFrame = AtomicReference(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565))

    override fun init(
        width: Int,
        height: Int,
        calibration: CameraCalibration?
    ) {

    }

    override fun processFrame(
        frame: Mat?,
        captureTimeNanos: Long
    ): Any? {
//        rotate(frame, frame, ROTATE_90_CLOCKWISE)

        if(frame != null) {
            val b = Bitmap.createBitmap(
                frame.width(),
                frame.height(),
                Bitmap.Config.RGB_565
            )
            Utils.matToBitmap(frame, b)
            _lastFrame.set(b)
        }

        return frame
    }

    override fun onDrawFrame(
        canvas: Canvas?,
        onscreenWidth: Int,
        onscreenHeight: Int,
        scaleBmpPxToCanvasPx: Float,
        scaleCanvasDensity: Float,
        userContext: Any?
    ) {

    }

    override fun getFrameBitmap(continuation: Continuation<out Consumer<Bitmap?>?>?) {
        continuation!!.dispatch { bitmapConsumer -> bitmapConsumer.accept(_lastFrame.get()) }
    }
}