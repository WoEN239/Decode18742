package org.woen.enumerators


import org.woen.telemetry.configs.Alias.MAX_BALL_COUNT



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
        object Sequence
        {
            object Request
            {
                val STREAM = arrayOf(
                    BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST)

                val PPP = arrayOf(
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE)
                val GGG = arrayOf(
                    BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN)

                val GPP = arrayOf(
                    BallRequest.Name.GREEN,
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE)
                val PGP = arrayOf(
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.GREEN,
                    BallRequest.Name.PURPLE)
                val PPG = arrayOf(
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.GREEN)

                object Name
                {
                    val STREAM = arrayOf(
                        BallRequest.Name.ANY_CLOSEST,
                        BallRequest.Name.ANY_CLOSEST,
                        BallRequest.Name.ANY_CLOSEST)
                    val EMPTY = arrayOf(
                        BallRequest.Name.NONE,
                        BallRequest.Name.NONE,
                        BallRequest.Name.NONE)

                    val PPP = arrayOf(
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.PURPLE)
                    val GGG = arrayOf(
                        BallRequest.Name.GREEN,
                        BallRequest.Name.GREEN,
                        BallRequest.Name.GREEN)

                    val GPP = arrayOf(
                        BallRequest.Name.GREEN,
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.PURPLE)
                    val PGP = arrayOf(
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.GREEN,
                        BallRequest.Name.PURPLE)
                    val PPG = arrayOf(
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.PURPLE,
                        BallRequest.Name.GREEN)
                }
            }


            object Storage
            {
                val STREAM = arrayOf(
                    Ball(Ball.UNKNOWN_COLOR, Ball.Name.UNKNOWN_COLOR),
                    Ball(Ball.UNKNOWN_COLOR, Ball.Name.UNKNOWN_COLOR),
                    Ball(Ball.UNKNOWN_COLOR, Ball.Name.UNKNOWN_COLOR),
                    Ball(Ball.NONE,          Ball.Name.NONE))
                val EMPTY = arrayOf(
                    Ball(),
                    Ball(),
                    Ball(),
                    Ball())

                val PPP = arrayOf(
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.NONE,   Ball.Name.NONE))
                val GGG = arrayOf(
                    Ball(Ball.GREEN, Ball.Name.PURPLE),
                    Ball(Ball.GREEN, Ball.Name.GREEN),
                    Ball(Ball.GREEN, Ball.Name.GREEN),
                    Ball(Ball.NONE,  Ball.Name.NONE))

                val GPP = arrayOf(
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.GREEN,  Ball.Name.GREEN),
                    Ball(Ball.NONE,   Ball.Name.NONE))
                val PGP = arrayOf(
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.GREEN,  Ball.Name.GREEN),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.NONE,   Ball.Name.NONE))
                val PPG = arrayOf(
                    Ball(Ball.GREEN,  Ball.Name.GREEN),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.PURPLE, Ball.Name.PURPLE),
                    Ball(Ball.NONE,   Ball.Name.NONE))

                object Name
                {
                    val STREAM = arrayOf(
                        Ball.Name.UNKNOWN_COLOR,
                        Ball.Name.UNKNOWN_COLOR,
                        Ball.Name.UNKNOWN_COLOR)
                    val EMPTY = arrayOf(
                        Ball.Name.NONE,
                        Ball.Name.NONE,
                        Ball.Name.NONE)

                    val PPP = arrayOf(
                        Ball.Name.PURPLE,
                        Ball.Name.PURPLE,
                        Ball.Name.PURPLE)
                    val GGG = arrayOf(
                        Ball.Name.GREEN,
                        Ball.Name.GREEN,
                        Ball.Name.GREEN)

                    val GPP = arrayOf(
                        Ball.Name.GREEN,
                        Ball.Name.PURPLE,
                        Ball.Name.PURPLE)
                    val PGP = arrayOf(
                        Ball.Name.PURPLE,
                        Ball.Name.GREEN,
                        Ball.Name.PURPLE)
                    val PPG = arrayOf(
                        Ball.Name.PURPLE,
                        Ball.Name.PURPLE,
                        Ball.Name.GREEN)
                }
            }
        }



        private val _patternSequence = arrayOf(
            Sequence.Request.STREAM,

            Sequence.Request.PPP,
            Sequence.Request.GGG,

            Sequence.Request.GPP,
            Sequence.Request.PGP,
            Sequence.Request.PPG,
        )

        enum class Name
        {
            ANY,

            ALL_PURPLE,
            ALL_GREEN,

            GPP,
            PGP,
            PPG,

            ANY_TWO_IDENTICAL_COLORS,
            ANY_THREE_IDENTICAL_COLORS,
            USE_DETECTED_PATTERN
        }



        const val ANY: Int = 0
        const val ALL_GREEN:  Int = 1
        const val ALL_PURPLE: Int = 2

        const val GPP: Int = 3
        const val PGP: Int = 4
        const val PPG: Int = 5

        const val ANY_TWO_IDENTICAL_COLORS:   Int = 6
        const val ANY_THREE_IDENTICAL_COLORS: Int = 7
        const val USE_DETECTED_PATTERN: Int = 8


        fun requestedBallCount(patternId: Name): Int
            = if (patternId == Name.ANY_TWO_IDENTICAL_COLORS) 2
            else MAX_BALL_COUNT


        fun tryConvertToPatternSequence(patternName: Name): Array<BallRequest.Name>
        {
            val patternId: Int = toInt(patternName)
            return if (patternId < ANY_TWO_IDENTICAL_COLORS) _patternSequence[patternId]
            else arrayOf()
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