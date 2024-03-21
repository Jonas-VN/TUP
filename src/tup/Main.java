package tup;

import io.InputReader;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Problem problem = InputReader.readFile("umps8.txt", 2, 2);
        System.out.println(problem);
        BranchAndBound bb = new BranchAndBound(problem);
        bb.solve();
    }
}