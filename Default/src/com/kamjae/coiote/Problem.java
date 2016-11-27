package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.Random;

public class Problem {
	
	private Random random;

	private int nCells;
	private int nPeriods;
	private int nTypes;

	private int[] typeTasks;	// Tasks each customer type can do
	private float[][][][] costs;
	private int[] tasksToDo;
	private int[][][] customers;

	public class Solution {
		private int[][][][] solMatrix;
		private int[][][] unassignedUsers;
		private int[] fullfilled;
		private int totalCustomers;
		private float totalCost;
		private float elapsedMillis;

		private Solution() {
			solMatrix = new int[nCells][nCells][nTypes][nPeriods];
			unassignedUsers = new int[nCells][nTypes][nPeriods];
			fullfilled = new int[nCells];
			totalCustomers = 0;
			totalCost = 0;
			elapsedMillis = 0;

			// The order is whacky, but with this we can init everything in one loop.		
			for (int i = 0  ; i < nCells ; i++) {
				fullfilled[i] = 0;
				for (int m = 0 ; m < nTypes ; m++) {
					for (int t = 0 ; t < nPeriods ; t++) {
						unassignedUsers[i][m][t] = customers[i][m][t];
						totalCustomers += customers[i][m][t];
						for (int j = 0 ; j < nCells ; j++) {
							solMatrix[i][j][m][t] = 0;
						}
					}
				}
			}
		}

		public float getTotalCost() {
			return totalCost;
		}
		
		public float getElapsedMillis() {
			return elapsedMillis;
		}

		@Override
		/**
		 * Outputs the solution for non-zero values of x[i][j][m][t].
		 * The result is ordered by destination cell.
		 */
		public String toString(){
			String output = "";
			for (int j = 0 ; j < nCells ; j++) {
				for (int i = 0 ; i < nCells ; i++) {
					for (int m = 0 ; m < nTypes ; m++) {
						for (int t = 0 ; t < nPeriods ; t++) {
							if (solMatrix[i][j][m][t] != 0) {
								output += String.format("(%d, %d, %d, %d) : %d\n", 
										i, j, m, t, solMatrix[i][j][m][t]);
							}
						}
					}
				}
			}
			return output;
		}
	}

	public enum Feasibility {
		FEASIBLE,
		UNF_DEMAND,
		UNF_CUSTOMERS
	}

	public Problem(String inputFile) {
		BufferedReader in;

		try {
			int totalTasks = 0;
			int totalUsers = 0;

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
			for (int i = 0 ; i < nCells ; i++){
				tasksToDo[i] = Integer.parseInt(line[i]);
				totalTasks += tasksToDo[i];
			}

			in.readLine();

			// Read and parse the available users
			for (int k = 0 ; k < nPeriods * nTypes ; k++) {
				line = in.readLine().split(" ");
				int m = Integer.parseInt(line[0]);
				int t = Integer.parseInt(line[1]);

				line = in.readLine().split(" ");

				for (int i = 0 ; i < nCells ; i++) {
					customers[i][m][t] = Integer.parseInt(line[i]);
					totalUsers += customers[i][m][t];
				}
			}

			System.out.println("TOTAL USERS: " + totalUsers);
		System.out.println("TOTAL TASKS: " + totalTasks);
			in.close();			
		} catch (IOException ioe) {
			System.err.println("Unable to read file: " + inputFile);
			ioe.printStackTrace();
			System.exit(1);
		}
		

		random = new Random();
	}

	/**
	 * Uses Greedy Search to find a solution to the problem. To do that it scans each 
	 * destination and looks for the cheapest users in order to execute all tasks.
	 * 
	 * @return a greedy solution
	 */
	public Solution greedySearch() {
		Solution greedy = new Solution();

		for (int j = 0 ; j < nCells ; j++) {
			if (greedy.totalCustomers <= 0)
				break;

			/*
			 *  Search the cheapest user category to go in cell J.
			 *  To evaluate "cheapness" the actual cost of a user is divided
			 *  by the number of tasks it can accomplish. In this way users who
			 *  can perform more tasks are given priority.
			 */
			while(greedy.fullfilled[j] < tasksToDo[j] && greedy.totalCustomers > 0) {
				float wBestCost = Float.POSITIVE_INFINITY;
				int[] bestUser = {-1, -1, -1};

				for (int i = 0 ; i < nCells ; i++) {
					if (i == j)
						continue;

					for (int m = 0 ; m < nTypes ; m++) {
						for (int t = 0 ; t < nPeriods ; t++) {
							if (greedy.unassignedUsers[i][m][t] > 0 && costs[i][j][m][t] / typeTasks[m] < wBestCost) {
								wBestCost = costs[i][j][m][t] / typeTasks[m];
								bestUser[0] = i;
								bestUser[1] = m;
								bestUser[2] = t;
							}
						}
					}
				}

				int iBest = bestUser[0];
				int mBest = bestUser[1];
				int tBest = bestUser[2];
				
				// Find minimum amount of cheap users to cover all required tasks
				int required = (int) Math.ceil(((float)(tasksToDo[j] - greedy.fullfilled[j])) / typeTasks[mBest]);
				// Clamp required users to the max available
				int dispatchable = Math.min(required, greedy.unassignedUsers[iBest][mBest][tBest]);

				// Actually assign the correct number of users to cell J
				greedy.unassignedUsers[iBest][mBest][tBest] -= dispatchable;
				greedy.fullfilled[j] += typeTasks[mBest] * dispatchable;
				greedy.totalCustomers -= dispatchable;
				greedy.totalCost += costs[iBest][j][mBest][tBest] * dispatchable;
				greedy.solMatrix[iBest][j][mBest][tBest] += dispatchable;
				System.out.println(String.format("CHEAPEST: %d %d %d | ASSIGNED TO %d: %d", iBest, mBest, tBest, j, dispatchable));
			}
		}

		return greedy;
	}

