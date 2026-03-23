package org.woen.enumerators



class Shooting
{
    enum class Mode
    {
        FIRE_EVERYTHING_YOU_HAVE,
        FIRE_PATTERN_CAN_SKIP,
        FIRE_UNTIL_PATTERN_IS_BROKEN
    }
}


object StockPattern
{
    object Request
    {
        val STREAM = arrayOf(
            BallRequest.ANY_CLOSEST,
            BallRequest.ANY_CLOSEST,
            BallRequest.ANY_CLOSEST)

        val PPP = arrayOf(
            BallRequest.PURPLE,
            BallRequest.PURPLE,
            BallRequest.PURPLE)
        val GGG = arrayOf(
            BallRequest.GREEN,
            BallRequest.GREEN,
            BallRequest.GREEN)

        val GPP = arrayOf(
            BallRequest.GREEN,
            BallRequest.PURPLE,
            BallRequest.PURPLE)
        val PGP = arrayOf(
            BallRequest.PURPLE,
            BallRequest.GREEN,
            BallRequest.PURPLE)
        val PPG = arrayOf(
            BallRequest.PURPLE,
            BallRequest.PURPLE,
            BallRequest.GREEN)
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
    }
}