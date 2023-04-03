import java.io.BufferedReader;
import java.io.FileReader;
import com.opencsv.CSVReader;

import java.io.IOException;
import java.util.*;


public class NFLOptimizer {
    public static final double OPPONENT_RANK = 0.34;
    public static final double WEIGHT = 0.92;
    public static final int WEEK = 17;
    public static final double CUTOFF = 165;
    public static final double MINI_CUTOFF = 21;

    public static void main(String[] args) throws IOException {
        // Counts number of lines in file
        BufferedReader pseudoReader = new BufferedReader(new FileReader("FanDuel-NFL-2023 ET-01 ET-08 ET-85546-players-list.csv"));
        int lines = 0;
        while (pseudoReader.readLine() != null) lines++;
        pseudoReader.close();
        // Creates a 2D array with all the necessary data.
        String[][] allStats = new String[0][];
        lines--;
        try {
            CSVReader reader = new CSVReader(new FileReader("FanDuel-NFL-2023 ET-01 ET-08 ET-85546-players-list.csv"));
            String[] nextLine;
            allStats = new String[lines][];
            for(int i = 0; i < lines; i++){
                nextLine = reader.readNext();
                allStats[i] = nextLine;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        BufferedReader seudoReader = new BufferedReader(new FileReader("FantasyPros_Fantasy_Football_2022_Stength_Of_Schedule.csv"));
        int otherLines = 0;
        while (seudoReader.readLine() != null) otherLines++;
        seudoReader.close();
        // Creates a 2D array with all the necessary data.
        String[][] opponents = new String[0][];
        otherLines--;
        try {
            CSVReader reader = new CSVReader(new FileReader("FantasyPros_Fantasy_Football_2022_Stength_Of_Schedule.csv"));
            String[] nextLine;
            opponents = new String[otherLines][];
            for(int i = 0; i < otherLines; i++){
                nextLine = reader.readNext();
                opponents[i] = nextLine;
            }
        } catch (Exception e) {
            System.out.println(e);
        }
        String player = "Nickname";
        String[] players = new String[allStats.length];
        for(int i = 0; i < allStats.length; i++){
            players[i] = allStats[i][3];
        }
        program(allStats, opponents, players, player);
        //bestTeam(greedyStats);
    }

    // Runs the program, took it out of main so that I could use recursion.
    private static void program(String[][] allStats, String[][] opponents, String[] players, String player){
        removePlayer(players, player);
        int playerCount = 0;
        for(int i = 0; i < allStats.length; i++){
            if(!Objects.equals(players[i], "-1")) playerCount++;
        }
        String [][] original = new String[playerCount][];
        String [][] newAllStats = new String[playerCount][];
        int index = 0;
        for(int i = 0; i < allStats.length; i++)
            if(!Objects.equals(players[i], "-1")){
                newAllStats[index] = allStats[i].clone();
                original[index] = allStats[i].clone();
                index++;
            }
        weakTeam(newAllStats, player);
        pointAdjustment(newAllStats, opponents, original.length);
        playedAdjustment(newAllStats, opponents, original.length);
        int tempCount = fantasyRatio(newAllStats, original.length);

        String[][] greedyStats = new String[tempCount][];
        cleanedArray(greedyStats, original.length, newAllStats);

        String[][] bestQB = bestQB(greedyStats, newAllStats);
        String[][] bestRB = bestRB(greedyStats, newAllStats);
        String[][] bestWR = bestWR(greedyStats, newAllStats);
        String[][] bestTE = bestTE(greedyStats, newAllStats);
        String[][] bestFlex = bestFlex(newAllStats);
        String[][] bestD = bestD(original, newAllStats);
        String[][] bestTeamOptions = bestTeam(bestQB, bestD, bestWR,bestRB,bestTE,newAllStats, bestFlex);
        cooperSort(bestTeamOptions);
        printResults(bestTeamOptions, newAllStats, opponents, players, player);
    }

    // Prints the results of the program, also provides the opportunity to eliminate a player manually if they seem
    // like a bad pick based on news.
    private static void printResults(String[][]results, String[][] allStats, String[][] opponents, String[] players, String player){
        System.out.println("\r~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        System.out.printf("%20s: %20s - %5f%n", "Quarterback", results[0][0], playerPointAdjustment("QB", opponents, results[0][0], allStats));
        System.out.printf("%20s: %20s - %5f%n", "1st Running Back", results[0][1], playerPointAdjustment("RB", opponents, results[0][1], allStats));
        System.out.printf("%20s: %20s - %5f%n", "2nd Running Back", results[0][2], playerPointAdjustment("RB", opponents, results[0][2], allStats));
        System.out.printf("%20s: %20s - %5f%n", "1st Wide Receiver", results[0][3], playerPointAdjustment("WR", opponents, results[0][3], allStats));
        System.out.printf("%20s: %20s - %5f%n", "2nd Wide Receiver", results[0][4], playerPointAdjustment("WR", opponents, results[0][4], allStats));
        System.out.printf("%20s: %20s - %5f%n", "3rd Wide Receiver", results[0][5], playerPointAdjustment("WR", opponents, results[0][5], allStats));
        System.out.printf("%20s: %20s - %5f%n", "Tight End", results[0][6], playerPointAdjustment("TE", opponents, results[0][6], allStats));
        System.out.printf("%20s: %20s - %5f%n", "Flex", results[0][7], playerPointAdjustment("FLEX", opponents, results[0][7], allStats));
        System.out.println(results[0][8]);
        System.out.printf("%20s: %20s - %5f%n", "Defense", results[0][8], playerPointAdjustment("D", opponents, results[0][8], allStats));
        System.out.println("*******************************************");
        System.out.printf("%20s: %20.5f%n", "Projected Score", Double.parseDouble(results[0][9]));
        System.out.printf("%20s: %,20d%n", "Projected Cost", Integer.parseInt(results[0][10]));
        System.out.println("~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");
        Scanner key = new Scanner(System.in);
        System.out.print("Would you like to see a different line-up (Please input Y or y for yes)? ");
        String input = key.next();
        if(Objects.equals(input , "y") || Objects.equals(input , "Y")){
            System.out.print("What player would you like to eliminate (Enter number 0-8)? ");
            int num = key.nextInt();
            player = results[0][num];
            //Kind of broken, if I go back to fix, remember "All Stats" in this method is not the original.
            program(allStats, opponents, players, player);
        }
    }

    private static void removePlayer(String[] players, String player){
        for(int i = 0; i < players.length; i++){
            if(Objects.equals(players[i], player)) players[i] = "-1";
        }
    }

    // Given a player's name, return their projected points.
    private static double playerPoints(String player, String[][] allStats){
        for (int i = 0; i < allStats.length; i++) {
            if (Objects.equals(player, allStats[i][3])) {
                return Double.parseDouble(allStats[i][5]);
            }
        }
        return -1;
    }

    // Eliminates all players who are manually eliminated or else injured.
    private static void weakTeam(String[][] allStats, String player) {
        int tempCount = 0;
        for (int i = 0; i < allStats.length; i++) {
            if (!Objects.equals(allStats[i][11], "")) {
                allStats[i][5] = "-1";
                tempCount++;
            }
        }
    }

    // Adjusted the projected points based on the strength of the opposing team
    private static void pointAdjustment(String[][] allStats, String[][] opponents, int lines) {
        double star = 0;
        for (int i = 0; i < allStats.length; i++) {
            if (Objects.equals(allStats[i][5], "")) {
                allStats[i][5] = String.valueOf(0);
                allStats[i][2] = String.valueOf(0);
                allStats[i][4] = "false";
            }
            for (int j = 0; j < opponents.length; j++) {
                if (Objects.equals(allStats[i][9], opponents[j][0])) {
                    if (Objects.equals(allStats[i][1], "QB") && !Objects.equals(allStats[i][4], "false")) {
                        Scanner lineString = new Scanner(opponents[j][1]);
                        for (int k = 0; k < 3; k++) lineString.next();
                        star = lineString.nextInt();
                        star = ((star - 3) * OPPONENT_RANK) + 1;
                        allStats[i][5] = Double.toString(star * Double.parseDouble(allStats[i][5]));
                    } else if (Objects.equals(allStats[i][1], "RB") && !Objects.equals(allStats[i][4], "false")) {
                        Scanner lineString = new Scanner(opponents[j][2]);
                        for (int k = 0; k < 3; k++) lineString.next();
                        star = lineString.nextInt();
                        star = ((star - 3) * OPPONENT_RANK) + 1;
                        allStats[i][5] = Double.toString(star * Double.parseDouble(allStats[i][5]));
                    } else if (Objects.equals(allStats[i][1], "WR") && !Objects.equals(allStats[i][4], "false")) {
                        Scanner lineString = new Scanner(opponents[j][3]);
                        for (int k = 0; k < 3; k++) lineString.next();
                        star = lineString.nextInt();
                        star = ((star - 3) * OPPONENT_RANK) + 1;
                        allStats[i][5] = Double.toString(star * Double.parseDouble(allStats[i][5]));
                    } else if (Objects.equals(allStats[i][1], "TE") && !Objects.equals(allStats[i][4], "false")) {
                        Scanner lineString = new Scanner(opponents[j][4]);
                        for (int k = 0; k < 3; k++) lineString.next();
                        star = lineString.nextInt();
                        star = ((star - 3) * OPPONENT_RANK) + 1;
                        allStats[i][5] = Double.toString(star * Double.parseDouble(allStats[i][5]));
                    } else if (Objects.equals(allStats[i][1], "D") && !Objects.equals(allStats[i][4], "false")) {
                        Scanner lineString = new Scanner(opponents[j][6]);
                        for (int k = 0; k < 3; k++) lineString.next();
                        star = lineString.nextInt();
                        star = ((star - 3) * OPPONENT_RANK) + 1;
                        allStats[i][5] = Double.toString(star * Double.parseDouble(allStats[i][5]));
                    }
                }
            }
        }
    }


    //Adjust the points of one player, used for printing purposes
    private static double playerPointAdjustment(String role, String[][] opponents, String name, String[][] allStats) {
        double star = 0;
        int index = 0;
        for (int i = 0; i < allStats.length; i++) {
            if (Objects.equals(allStats[i][3], name) && (Objects.equals(allStats[i][1], role) || (Objects.equals(allStats[i][1], "FLEX")))) {
                index = i;
                break;
            }
        }
        return Double.parseDouble(allStats[index][5]);
    }

    // Adjust the projected points based on the history of how often the player plays.
    private static void playedAdjustment(String[][] allStats, String[][] opponents, int lines) {
        double x = 0;
        for (int i = 1; i < allStats.length; i++) {
            if (Objects.equals(allStats[i][6], "")) {
                allStats[i][6] = String.valueOf(1);
            }
            if (Double.parseDouble(allStats[i][6]) <= (WEEK-1)) {
                x = Double.parseDouble(allStats[i][6]) / (WEEK-1);
                x = Math.sqrt(x);
                x *= (1.0/3);
                x *= Double.parseDouble(allStats[i][5]);
                x += (((WEEK-1) * (2.0/3))) * 1.75;
                allStats[i][5] = Double.toString(x);
            } else{
                x = 1.75;
                x = Math.sqrt(x);
                x *= (1.0/3);
                x *= Double.parseDouble(allStats[i][5]);
                x += (((WEEK-1) * (2.0/3))) * 1.75;
                allStats[i][5] = Double.toString(x);
            }

        }
    }

    private static int fantasyRatio(String[][] allStats, int lines) {
        double[] position = new double[5];
        int[] count = new int[5];
        int tempCount = 0;
        for (int i = 0; i < allStats.length; i++) {
            if (Objects.equals(allStats[i][5], "")) {
                allStats[i][2] = String.valueOf(0);
                allStats[i][4] = "false";
            }else if (Double.parseDouble(allStats[i][5]) <= MINI_CUTOFF) {
                allStats[i][2] = String.valueOf(0);
                allStats[i][4] = "false";
            } else
                allStats[i][2] = String.valueOf((Double.parseDouble(allStats[i][5]) / Double.parseDouble(allStats[i][7])));
            if (Objects.equals(allStats[i][1], "QB") && !Objects.equals(allStats[i][4], "false")) {
                position[0] += Double.parseDouble(allStats[i][2]);
                count[0]++;
            } else if (Objects.equals(allStats[i][1], "RB") && !Objects.equals(allStats[i][4], "false")) {
                position[1] += Double.parseDouble(allStats[i][2]);
                count[1]++;
            } else if (Objects.equals(allStats[i][1], "WR") && !Objects.equals(allStats[i][4], "false")) {
                position[2] += Double.parseDouble(allStats[i][2]);
                count[2]++;
            } else if (Objects.equals(allStats[i][1], "TE") && !Objects.equals(allStats[i][4], "false")) {
                position[3] += Double.parseDouble(allStats[i][2]);
                count[3]++;
            } else if (Objects.equals(allStats[i][1], "D") && !Objects.equals(allStats[i][4], "false")) {
                position[4] += Double.parseDouble(allStats[i][2]);
                count[4]++;
            }
        }
        for (int i = 0; i < position.length; i++) {
            position[i] = (position[i] / count[i]) * WEIGHT;
        }
        double temp;
        // Marks the bottom 80% of players for elimination.
        for (int i = 0; i < allStats.length; i++) {
            temp = Double.parseDouble(allStats[i][2]);
            if (Objects.equals(allStats[i][1], "QB") && position[0] < temp) {
                allStats[i][4] = "true";
                tempCount++;
            } else if (Objects.equals(allStats[i][1], "RB") && position[1] < temp) {
                allStats[i][4] = "true";
                tempCount++;
            } else if (Objects.equals(allStats[i][1], "WR") && position[2] < temp) {
                allStats[i][4] = "true";
                tempCount++;
            } else if (Objects.equals(allStats[i][1], "TE") && position[3] < temp) {
                allStats[i][4] = "true";
                tempCount++;
            } else if (Objects.equals(allStats[i][1], "D") && position[4] < temp) {
                allStats[i][4] = "true";
                tempCount++;
            }
        }
        return tempCount;
    }

    private static void cleanedArray(String[][] greedyStats, int lines, String[][] allStats) {
        int lastCount = 0;
        for (int i = 0; i < allStats.length; i++) {
            if (Objects.equals(allStats[i][4], "true")) {
                greedyStats[lastCount] = allStats[i];
                lastCount++;
            }
        }
    }

    private static int numPosition(String[][] greedyStats, String position) {
        int result = 0;
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], position)) {
                result++;
            }
        }
        return result;
    }

    private static String[][] removeDuplicates(String[][] array, int length) {
        int result = 0;
        for (int i = 0; i < array.length; i++) {
            if (!Objects.equals(array[i][length+2], "false")) {
                result++;
            }
        }
        String[][] newArray = new String[result][];
        int index = 0;
        for (int i = 0; i < array.length; i++) {
            if (!Objects.equals(array[i][length+2], "false")) {
                newArray[index] = array[i];
                index++;
            }
        }
        int newResult = 0;
        for (int i = 0; i < newArray.length; i++) {
            for(int j = 0; j < newArray.length && j!= i; j++){
                if(Objects.equals(newArray[i][length], newArray[j][length])){
                    if(Integer.parseInt(newArray[i][length+1]) <= Integer.parseInt(newArray[j][length+1])){
                        newArray[j][length+2] = "false";
                    } else newArray[i][length+2] = "false";
                }
            }
        }
        for (int i = 0; i < array.length; i++) {
            if (!Objects.equals(array[i][length+2], "false")) {
                newResult++;
            }
        }
        String[][] newNewArray = new String[newResult][];
        int newIndex = 0;
        for (int i = 0; i < newArray.length; i++) {
            if (!Objects.equals(newArray[i][length+2], "false")) {
                newNewArray[newIndex] = newArray[i];
                newIndex++;
            }
        }
        return newNewArray;
    }

    // Removes elements from the given array with a value in the given column below the average value
    // and returns the cleaned array
    private static String[][] finalOpti(String[][] array, int column) {
        // Calculate the average value of the given column
        double sum = 0;
        for (int i = 0; i < array.length; i++) {
            sum += Double.parseDouble(array[i][column]);
        }
        double average = sum / array.length;
        average *= WEIGHT;

        // Mark elements with a value below the average as duplicates
        for (int i = 0; i < array.length; i++) {
            if (Double.parseDouble(array[i][column]) <= average) {
                array[i][column+2] = "false";
            }
        }

        // Remove the duplicates and return the cleaned array
        return removeDuplicates(array, column);
    }


    private static String[][] bestWR(String[][] greedyStats, String[][] allStats) {
        int result = 0;
        String[][] wideReceiver = new String[numPosition(greedyStats, "WR")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], "WR")) {
                wideReceiver[result] = greedyStats[i];
                result++;
            }
        }
        int tempPoints = 0;
        int tempCost = 0;
        int index = 0;
        String[][] bestWR = new String[(wideReceiver.length*(wideReceiver.length-1)*(wideReceiver.length-2))][];
        for(int i = 0; i < wideReceiver.length; i++){
            tempPoints += Double.parseDouble(wideReceiver[i][5]);
            tempCost += Integer.parseInt(wideReceiver[i][7]);
            for (int j = 0; j < wideReceiver.length; j++) {
                if (j != i) {
                    tempPoints += Double.parseDouble(wideReceiver[j][5]);
                    tempCost += Integer.parseInt(wideReceiver[j][7]);
                    for (int k = 0; k < wideReceiver.length; k++) {
                        if (k != i && k != j) {
                            tempPoints += Double.parseDouble(wideReceiver[k][5]);
                            tempCost += Integer.parseInt(wideReceiver[k][7]);
                            bestWR[index] = new String[]{wideReceiver[i][3], wideReceiver[j][3], wideReceiver[k][3], Double.toString(tempPoints), Integer.toString(tempCost), "true"};
                            index++;
                            tempPoints -= Double.parseDouble(wideReceiver[k][5]);
                            tempCost -= Integer.parseInt(wideReceiver[k][7]);
                        }
                    }
                    tempPoints -= Double.parseDouble(wideReceiver[j][5]);
                    tempCost -= Integer.parseInt(wideReceiver[j][7]);
                }
            }
            tempPoints -= Double.parseDouble(wideReceiver[i][5]);
            tempCost -= Integer.parseInt(wideReceiver[i][7]);
        }
        int numWR = numPosition(allStats, "WR");
        System.out.printf("Wide Receiver options: %,d%n", ((numWR*(numWR-1)*(numWR-2))));
        int factor = (int)(((double) ((numWR*(numWR-1)*(numWR-2))+2)/bestWR.length));
        System.out.printf("Wide Receiver options reduced by a factor of %,d to %,d after greedy search.%n",  factor, bestWR.length);
        String[][] finalBestWR = finalOpti(bestWR, 3);
        factor = (int)(((double) bestWR.length/finalBestWR.length));
        System.out.printf("Wide Receiver options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalBestWR.length);
        return finalBestWR;
    }

    private static String[][] bestTE(String[][] greedyStats, String[][] allStats) {
        int result = 0;
        String[][] tightEnd = new String[numPosition(greedyStats, "TE")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], "TE")) {
                tightEnd[result] = new String[]{greedyStats[i][3], greedyStats[i][5], greedyStats[i][7], "true"};
                result++;
            }
        }
        int numTE = numPosition(allStats, "RB");
        System.out.printf("Tight End options: %,d%n", numTE);
        int factor = (int)((double) numTE/tightEnd.length);
        System.out.printf("Tight End options reduced by a factor of %,d to %,d after greedy search.%n",  factor, tightEnd.length);
        String[][] finalBestTE = finalOpti(tightEnd, 1);
        factor = (int)(((double) tightEnd.length/finalBestTE.length));
        System.out.printf("Tight End options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalBestTE.length);
        return finalBestTE;
    }

    private static String[][] bestRB(String[][] greedyStats, String[][] allStats) {
        int result = 0;
        String[][] runningBack = new String[numPosition(greedyStats, "RB")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], "RB")) {
                runningBack[result] = greedyStats[i];
                result++;
            }
        }
        int tempPoints = 0;
        int tempCost = 0;
        int index = 0;
        String[][] bestRB = new String[(runningBack.length*(runningBack.length-1))][];
        for(int i = 0; i < runningBack.length; i++){
            tempPoints += Double.parseDouble(runningBack[i][5]);
            tempCost += Integer.parseInt(runningBack[i][7]);
            for (int j = 0; j < runningBack.length; j++) {
                if (j != i) {
                    tempPoints += Double.parseDouble(runningBack[j][5]);
                    tempCost += Integer.parseInt(runningBack[j][7]);
                    bestRB[index] = new String[]{runningBack[i][3], runningBack[j][3], Double.toString(tempPoints), Integer.toString(tempCost), "true"};
                    index++;
                    tempPoints -= Double.parseDouble(runningBack[j][5]);
                    tempCost -= Integer.parseInt(runningBack[j][7]);
                }
            }
            tempPoints -= Double.parseDouble(runningBack[i][5]);
            tempCost -= Integer.parseInt(runningBack[i][7]);
        }
        int numRB = numPosition(allStats, "RB");
        System.out.printf("Running Back options: %,d%n", (numRB*(numRB-1)));
        int factor = (int)((double) ((numRB*(numRB-1))/bestRB.length));
        System.out.printf("Running Back options reduced by a factor of %,d to %,d after greedy search.%n",  factor, bestRB.length);
        String[][] finalBestRB = finalOpti(bestRB, 2);
        factor = (int)(((double) bestRB.length/finalBestRB.length));
        System.out.printf("Running Back options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalBestRB.length);
        return finalBestRB;
    }

    private static String[][] bestFlex(String[][] greedyStats) {
        int result = 0;
        String[][] flex = new String[numPosition(greedyStats, "TE") + numPosition(greedyStats, "WR")+ numPosition(greedyStats, "RB")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (!Objects.equals(greedyStats[i][1], "QB") && !Objects.equals(greedyStats[i][1], "D")) {
                flex[result] = new String[]{greedyStats[i][3], greedyStats[i][5], greedyStats[i][7], "true"};
                result++;
            }
        }
        String[][] finalFlex = finalOpti(flex, 1);
        System.out.printf("Flex options: %,d%n", flex.length);
        System.out.printf("Flex options reduced by a factor of %,d to %,d after pruning.%n", (flex.length/finalFlex.length), finalFlex.length);
        return finalFlex;
    }

    private static String[][] bestQB(String[][] greedyStats, String[][] allStats) {
        int result = 0;
        String[][] quarterBack = new String[numPosition(greedyStats, "QB")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], "QB")) {
                quarterBack[result] = new String[]{greedyStats[i][3], greedyStats[i][5], greedyStats[i][7], "true"};
                result++;
            }
        }
        int numQB = numPosition(allStats, "RB");
        System.out.printf("Quarterback options: %,d%n", numQB);
        int factor = (int)((double) numQB/quarterBack.length);
        System.out.printf("Quarterback options reduced by a factor of %,d to %,d after greedy search.%n",  factor, quarterBack.length);
        //String[][] finalBestQB = finalOpti(quarterBack, 1);
        //factor = (int)(((double) quarterBack.length/finalBestQB.length));
        //System.out.printf("Quarterback options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalBestQB.length);
        //return finalBestQB;
        return  quarterBack;
    }

    private static String[][] bestD(String[][] greedyStats, String[][] allStats) {
        int result = 0;
        String[][] defense = new String[numPosition(greedyStats, "D")][];
        for (int i = 0; i < greedyStats.length; i++) {
            if (Objects.equals(greedyStats[i][1], "D")) {
                defense[result] = new String[]{greedyStats[i][3], greedyStats[i][5], greedyStats[i][7], "true"};
                result++;
            }
        }
        int numQB = numPosition(allStats, "D");
        System.out.printf("Defense options: %,d%n", numQB);
        int factor = (int)((double) numQB/defense.length);
        System.out.printf("Defense options reduced by a factor of %,d to %,d after greedy search.%n",  factor, defense.length);
        String[][] finalBestQB = removeDuplicates(defense, 1);
        factor = (int)(((double) defense.length/finalBestQB.length));
        System.out.printf("Defense options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalBestQB.length);
        return finalBestQB;
    }

    private static int cooperSort(String[][] nums){
        System.out.print("Processing results");
        String[] temp = nums[0];
        int count = 0;
        boolean sorted = false;
        while(!sorted) {
            for (int i = 0; i < nums.length / 2; i++) {
                if(i%5 == 0) System.out.print("\rProcessing results.");
                if (Double.parseDouble(nums[i][9]) < Double.parseDouble(nums[nums.length - 1 - i][9])) {
                    temp = nums[i];
                    nums[i] = nums[nums.length - 1 - i];
                    nums[nums.length - 1 - i] = temp;
                }
            }
            sorted = sortCheck(nums);
            count++;
            if(!sorted) {
                for (int i = 0; i < nums.length - 1; i += 2) {
                    if(i%5 == 0) System.out.print("\rProcessing results..");
                    if (Double.parseDouble(nums[i][9]) < Double.parseDouble(nums[i + 1][9])) {
                        temp = nums[i];
                        nums[i] = nums[i + 1];
                        nums[i + 1] = temp;
                    }
                }
            }
            sorted = sortCheck(nums);
            count++;
            if(!sorted) {
                for (int i = 1; i < nums.length - 1; i += 2) {
                    if(i%5 == 0) System.out.print("\rProcessing results...");
                    if (Double.parseDouble(nums[i][9]) < Double.parseDouble(nums[i + 1][9])) {
                        temp = nums[i];
                        nums[i] = nums[i + 1];
                        nums[i + 1] = temp;
                    }
                }
            }
        }
        return count;
    }

    private static boolean sortCheck(String[][] nums){
        for(int i = 0; i < nums.length - 1; i++){
            if(Double.parseDouble(nums[i][9]) < Double.parseDouble(nums[i + 1][9])) return false;
        }
        return true;
    }

    private static String[][] bestTeam(String[][] bestQB, String[][] bestD, String[][] bestWR, String[][] bestRB, String[][] bestTE, String[][] allStats, String[][] bestFlex) {
        int result = 0;
        double tempPoints = 0;
        int tempCost = 0;
        int index = 0;
        int big = (bestFlex.length-6) * bestQB.length* bestTE.length*bestWR.length*bestRB.length*bestD.length;
        System.out.printf("Total options: %,d%n", big);
        for(int i = 0; i < bestQB.length; i++){
            for (int j = 0; j < bestWR.length; j++) {
                for (int k = 0; k < bestRB.length; k++) {
                    for (int l = 0; l < bestTE.length; l++) {
                        for(int n = 0; n < bestFlex.length && !Objects.equals(bestFlex[n][0], bestRB[k][0]) && !Objects.equals(bestFlex[n][0], bestRB[k][1]) && !Objects.equals(bestFlex[n][0], bestWR[j][0]) && !Objects.equals(bestFlex[n][0], bestWR[j][1]) && !Objects.equals(bestFlex[n][0], bestWR[j][2]) && !Objects.equals(bestFlex[n][0], bestTE[l][0]); n++){
                            for (int m = 0; m < bestD.length; m++) {
                                tempPoints = Double.parseDouble(bestQB[i][1])+Double.parseDouble(bestWR[j][3])+Double.parseDouble(bestRB[k][2])+Double.parseDouble(bestTE[l][1])+Double.parseDouble(bestFlex[n][1])+Double.parseDouble(bestD[m][1]);
                                tempCost = Integer.parseInt(bestQB[i][2])+Integer.parseInt(bestWR[j][4])+Integer.parseInt(bestRB[k][3])+Integer.parseInt(bestTE[l][2])+Integer.parseInt(bestFlex[n][2])+Integer.parseInt(bestD[m][2]);
                                if (tempCost > 60000){
                                    tempPoints = 0;
                                    tempCost = 0;
                                    break;
                                }
                                if (tempPoints <= CUTOFF){
                                    tempPoints = 0;
                                    tempCost = 0;
                                    break;
                                }
                                result++;
                                tempPoints = 0;
                                tempCost = 0;
                            }
                        }
                    }
                }
            }
        }
        String[][] teamOptions = new String[result][];
        for(int i = 0; i < bestQB.length; i++){
            for (int j = 0; j < bestWR.length; j++) {
                for (int k = 0; k < bestRB.length; k++) {
                    for (int l = 0; l < bestTE.length; l++) {
                        for(int n = 0; n < bestFlex.length && !Objects.equals(bestFlex[n][0], bestRB[k][0]) && !Objects.equals(bestFlex[n][0], bestRB[k][1]) && !Objects.equals(bestFlex[n][0], bestWR[j][0]) && !Objects.equals(bestFlex[n][0], bestWR[j][1]) && !Objects.equals(bestFlex[n][0], bestWR[j][2]) && !Objects.equals(bestFlex[n][0], bestTE[l][0]); n++){
                            for (int m = 0; m < bestD.length; m++) {
                                tempPoints = Double.parseDouble(bestQB[i][1])+Double.parseDouble(bestWR[j][3])+Double.parseDouble(bestRB[k][2])+Double.parseDouble(bestTE[l][1])+Double.parseDouble(bestFlex[n][1])+Double.parseDouble(bestD[m][1]);
                                tempCost = Integer.parseInt(bestQB[i][2])+Integer.parseInt(bestWR[j][4])+Integer.parseInt(bestRB[k][3])+Integer.parseInt(bestTE[l][2])+Integer.parseInt(bestFlex[n][2])+Integer.parseInt(bestD[m][2]);
                                if (tempCost > 60000){
                                    tempPoints = 0;
                                    tempCost = 0;
                                    break;
                                }
                                if (tempPoints <= CUTOFF){
                                    tempPoints = 0;
                                    tempCost = 0;
                                    break;
                                }
                                teamOptions[index] = new String[]{bestQB[i][0], bestRB[k][0], bestRB[k][1], bestWR[j][0], bestWR[j][1], bestWR[j][2], bestTE[l][0], bestFlex[n][0], bestD[m][0], Double.toString(tempPoints), Integer.toString(tempCost), "true"};
                                index++;
                                tempPoints = 0;
                                tempCost = 0;
                            }
                        }
                    }
                }
            }
        }
        int factor = (int)(((double) big/teamOptions.length));
        System.out.printf("Total options reduced by a factor of %,d to %,d after greedy search.%n",  factor, teamOptions.length);
        String[][] finalTeamOptions = finalOpti(teamOptions, 9);
        factor = (int)(((double) teamOptions.length/finalTeamOptions.length));
        System.out.printf("Total options reduced by a factor of %,d to %,d after removal of duplicates and lessers.%n", factor, finalTeamOptions.length);
        return finalTeamOptions;
    }
}
