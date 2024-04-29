package tup;

import io.InputReader;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException, InterruptedException {
        Problem problem = InputReader.readFile("umps14.txt", 7, 3);
        long StartTime = System.currentTimeMillis();
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        System.out.println("Total time: " + (System.currentTimeMillis() - StartTime) / 1000.0 + "s");
    }
}