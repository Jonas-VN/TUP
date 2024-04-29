package io;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class OutputWriter {
    public static void writeSolutionToFile(String filePath, int[][] solution) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("src/io/output/" + filePath))) {
            for (int[] array1D : solution) {
                for (int element : array1D) {
                    writer.write(element + " ");
                }
                writer.newLine();
            }
            System.out.println("Best solution has been written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing solution to file: " + e.getMessage());
        }
    }
}
