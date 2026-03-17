package org.woen.enumerators



class Shooting
{
    enum class Mode
    {
        FIRE_EVERYTHING_YOU_HAVE,
        FIRE_PATTERN_CAN_SKIP,
        FIRE_UNTIL_PATTERN_IS_BROKEN
    }


    object StockPattern
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
}