package tup;

import io.InputReader;
import io.OutputWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        String instance = "umps14";
        int q1 = 7;
        int q2 = 2;

        var redirectConsoleToFile = false;

        if (redirectConsoleToFile) {
            // Creating a File object that
            // represents the disk file
            PrintStream o = new PrintStream(new File(String.format("tup-log-%s_%d_%d.txt", instance, q1, q2)));

            // Store current System.out
            // before assigning a new value
            PrintStream console = System.out;

            // Assign o to output stream
            // using setOut() method
            System.setOut(o);
        }

        Problem problem = InputReader.readFile(instance + ".txt", q1, q2);
        problem.enablePartialMatching = true;
        problem.enableCaching = true;
        problem.enableLowerBoundPartialMatching = true;
        problem.enableLowerBoundCaching = true;

 //       problem.printTournement();

//        LowerBound lb = new LowerBound(problem);
//        lb.solve();
//        lb.printLowerBounds();


        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        bb.printLowerBound();

        if (redirectConsoleToFile){
            bb.printLowerBounds();
        }

        OutputWriter.writeSolutionToFile(instance + "_" + q1 + "_" + q2 + ".txt", bb.bestSolution);
    }
}