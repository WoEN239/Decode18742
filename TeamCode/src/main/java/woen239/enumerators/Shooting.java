package woen239.enumerators;



public class Shooting
{
    public enum Mode
    {
        FIRE_EVERYTHING_YOU_HAVE,
        FIRE_PATTERN_CAN_SKIP,
        FIRE_UNTIL_PATTERN_IS_BROKEN,
        FIRE_ONLY_IF_ENTIRE_REQUEST_IS_VALID
    }

    static public class StockPattern
    {
        static private final BallRequest.Name[][] _patternSequence =
            {
                {
                    BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST,
                    BallRequest.Name.ANY_CLOSEST
                },

                {
                    BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN,
                    BallRequest.Name.GREEN
                },

                {
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE,
                    BallRequest.Name.PURPLE
                },
            };
        static public final int ANY = 0, ALL_GREEN = 1, ALL_PURPLE = 2,
                ANY_THREE_IDENTICAL_COLORS = 3, USE_DETECTED_PATTERN = 4;



        public enum Name
        {
            ANY,
            ALL_GREEN,
            ALL_PURPLE,
            ANY_THREE_IDENTICAL_COLORS,
            USE_DETECTED_PATTERN
        }



        static public BallRequest.Name[] TryConvertToPatternSequence(Name patternName)
        {
            int patternId = ToInt(patternName);
            if (patternId < ANY_THREE_IDENTICAL_COLORS)
                return _patternSequence[patternId];

            else return null;
        }



        static public int ToInt(Name patternName)
        {
            switch (patternName)
            {
                case ANY:        return ANY;
                case ALL_GREEN:  return ALL_GREEN;
                case ALL_PURPLE: return ALL_PURPLE;
                case ANY_THREE_IDENTICAL_COLORS: return ANY_THREE_IDENTICAL_COLORS;
                default: return USE_DETECTED_PATTERN;
            }
        }
    }
}