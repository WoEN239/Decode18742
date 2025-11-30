package woen239.enumerators;


import static org.woen.telemetry.Configs.STORAGE.MAX_BALL_COUNT;

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
                ANY_TWO_IDENTICAL_COLORS = 3, ANY_THREE_IDENTICAL_COLORS = 4,
                    USE_DETECTED_PATTERN = 4;



        public enum Name
        {
            ANY,
            ALL_GREEN,
            ALL_PURPLE,

            ANY_TWO_IDENTICAL_COLORS,
            ANY_THREE_IDENTICAL_COLORS,
            USE_DETECTED_PATTERN
        }



        static public int RequestedBallCount(Name patternId)
        {
            return patternId == Name.ANY_TWO_IDENTICAL_COLORS ? 2 : MAX_BALL_COUNT;
        }
        static public BallRequest.Name[] TryConvertToPatternSequence(Name patternName)
        {
            int patternId = ToInt(patternName);
            if (patternId < ANY_TWO_IDENTICAL_COLORS)
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
                case ANY_TWO_IDENTICAL_COLORS:   return ANY_TWO_IDENTICAL_COLORS;
                case ANY_THREE_IDENTICAL_COLORS: return ANY_THREE_IDENTICAL_COLORS;
                default: return USE_DETECTED_PATTERN;
            }
        }
        static public Name ToName(int patternId)
        {
            switch (patternId)
            {
                case ANY:        return Name.ANY;
                case ALL_GREEN:  return Name.ALL_GREEN;
                case ALL_PURPLE: return Name.ALL_PURPLE;
                case ANY_TWO_IDENTICAL_COLORS:   return Name.ANY_TWO_IDENTICAL_COLORS;
                case ANY_THREE_IDENTICAL_COLORS: return Name.ANY_THREE_IDENTICAL_COLORS;
                default: return Name.USE_DETECTED_PATTERN;
            }
        }
    }
}