package org.woen.modules.camera

import com.qualcomm.hardware.limelightvision.Limelight3A
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit
import org.firstinspires.ftc.robotcore.external.navigation.DistanceUnit
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D
import org.firstinspires.ftc.robotcore.external.navigation.Position
import org.firstinspires.ftc.robotcore.external.navigation.YawPitchRollAngles
import kotlin.math.cos
import kotlin.math.sin

class TurretLocalizer {
    fun getGlobalRobotPose(limelight: Limelight3A, turretYawDeg: Double): Pose3D? {
        val result = limelight.getLatestResult()
        if (result == null || !result.isValid()) return null

        val fiducials = result.getFiducialResults()
        if (fiducials.isEmpty()) return null

        val fr = fiducials.get(0)
        val camInTagSpace = fr.getRobotPoseTargetSpace()
        if (camInTagSpace == null) return null

        val tagX: Double
        val tagY: Double
        val tagYaw: Double
        if (fr.getFiducialId() == 24) {
            tagX = -1.4827
            tagY = 1.4133
            tagYaw = 54.0
        } else if (fr.getFiducialId() == 20) {
            tagX = -1.4827
            tagY = -1.4133
            tagYaw = 180 - 54.0
        } else return null

        // 1. Относительные координаты камеры (Target Space)
        val xRel = camInTagSpace.getPosition().x
        val yRel = camInTagSpace.getPosition().z

        // 2. Учет выноса камеры на турели (Смещение линзы относительно центра робота)
        val turretRad = Math.toRadians(turretYawDeg)
        val camX_robot = H3_AXIS_TO_CENTER + cos(turretRad) * H1_CAMERA_TO_AXIS
        val camY_robot = sin(turretRad) * H1_CAMERA_TO_AXIS

        // 3. Расчет угла робота (твой рабочий вариант)
        val camYawRel = camInTagSpace.getOrientation().getYaw(AngleUnit.DEGREES)
        val robotYawField = tagYaw + (camYawRel - turretYawDeg) + 90

        // 4. Коррекция позиции: вычитаем смещение камеры из координат, полученных по тегу
        // Сначала поворачиваем смещение (camX, camY) в систему координат тега
        val relRobotRad = Math.toRadians(camYawRel - turretYawDeg)
        val offsetX_tag = camX_robot * cos(relRobotRad) - camY_robot * sin(relRobotRad)
        val offsetY_tag = camX_robot * sin(relRobotRad) + camY_robot * cos(relRobotRad)

        val correctedXRel = xRel - offsetX_tag
        val correctedYRel = yRel - offsetY_tag

        // 5. Глобальный поворот вектора на поле
        val tagRad = Math.toRadians(tagYaw)
        val cosT = cos(tagRad)
        val sinT = sin(tagRad)

        val finalX = tagX + (correctedXRel * cosT - correctedYRel * sinT)
        val finalY = tagY + (correctedXRel * sinT + correctedYRel * cosT)

        return Pose3D(
            Position(DistanceUnit.METER, finalX, finalY, 0.0, 0),
            YawPitchRollAngles(AngleUnit.DEGREES, robotYawField, 0.0, 0.0, 0)
        )
    }

    companion object {
        private const val TAG = "TurretLoc"

        // Твои размеры из прошлых сообщений (в метрах)
        var H1_CAMERA_TO_AXIS: Double = 0.164
        var H3_AXIS_TO_CENTER: Double = 0.035
    }
}