package tup;

import io.InputReader;
import io.OutputWriter;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        String instance = "umps14";
        int q1 = 7;
        int q2 = 3;
        Problem problem = InputReader.readFile(instance + ".txt", q1, q2);
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        OutputWriter.writeSolutionToFile(instance + "_" + q1 + "_" + q2 + ".txt", bb.bestSolution);
    }
}