package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Problem {
	
	private int nCells;
	private int nPeriods;
	private int nTypes;
	
	private int[] typeTasks;	// Tasks each customer type can do
	private float[][][][] costs;
	private int[] tasksToDo;
	private int[][][] customers;
	
	
	public Problem(String inputFile) {
		BufferedReader in;
		
		try {
			in = new BufferedReader(new FileReader(inputFile));
			
			// Read and parse first line, containing the cardinalities of the problem
			String[] line = in.readLine().split(" ");
			nCells = Integer.parseInt(line[0]);
			nPeriods = Integer.parseInt(line[1]);
			nTypes = Integer.parseInt(line[2]);
			
			typeTasks = new int[nTypes];
			costs = new float[nCells][nCells][nTypes][nPeriods];
			tasksToDo = new int[nCells];
			customers = new int[nCells][nTypes][nPeriods];
			
			in.readLine();
			
			line = in.readLine().split(" ");
			
			// Read and parse the second line, containing the tasks each user type can do
			for (int i = 0 ; i < nTypes ; i++)
				typeTasks[i] = Integer.parseInt(line[i]);
			
			in.readLine();
			
			// Read and parse the costs matrices
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Integer.parseInt(line[0]);	// Type
				int t = Integer.parseInt(line[1]);  // Period
				
				for (int i = 0; i < nCells ; i++) {
					line = in.readLine().split(" ");
					for (int j = 0 ; j < nCells ; j++) {
						costs[i][j][m][t] = Float.parseFloat(line[j]);
					}
				}
			}
			
			in.readLine();
			
			// Read and parse the tasks to do
			line = in.readLine().split(" ");
			for (int i = 0 ; i < nCells ; i++)
				tasksToDo[i] = Integer.parseInt(line[i]);
			
			in.readLine();
			
			// Read and parse the available users
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Integer.parseInt(line[0]);
				int t = Integer.parseInt(line[1]);
				
				line = in.readLine().split(" ");
				
				for (int i = 0 ; i < nCells ; i++) {
					customers[i][m][t] = Integer.parseInt(line[i]);
				}
			}
			
			in.close();			
		} catch (IOException ioe) {
			System.err.println("Unable to read file: " + inputFile);
			ioe.printStackTrace();
			System.exit(1);
		}
	}
}
