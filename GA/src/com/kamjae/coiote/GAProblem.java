package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class GAProblem {
	private int nCells;
	private byte nPeriods;
	private byte nTypes;

	private byte[] typeTasks;	// Tasks each customer type can do
	private byte[] tasksToDo;
	private byte[][][] customers;
	private float[][][][] costs;

	// DEBUG
	private int emptyIMT;
	private int emptyJ;

	public GAProblem(String inputFile) {
		BufferedReader in;

		try {
			in = new BufferedReader(new FileReader(inputFile));

			// Read and parse first line, containing the cardinalities of the problem
			String[] line = in.readLine().split(" ");
			nCells = Integer.parseInt(line[0]);
			nPeriods = Byte.parseByte(line[1]);
			nTypes = Byte.parseByte(line[2]);

			typeTasks = new byte[nTypes];
			tasksToDo = new byte[nCells];
			customers = new byte[nCells][nTypes][nPeriods];
			costs = new float[nCells][nCells][nTypes][nPeriods];

			in.readLine();

			line = in.readLine().split(" ");

			// Read and parse the second line, containing the tasks each user type can do
			for (int i = 0 ; i < nTypes ; i++)
				typeTasks[i] = Byte.parseByte(line[i]);

			in.readLine();

			// Read and parse the costs matrices
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				byte m = Byte.parseByte(line[0]);	// Type
				byte t = Byte.parseByte(line[1]);  // Period

				for (int i = 0; i < nCells ; i++) {
					line = in.readLine().split(" ");
					for (int j = 0 ; j < nCells ; j++) {
						costs[i][j][m][t] = Float.parseFloat(line[j]);
					}
				}
			}

			in.readLine();

			emptyIMT = 0;
			emptyJ = 0;

			// Read and parse the tasks to do
			line = in.readLine().split(" ");
			for (int i = 0 ; i < nCells ; i++){
				tasksToDo[i] = Byte.parseByte(line[i]);
				if(tasksToDo[i] == 0){
					emptyJ++;
				}
			}

			in.readLine();

			// Read and parse the available users
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Byte.parseByte(line[0]);
				int t = Byte.parseByte(line[1]);

				line = in.readLine().split(" ");

				for (int i = 0 ; i < nCells ; i++) {
					customers[i][m][t] = Byte.parseByte(line[i]);
					if(customers[i][m][t] == 0){
						emptyIMT++;
					}
				}
			}

			in.close();			
			System.out.println("EMPTY J = " + emptyJ);
			System.out.println("EMPTY IMT = " + emptyIMT);
		} catch (IOException ioe) {
			System.err.println("Unable to read file: " + inputFile);
			ioe.printStackTrace();
			System.exit(1);
		}
	}

	public GASolution solveProblem(){
		// solveProblem is the only function called by Main
		// Inside we do all computation
		long start = System.nanoTime();

		GA coiote = new GA(  
                1, //population has N chromosomes
                0.7, //crossover probability
                30, //random selection chance % (regardless of fitness)
                1, //max generations
                0, //num prelim runs (to build good breeding stock for final/full run)
                0, //max generations per prelim run
                0.05, //chromosome mutation prob.
                GACrossover.ctOnePoint, //crossover type
                false,
                nCells,
                nTypes,
                nPeriods,
                typeTasks,
                tasksToDo,
                customers,
                costs);
        coiote.evolve();

		return coiote.chromosomes[coiote.bestFitnessChromIndex];
	}
}
