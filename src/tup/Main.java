package tup;

import io.InputReader;
import io.OutputWriter;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        String instance = "umps12";
        int q1 = 6;
        int q2 = 3;

        Problem problem = InputReader.readFile(instance + ".txt", q1, q2);
        problem.enablePartialMatching = true;
        problem.enableCaching = true;
        problem.enableLowerBoundPartialMatching = true;
        problem.enableLowerBoundCaching = true;
        problem.enableMultiThreading = true;
        problem.enableMultiThreadingSub = true;

        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();

        OutputWriter.writeSolutionToFile(instance + "_" + q1 + "_" + q2 + ".txt", bb.bestSolution);
    }
}