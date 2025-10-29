package woen239.neural;

import androidx.annotation.NonNull;

public class Matrix
{
    private double[][] _matrix;
    private int  _row, _column;


    public int Row()    { return _row; }
    public int Column() { return _column; }
    public double[][] MatrixArr() { return _matrix; }


    public void Initialization(int row, int column)
    {
        _row = row;
        _column = column;

        _matrix = new double[row][column];
    }

    public void Random()
    {
        for (int x = 0; x < _row; x++)
        {
            for (int y = 0; y < _column; y++)
            {
                _matrix[x][y] = ((Math.random() % 100) * 0.05 / (_column + 45));
            }
        }
    }

    public static void Multi(@NonNull Matrix matrix, int n, double b, double[] neuron, double[] c)
    {
        if (matrix.Column() != n)
            throw new RuntimeException("matrix multiplication error \n");

        for (int x = 0; x < matrix.Row(); x++)
        {
            double buffer = 0;

            for (int y = 0; y < matrix.Column(); y++)
                buffer += matrix.MatrixArr()[x][y] * neuron[y];

            c[x] = buffer;
        }
    }

    public static void SumVector(double[] a, double[] b, int n)
    {
        for (int i = 0; i < n; i++) a[i] += b[i];
    }
    public static void MultiT(@NonNull Matrix matrix, double[] neuron, int n, double[] c)
    {
        if (matrix.Row() != n)
            throw new RuntimeException("MultiT error \n");

        double buffer = 0;
        for (int y = 0; y < matrix.Column(); y++)
        {
            for (int x = 0; x < matrix.Row(); x++)
                buffer += matrix.MatrixArr()[x][y] * neuron[x];

            c[y] = buffer;
        }
    }
};