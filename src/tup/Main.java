package tup;

import io.InputReader;

import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Problem problem = InputReader.readFile("umps12.txt", 4, 3);
        long StartTime = System.currentTimeMillis();
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
        System.out.println("Total time: " + (System.currentTimeMillis() - StartTime) + "ms");
    }
}