package io;

import tup.Problem;

import java.io.*;
import java.util.Arrays;
import java.util.Scanner;


public class InputReader {
    public static Problem readFile(String fileName, int q1, int q2) throws FileNotFoundException {
        File file = new File(new File("src\\io\\input\\" + fileName).getAbsolutePath());
        // Read the file
        Scanner scanner = new Scanner(file);
        String line;

        line = removePadding(scanner, "nTeams");
        assert line != null;
        int nTeams = Integer.parseInt(line.split("=")[1].split(";")[0]);

        removePadding(scanner, "dist");
        int[][] dist = parseArray(scanner, nTeams);

        removePadding(scanner, "opponents");
        int[][] opponents = parseArray(scanner, (nTeams - 1) * 2);

        scanner.close();
        return new Problem(nTeams, dist, opponents, q1, q2);

    }

    private static int[][] parseArray(Scanner scanner, int nTeams) {
        int[][] array = new int[nTeams][nTeams];
        for (int i = 0; i < nTeams; i++) {
            String line = scanner.nextLine().split("\\[")[1].split("]")[0];
            array[i] = Arrays.stream(line.trim().split("\\s+" )).mapToInt(Integer::parseInt).toArray();
        }
        return array;
    }

    private static String removePadding(Scanner scanner, String end) {
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains(end)) {
                return line;
            }
        }
        return null;
    }
}