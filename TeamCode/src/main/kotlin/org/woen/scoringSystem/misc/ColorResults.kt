package org.woen.scoringSystem.misc


import org.woen.enumerators.Ball
import org.woen.enumerators.StorageSlot
import org.woen.scoringSystem.storage.MAX_BALL_COUNT



class StorageSlotsUpdates(
    var bottom: Boolean = false,
    var center: Boolean = false,
    var turret: Boolean = false)


class ColorPrediction(var color: Ball.Name, var sensorRelation: Int)
class IntakePredictions
{
    private val intakePredictionB: ArrayList<ColorPrediction> = arrayListOf()
    private val intakePredictionC: ArrayList<ColorPrediction> = arrayListOf()
    private val intakePredictionT: ArrayList<ColorPrediction> = arrayListOf()


    fun clearForSlot(curSlot: Int) = when (curSlot)
    {
        StorageSlot.BOTTOM -> intakePredictionB.clear()
        StorageSlot.CENTER -> intakePredictionC.clear()
        else -> intakePredictionT.clear()
    }
    fun clearAll()
    {
        intakePredictionB.clear()
        intakePredictionC.clear()
        intakePredictionT.clear()
    }

    fun update(colorResults: ColorResults, curSlot: Int)
    {
        val resultB = colorResults.parsedResults[StorageSlot.BOTTOM]
        val resultC = colorResults.parsedResults[StorageSlot.CENTER]
        val resultT = colorResults.parsedResults[StorageSlot.TURRET]

        val updateSlotForSensorC = if (
            colorResults.updateTargetsBCT.turret &&
            Ball.isTrueColor(resultT) &&
            Ball.isNotDirectOpposite(resultT, resultC))
            curSlot else curSlot - 1

        val updateSlotForSensorB = if (
            colorResults.updateTargetsBCT.center &&
            Ball.isTrueColor(resultC) &&
            Ball.isNotDirectOpposite(resultC, resultB))
            updateSlotForSensorC else updateSlotForSensorC - 1

        addPrediction(resultB, StorageSlot.BOTTOM, updateSlotForSensorB)
        addPrediction(resultC, StorageSlot.CENTER, updateSlotForSensorC)
        addPrediction(resultT, StorageSlot.TURRET, curSlot)
    }
    private fun addPrediction(ballColor: Ball.Name, sensorSlot: Int, intakeSlot: Int)
    {
        val sensorRelation = MAX_BALL_COUNT - intakeSlot + sensorSlot
        when (intakeSlot)
        {
            StorageSlot.BOTTOM -> intakePredictionB.add(ColorPrediction(ballColor, sensorRelation))
            StorageSlot.CENTER -> intakePredictionC.add(ColorPrediction(ballColor, sensorRelation))
            StorageSlot.TURRET -> intakePredictionT.add(ColorPrediction(ballColor, sensorRelation))
        }
    }
    fun calcFinal(curSlot: Int): Ball.Name
    {
        val predictions = when (curSlot)
        {
            StorageSlot.BOTTOM -> intakePredictionB
            StorageSlot.CENTER -> intakePredictionC
            else -> intakePredictionT
        }

        var countP = 0
        var countG = 0
        var lastColor = Ball.Name.UNKNOWN_COLOR
        predictions.forEach{ p ->
            if (p.color == Ball.Name.PURPLE) {
                countP += p.sensorRelation
                lastColor = p.color
            }
            if (p.color == Ball.Name.GREEN) {
                countG += p.sensorRelation
                lastColor = p.color
            }
        }

        return if (countP > countG) Ball.Name.PURPLE
          else if (countG > countP) Ball.Name.GREEN else lastColor
    }
}



class ColorResults
{
    var colorIntakeIsActive = false
    var intakePredictions = IntakePredictions()
    var updateTargetsBCT = StorageSlotsUpdates()
    var parsedResults = arrayOf(Ball.Name.NOT_UPDATED, Ball.Name.NOT_UPDATED, Ball.Name.NOT_UPDATED)



    fun reactivateAllColorTargets()
    {
        updateTargetsBCT.bottom = true
        updateTargetsBCT.center = true
        updateTargetsBCT.turret = true
        colorIntakeIsActive = true
    }
    fun reactivateColorTargetsForIntake() { updateTargetsBCT.bottom = true }
    fun reactivateColorTargetsForShooting(isLastBall: Boolean)
    {
        updateTargetsBCT.bottom = true
        if (!isLastBall) updateTargetsBCT.center = true
        updateTargetsBCT.turret = true
    }
    fun deactivateNextColorTarget(isShooting: Boolean)
    {
        if (!isShooting)
        {
            if (!updateTargetsBCT.center)
            {
                updateTargetsBCT.bottom = false
                colorIntakeIsActive = false
            }
            if (!updateTargetsBCT.turret) updateTargetsBCT.center = false
            updateTargetsBCT.turret = false
        }
    }


    fun finishedIntakeCountByColors(): Int
    {
        return if (Ball.isMaskEmpty(parsedResults[StorageSlot.TURRET])) 0
          else if (Ball.isMaskEmpty(parsedResults[StorageSlot.CENTER])) 1
          else if (Ball.isMaskEmpty(parsedResults[StorageSlot.BOTTOM])) 2
          else 3
    }
    fun isEmptyBySensors()
        =  Ball.isMaskEmpty(parsedResults[StorageSlot.BOTTOM])
        && Ball.isMaskEmpty(parsedResults[StorageSlot.CENTER])
        && Ball.isMaskEmpty(parsedResults[StorageSlot.TURRET])
    fun onlyLastBallForShooting()
        =   Ball.isMaskEmpty(parsedResults[StorageSlot.BOTTOM])
        &&  Ball.isMaskEmpty(parsedResults[StorageSlot.CENTER])
        && !Ball.isMaskEmpty(parsedResults[StorageSlot.TURRET])
}