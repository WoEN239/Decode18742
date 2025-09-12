package barrel;

import barrel.enumerators.Ball;
import barrel.enumerators.BallRequest;
//import barrel.enumerators.IntakeResult;
import barrel.enumerators.RequestResult;

public class Barrel
{
    private BarrelStorage _storage;


    public Barrel()
    {
        _storage = new BarrelStorage();
    }


    public boolean HandleInput()
    {
        //  Get color sensor values
        //  Logic for them
        Ball.Name inputBall = Ball.Name.GREEN;


        //IntakeResult intakeResult = _storage.UpdateAfterInput(inputBall);

        return true;
    }

    public BallRequest.Name HandleOutput(BallRequest requested)
    {
        RequestResult outputResult = _storage.UpdateAfterOutput(requested);




        return BallRequest.Name.NONE;
    }

    public void StopAnyLogic()
    {

    }

    public void ResumeLogic()
    {

    }
}
