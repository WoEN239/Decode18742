package linearOpModes;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.woen.telemetry.ThreadedTelemetry;

import neural.NeuralNetwork;

@TeleOp
public class TestOpMode extends LinearOpMode {
    @Override
    public void runOpMode() throws InterruptedException {
        waitForStart();
        resetRuntime();

        while (opModeIsActive())
        {
            NeuralNetwork test = new NeuralNetwork();

            test.SaveParameters();

            sleep(1000);
        }
    }
}