	public Solution randomGreedySearch() {
		Solution greedy = new Solution();
		LinkedList<Integer> listJ = new LinkedList<>();
		for(int i = 0; i < nCells; i++)
			listJ.add(i);
		Collections.shuffle(listJ);

		while(!listJ.isEmpty()) {
			int j = listJ.pop();
			if (greedy.totalCustomers <= 0)
				break;

			/*
			 *  Search the cheapest user category to go in cell J.
			 *  To evaluate "cheapness" the actual cost of a user is divided
			 *  by the number of tasks it can accomplish. In this way users who
			 *  can perform more tasks are given priority.
			 */
			while(greedy.fullfilled[j] < tasksToDo[j] && greedy.totalCustomers > 0) {
				float wBestCost = Float.POSITIVE_INFINITY;
				int[] bestUser = {-1, -1, -1};

				for (int i = 0 ; i < nCells ; i++) {
					if (i == j)
						continue;

					for (int m = 0 ; m < nTypes ; m++) {
						for (int t = 0 ; t < nPeriods ; t++) {
							if (greedy.unassignedUsers[i][m][t] > 0 && costs[i][j][m][t] / typeTasks[m] < wBestCost) {
								wBestCost = costs[i][j][m][t] / typeTasks[m];
								bestUser[0] = i;
								bestUser[1] = m;
								bestUser[2] = t;
							}
						}
					}
				}

				int iBest = bestUser[0];
				int mBest = bestUser[1];
				int tBest = bestUser[2];
				
				// Find minimum amount of cheap users to cover all required tasks
				int required = (int) Math.ceil(((float)(tasksToDo[j] - greedy.fullfilled[j])) / typeTasks[mBest]);
				// Clamp required users to the max available
				int dispatchable = Math.min(required, greedy.unassignedUsers[iBest][mBest][tBest]);

				// Actually assign the correct number of users to cell J
				greedy.unassignedUsers[iBest][mBest][tBest] -= dispatchable;
				greedy.fullfilled[j] += typeTasks[mBest] * dispatchable;
				greedy.totalCustomers -= dispatchable;
				greedy.totalCost += costs[iBest][j][mBest][tBest] * dispatchable;
				greedy.solMatrix[iBest][j][mBest][tBest] += dispatchable;
				System.out.println(String.format("CHEAPEST: %d %d %d | ASSIGNED TO %d: %d", iBest, mBest, tBest, j, dispatchable));
			}
		}

		return greedy;
	}

