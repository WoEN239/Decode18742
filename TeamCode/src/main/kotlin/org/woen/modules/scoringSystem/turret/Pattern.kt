package org.woen.modules.scoringSystem.turret

import org.woen.enumerators.BallRequest

class Pattern private constructor(val cameraTagId: Int, val subsequence: Array<BallRequest.Name>) {
    companion object {
        val patterns = setOf(
            Pattern(
                21,
                arrayOf(BallRequest.Name.GREEN, BallRequest.Name.PURPLE, BallRequest.Name.PURPLE)
            ),
            Pattern(
                22,
                arrayOf(BallRequest.Name.PURPLE, BallRequest.Name.GREEN, BallRequest.Name.PURPLE)
            ),
            Pattern(
                23,
                arrayOf(BallRequest.Name.PURPLE, BallRequest.Name.PURPLE, BallRequest.Name.GREEN)
            )
        )
    }
}