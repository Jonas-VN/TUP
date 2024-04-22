package tup;

import io.InputReader;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Problem problem = InputReader.readFile("umps8.txt", 4, 2);
        System.out.println(problem);
        long startTime = System.currentTimeMillis();
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        long endTime = System.currentTimeMillis();
        // LowerBound lb = new LowerBound(problem);
        // lb.solve();

       System.out.println("Execution time: " + (endTime - startTime) + "ms");

    }
}