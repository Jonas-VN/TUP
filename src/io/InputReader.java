package io;

import tup.Problem;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;


public class InputReader {
    public static Problem readFile(String fileName, int q1, int q2) throws FileNotFoundException {
        File file = new File(new File(fileName).getAbsolutePath());
        // Read the file
        Scanner scanner = new Scanner(file);

        scanner.nextLine(); // \n
        int nTeams = Integer.parseInt(scanner.nextLine().split("=")[1].split(";")[0]);

        scanner.nextLine(); // \n
        scanner.nextLine(); // dist= [
        int[][] dist = new int[nTeams][nTeams];
        for (int i = 0; i < nTeams; i++) {
            String line = scanner.nextLine().split("\\[")[1].split("]")[0];
            dist[i] = Arrays.stream(line.trim().split("\\s+" )).mapToInt(Integer::parseInt).toArray();
        }

        scanner.nextLine(); // ];
        scanner.nextLine(); // \n
        scanner.nextLine(); // opponents= [
        int[][] opponents = new int[nTeams][nTeams];
        for (int i = 0; i < nTeams; i++) {
            String line = scanner.nextLine().split("\\[")[1].split("]")[0];
            opponents[i] = Arrays.stream(line.trim().split("\\s+" )).mapToInt(Integer::parseInt).toArray();
        }
        return new Problem(nTeams, dist, opponents, q1, q2);
    }
}
