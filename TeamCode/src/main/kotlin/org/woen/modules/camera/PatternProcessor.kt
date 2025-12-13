package org.woen.modules.camera

import android.graphics.Bitmap
import android.graphics.Canvas
import org.firstinspires.ftc.robotcore.external.function.Consumer
import org.firstinspires.ftc.robotcore.external.function.Continuation
import org.firstinspires.ftc.robotcore.external.stream.CameraStreamSource
import org.firstinspires.ftc.robotcore.internal.camera.calibration.CameraCalibration
import org.firstinspires.ftc.vision.VisionProcessor
import org.opencv.android.Utils
import org.opencv.core.Core
import org.opencv.core.Core.ROTATE_90_COUNTERCLOCKWISE
import org.opencv.core.Core.rotate
import org.opencv.core.Mat
import org.opencv.core.Scalar
import org.opencv.core.Size
import org.opencv.imgproc.Imgproc
import org.woen.telemetry.Configs
import java.util.concurrent.atomic.AtomicReference

class Patternproccesor : VisionProcessor, CameraStreamSource {
    var x: Double = 590.0
    var y: Double = 480.0
    private var _lastFrame =
        AtomicReference(Bitmap.createBitmap(1, 1, Bitmap.Config.RGB_565))

    override fun init(
        width: Int,
        height: Int,
        calibration: CameraCalibration?
    ) {
        _lastFrame.set(Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565))

    }

    override fun processFrame(
        frame: Mat?,
        captureTimeNanos: Long
    ): Any? {
        val copiedFrame = Mat()

        rotate(frame, copiedFrame, ROTATE_90_COUNTERCLOCKWISE)
        Imgproc.cvtColor(copiedFrame, copiedFrame, Imgproc.COLOR_RGB2HSV) //конвертация в хсв


        Imgproc.resize(copiedFrame, copiedFrame, Size(x, y)) // установка разрешения
        Core.inRange(
            frame,
            Scalar(Configs.CAMERA.CAMERA_H_RED_DOWN, Configs.CAMERA.CAMERA_C_RED_DOWN, Configs.CAMERA.CAMERA_V_RED_DOWN),
            Scalar(Configs.CAMERA.CAMERA_H_RED_UP, Configs.CAMERA.CAMERA_C_RED_UP, Configs.CAMERA.CAMERA_V_RED_UP),
            frame
        )
        //Core.bitwise_or(img_range_red, img_range_blue, frame);//объединяем два инрейнджа
        Imgproc.erode(
            frame,
            frame,
            Imgproc.getStructuringElement(
                Imgproc.MORPH_ERODE,
                Size(Configs.CAMERA.CAMERA_KSIZE, Configs.CAMERA.CAMERA_KSIZE)
            )
        ) // Сжать
        Imgproc.dilate(
            frame,
            frame,
            Imgproc.getStructuringElement(
                Imgproc.MORPH_ERODE,
                Size(Configs.CAMERA.CAMERA_KSIZE, Configs.CAMERA.CAMERA_KSIZE)
            )
        ) // Раздуть


        val b = Bitmap.createBitmap(
            copiedFrame.width(),
            copiedFrame.height(),
            Bitmap.Config.RGB_565
        )
        val moments = Imgproc.moments(copiedFrame)

        val boundingRect = Imgproc.boundingRect(copiedFrame) //boudingRect представляем прямоугольник


        Utils.matToBitmap(copiedFrame, b)
        _lastFrame.set(b)

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

    override fun getFrameBitmap(
        continuation: Continuation<out Consumer<Bitmap?>?>?
    ) {
        continuation!!.dispatch { bitmapConsumer ->
            bitmapConsumer.accept(_lastFrame.get())
        }
    }
}
