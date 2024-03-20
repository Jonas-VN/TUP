package tup;

import java.util.Arrays;
import java.util.List;

public class BranchAndBound {
    private Problem problem;
    private int bestDistance = Integer.MAX_VALUE;
    private int[][] bestSolution;

    public BranchAndBound(Problem problem) {
        this.problem = problem;
        bestSolution = new int[problem.nTeams][problem.nTeams];
    }

    public void solve() {
        int[][] path = new int[problem.nTeams][2*problem.nTeams - 2];

        // Wijs in de eerste ronde elke scheidsrechter willekeurig toe aan een wedstrijd
        for (int i = 0; i < problem.nTeams; i++) {
            path[i][0] = i;//note: in debuggen krijgt referee '0' wedstrijd '0' toegewezen, kan misschien verwarring brengen :)
        }

        // Voer het branch-and-bound algoritme uit vanaf de tweede ronde
       // branchAndBound(path, 0, 0);
        problem.assignUmpires();
    }
/*
    private void branchAndBound(int[][] path, int umpire, int round) {
        int uPlus = (umpire % problem.nTeams) + 1; //umpire te bekijken in volgende iteratie
        int rPlus;
        if (umpire + 1 == problem.nTeams) {
            rPlus = round + 1;
        } else {
            rPlus = round;
        }
        List<Integer> A = problem.getFeasibleAllocations(umpire, round);
        for (Integer a : A) {
                if (!problem.canBePruned(a)) {
                    //path[umpire][round] = a;
                    printPath(path);
                    if (round < problem.nTeams - 1) {
                        branchAndBound(path, uPlus, rPlus);
                    } else {
                        int totalDistance = calculateTotalDistance(path);//Hier moet local search worden gedaan
                        if (totalDistance < bestDistance) {
                            bestDistance = totalDistance;
                            for (int i = 0; i < problem.nTeams; i++) {
                            }
                        }
                    }
                    path[umpire][round] = -1;
                }
            }
            printPath(path);
        }




 */
    private int calculateTotalDistance(int[][] path) {
        int totalDistance = 0;
        for (int i = 0; i < problem.nTeams; i++) {
            for (int j = 0; j < problem.nTeams - 1; j++) {
                totalDistance += problem.dist[path[i][j]][path[i][j + 1]];
            }
        }
        return totalDistance;
    }



    public String getBestSolution() {
        return Arrays.deepToString(bestSolution);
    }


    public int getBestDistance() {
        return bestDistance;
    }





    private void printPath(int[][] path) {
        System.out.println("-------------------------------------------------------------------------------------");
        System.out.println(Arrays.deepToString(path));
    }
}