	public Solution altruisticGreedySearch() {
		Solution greedy = new Solution();
		LinkedList<Integer> listJ = new LinkedList<>();
		for(int i = 0; i < nCells; i++)
			listJ.add(i);
		Collections.shuffle(listJ);

		int uncompleteCells = nCells;
		for(int i = 0; i < nCells; i++)
			if(tasksToDo[i] == 0)
				uncompleteCells--;

		int index = 1;
		while(uncompleteCells > 0){
			if(greedy.totalCustomers <= 0)
				break;
			System.out.println("Uncomplete: " + uncompleteCells);
			System.out.println("Iteration " + index++);
			for(Integer j : listJ){
				//System.out.println(String.format("J: %d, Done: %d, ToDo: %d", j, greedy.fullfilled[j], tasksToDo[j]));
				if(greedy.fullfilled[j] < tasksToDo[j]){
					//int tasksInThisIteration = (int)Math.ceil(Math.random() * (tasksToDo[j] - greedy.fullfilled[j]) / 4);
					int tasksInThisIteration = 1;
					//System.out.println("In this iteration: " + tasksInThisIteration);
					while(tasksInThisIteration > 0 && greedy.totalCustomers > 0) {
						float wBestCost = Float.POSITIVE_INFINITY;
						int[] bestUser = {-1, -1, -1};
						for (int i = 0 ; i < nCells ; i++) {
							if (i != j){
								for (int m = 0 ; m < nTypes ; m++) {
									for (int t = 0 ; t < nPeriods ; t++) {
										if (greedy.unassignedUsers[i][m][t] > 0 && costs[i][j][m][t] / typeTasks[m] < wBestCost) {
											wBestCost = costs[i][j][m][t] / typeTasks[m];
											bestUser[0] = i;
											bestUser[1] = m;
											bestUser[2] = t;
											// Un unico migliroe non basta perchè tutti sceglierebbero quello di prima
											// Quindi 2° idea, un set di minimi tra cui scegliere a random
											// Fallo a casa
										}
									}
								}
							}
						}
						int iBest = bestUser[0];
						int mBest = bestUser[1];
						int tBest = bestUser[2];
						// Find minimum amount of cheap users to cover all required tasks
						int required = (int) Math.ceil((float)(tasksInThisIteration) / typeTasks[mBest]);
						// Clamp required users to the max available
						int dispatchable = Math.min(required, greedy.unassignedUsers[iBest][mBest][tBest]);
						// Actually assign the correct number of users to cell J
						greedy.unassignedUsers[iBest][mBest][tBest] -= dispatchable;
						greedy.totalCustomers -= dispatchable;
						greedy.totalCost += costs[iBest][j][mBest][tBest] * dispatchable;
						greedy.solMatrix[iBest][j][mBest][tBest] += dispatchable;
						greedy.fullfilled[j] += typeTasks[mBest] * dispatchable;
						tasksInThisIteration -= typeTasks[mBest] * dispatchable;
						if(greedy.fullfilled[j] >= tasksToDo[j]){
							//System.out.println("Cell " + j + " completed.");
							uncompleteCells--;
							//System.out.println("Uncomplete cells: " + uncompleteCells);
						}
						//System.out.println(String.format("CHEAPEST: %d %d %d | ASSIGNED TO %d: %d", iBest, mBest, tBest, j, dispatchable));
					}	
				}
			}
		}
		return greedy;
	}
	
	public Solution randomSearch() {
		long start = System.nanoTime();
		Solution randomic = new Solution();
		LinkedList<Integer> destinations = new LinkedList<Integer>();
		
		for (int i = 0 ; i < nCells ; i++) {
			destinations.add(i);
		}
		
		Collections.shuffle(destinations);
		
		for (int j : destinations) {
			while(randomic.fullfilled[j] < tasksToDo[j] && randomic.totalCustomers > 0) {
				// Randomize type, source and timeframe
				int i = random.nextInt(nCells);
				int m = random.nextInt(nTypes);
				int t = random.nextInt(nPeriods);
				
				if (randomic.unassignedUsers[i][m][t] > 0) {
					// Find minimum amount of random users to cover all required tasks
					int required = (int) Math.ceil(((float)(tasksToDo[j] - randomic.fullfilled[j])) / typeTasks[m]);
					// Clamp required users to the max available
					int dispatchable = Math.min(required, randomic.unassignedUsers[i][m][t]);
					int dispatched = dispatchable == 1 ? 1 : random.nextInt(dispatchable - 1) + 1;
					
					randomic.unassignedUsers[i][m][t] -= dispatched;
					randomic.fullfilled[j] += typeTasks[m] * dispatched;
					randomic.totalCustomers -= dispatched;
					randomic.totalCost += costs[i][j][m][t] * dispatched;
					randomic.solMatrix[i][j][m][t] += dispatched;
				}
			}
		}
		
		randomic.elapsedMillis = (System.nanoTime() - start) / 1E+6f;
		
		return randomic;
	}

	public Feasibility checkFeasibility(Solution sol) {
		for (int i = 0 ; i < nCells ; i++) {
			// Check against Task Demand constraint
			if (sol.fullfilled[i] < tasksToDo[i])
				return Feasibility.UNF_DEMAND;

			for (int m = 0 ; m < nTypes ; m++) {
				for (int t = 0 ; t < nPeriods ; t++) {
					int assigned = 0;
					// Compute total amount of users of type m, coming from cell i, dispatched at time t 
					for (int j = 0 ; j < nCells ; j++) {
						assigned += sol.solMatrix[i][j][m][t];
					}
					// Check against over-assignment of users 
					if (assigned > customers[i][m][t])
						return Feasibility.UNF_CUSTOMERS;
				}
			}
		}

		return Feasibility.FEASIBLE;
	}
}