package tup;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class SolutionWriter {
    public static void writeSolutionToFile(String filePath, String solution) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(solution);
            System.out.println("Best solution has been written to " + filePath);
        } catch (IOException e) {
            System.err.println("Error writing solution to file: " + e.getMessage());
        }
    }
}
