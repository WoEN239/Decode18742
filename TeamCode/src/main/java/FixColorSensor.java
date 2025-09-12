import static com.qualcomm.hardware.ams.AMSColorSensor.AMS_TCS34725_ADDRESS;

import com.qualcomm.hardware.adafruit.AdafruitI2cColorSensor;
import com.qualcomm.hardware.ams.AMSColorSensor;
import com.qualcomm.robotcore.hardware.I2cDeviceSynchDeviceWithParameters;

import java.lang.reflect.Field;

public class FixColorSensor {
    public static AdafruitI2cColorSensor fixSensor(AdafruitI2cColorSensor sensor){
        try {
            AMSColorSensor.class.getDeclaredField("AMS_TCS34725_ID").setAccessible(true);

            AMSColorSensor.Parameters parameters = new AMSColorSensor.Parameters(AMS_TCS34725_ADDRESS, 0x4D);

            Field paramField = I2cDeviceSynchDeviceWithParameters.class.getDeclaredField("parameters");

            paramField.setAccessible(true);

            try {
                paramField.set(sensor, parameters);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            sensor.initialize();
        } catch (NoSuchFieldException e) {
            throw new RuntimeException("color sensor hack not successful");
        }

        return sensor;
    }
}
