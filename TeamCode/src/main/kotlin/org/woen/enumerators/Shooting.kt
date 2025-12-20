package org.woen.enumerators


import org.woen.modules.scoringSystem.storage.Alias.MAX_BALL_COUNT



class Shooting
{
    enum class Mode
    {
        FIRE_EVERYTHING_YOU_HAVE,
        FIRE_PATTERN_CAN_SKIP,
        FIRE_UNTIL_PATTERN_IS_BROKEN,
        FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    }

    object StockPattern
    {
        private val _patternSequence = arrayOf(
            arrayOf(BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST),

            arrayOf(BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN),

            arrayOf(BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE),


            arrayOf(BallRequest.Name.PURPLE,
                BallRequest.Name.PURPLE,
                BallRequest.Name.GREEN),

            arrayOf(BallRequest.Name.PURPLE,
                BallRequest.Name.GREEN,
                BallRequest.Name.PURPLE),

            arrayOf(BallRequest.Name.GREEN,
                BallRequest.Name.PURPLE,
                BallRequest.Name.PURPLE),
        )

        enum class Name
        {
            ANY,
            ALL_GREEN,
            ALL_PURPLE,

            PPG,
            PGP,
            GPP,

            ANY_TWO_IDENTICAL_COLORS,
            ANY_THREE_IDENTICAL_COLORS,
            USE_DETECTED_PATTERN
        }

        const val ANY: Int = 0
        const val ALL_GREEN:  Int = 1
        const val ALL_PURPLE: Int = 2

        const val PPG: Int = 3
        const val PGP: Int = 4
        const val GPP: Int = 5

        const val ANY_TWO_IDENTICAL_COLORS:   Int = 6
        const val ANY_THREE_IDENTICAL_COLORS: Int = 7
        const val USE_DETECTED_PATTERN: Int = 8


        fun requestedBallCount(patternId: Name): Int
            = if (patternId == Name.ANY_TWO_IDENTICAL_COLORS) 2
            else MAX_BALL_COUNT


        fun tryConvertToPatternSequence(patternName: Name): Array<BallRequest.Name>?
        {
            val patternId: Int = toInt(patternName)
            return if (patternId < ANY_TWO_IDENTICAL_COLORS) _patternSequence[patternId]
            else null
        }


        fun toInt(patternName: Name): Int
        {
            return when (patternName)
            {
                Name.ANY -> ANY
                Name.ALL_GREEN  -> ALL_GREEN
                Name.ALL_PURPLE -> ALL_PURPLE

                Name.PPG -> PPG
                Name.PGP -> PGP
                Name.GPP -> GPP

                Name.ANY_TWO_IDENTICAL_COLORS   -> ANY_TWO_IDENTICAL_COLORS
                Name.ANY_THREE_IDENTICAL_COLORS -> ANY_THREE_IDENTICAL_COLORS
                Name.USE_DETECTED_PATTERN -> USE_DETECTED_PATTERN
            }
        }
        fun toName(patternId:  Int): Name
        {
            return when (patternId)
            {
                ANY -> Name.ANY
                ALL_GREEN  -> Name.ALL_GREEN
                ALL_PURPLE -> Name.ALL_PURPLE

                PPG -> Name.PPG
                PGP -> Name.PGP
                GPP -> Name.GPP

                ANY_TWO_IDENTICAL_COLORS   -> Name.ANY_TWO_IDENTICAL_COLORS
                ANY_THREE_IDENTICAL_COLORS -> Name.ANY_THREE_IDENTICAL_COLORS
                else -> Name.USE_DETECTED_PATTERN
            }
        }
    }
}