package tup;

import io.InputReader;
import io.OutputWriter;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        String instance = "umps10";
        int q1 = 5;
        int q2 = 2;
        Problem problem = InputReader.readFile(instance + ".txt", q1, q2);
        long StartTime = System.currentTimeMillis();
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        System.out.println("Total time: " + (System.currentTimeMillis() - StartTime) / 1000.0 + "s");
        OutputWriter.writeSolutionToFile(instance + "_" + q1 + "_" + q2 + ".txt", bb.bestSolution);
    }
}