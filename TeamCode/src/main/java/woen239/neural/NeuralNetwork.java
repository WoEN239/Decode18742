package woen239.neural;

import java.io.File;
import java.io.FileOutputStream;

import android.app.Activity;

import org.woen.telemetry.ThreadedTelemetry;
import org.firstinspires.ftc.robotcore.internal.system.AppUtil;


public class NeuralNetwork {
    private int[] _layers; // Sizes of all nn layers

    private Activation[] _actvf; //  Activation Functions


    private Matrix[] weights;
    private double[][] bias, neuronBias;
    private double[][] neurons, neuronErr;


    public NeuralNetwork(int[] layerSettings, Activation[] actvf) {
        _layers = layerSettings;
        _actvf = actvf;
    }

    public NeuralNetwork() {} // Temporary for test

    public void Learn()
    {

    }

    public boolean SaveParameters()
    {
        File baseDir = GetCurrentPackagePath();
        if (baseDir == null)
        {
            ThreadedTelemetry.getLAZY_INSTANCE().log
            (
                "\n[!]  - Error saving nn parameters!"
                + "\n       Failed getting base program directory"
            );
            return false;
        }

        String data = "Aboba";
        File path = SavePathHelper(baseDir, baseDir.getAbsolutePath());

        int fileAmount  = FileAmount(path);
        String fileName = (fileAmount + 1) + "-Test.aboba";

        try
        {
            if (!path.exists())
            {
                if (!path.mkdirs())
                {
                    ThreadedTelemetry.getLAZY_INSTANCE().log
                    (
                        "\n[!]  - Error saving nn parameters!"
                            + "\n       Failed creating save directory"
                    );
                    return false;
                }
            }

            File fileHandle = new File(path, fileName);
            FileOutputStream fos = new FileOutputStream(fileHandle);

            fos.write(data.getBytes());
            fos.close();

            ThreadedTelemetry.getLAZY_INSTANCE().log
            (
                "\n[i]  - Successfully saved nn parameters!"
                    + "\n       Path: " + path
                    + "\n       File name: " + fileName
            );

            return true;
        }
        catch (Exception e)
        {
            ThreadedTelemetry.getLAZY_INSTANCE().log
                    (
                            "\n[!]  - Error saving nn parameters!\n"
                                    + e.toString() + "\n"
                    );

            return false;
        }
    }

    public void LoadParameters()
    {

    }

    public void RandomInit()
    {

    }




    private int FileAmount (File directory)
    {
        if (!directory.exists()) return -1;

        File[] files = directory.listFiles();
        return files == null ? 0 : files.length;
    }

    private File GetCurrentPackagePath()
    {
        // Calculate the path to ../Saves from your current package
        Activity activity = AppUtil.getInstance().getActivity();

        return activity == null ? null : activity.getFilesDir();
    }

    private File SavePathHelper(File baseDir, String basePath)
    {
        // Navigate up from /nn to /java, then to /Saves
        if (basePath.endsWith("/nn"))
            return new File(basePath.substring(0, basePath.length() - 3) + "/saves");

        else
            return new File(baseDir.getParentFile(), "saves");
        // Fallback if path structure is different
    }
}