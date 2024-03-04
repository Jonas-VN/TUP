package tup;

import io.InputReader;

import java.io.File;
import java.io.FileNotFoundException;

public class Main {
    public static void main(String[] args) throws FileNotFoundException {
        Problem problem = InputReader.readFile("src\\io\\input\\umps8.txt", 1, 2);
        System.out.println(problem);
    }
}