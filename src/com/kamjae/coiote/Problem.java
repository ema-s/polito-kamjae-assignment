package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.ArrayList;
import java.util.Random;

public class Problem {
	
	public static final int SIMULATION_THRESHOLD = 4500;
	
	private Random random;

	private int nCells;
	private int nPeriods;
	private int nTypes;

	private int[] typeTasks;	// Tasks each customer type can do
	private float[][][][] costs;
	private int[] tasksToDo;
	private ArrayList<User> customers;

	// Data to keep track of solution evolution
	private Solution bestSolution;
	
	private class AnnealingParams {
		public static final int DEFAULT_MAX_ITERATIONS = 50;
		
		private static final float DEFAULT_ALPHA = 0.98f;
		private static final int DEFAULT_GAMMA = 15;
		
		public float alpha;
		
		private int l;
		private float t0;
		
		public AnnealingParams(float alpha, int gamma, Solution start) {
			this.alpha = alpha;
			setGamma(gamma, start.neighborhood.size());
			setT0(start);
		}
		
		public AnnealingParams(Solution start) {
			this(DEFAULT_ALPHA, DEFAULT_GAMMA, start);
		}
		
		private void setGamma(int gamma, int neighborhoodSize) {
			l = gamma * neighborhoodSize;
		}
		
		private void setT0(Solution s) {
			float avgCost = 0;
			
			for (Move m : s.neighborhood)
				avgCost += m.newCost;
			
			avgCost /= s.neighborhood.size();
			
			t0 = (float)((s.totalCost - avgCost) / Math.log(0.5));
		}
		
		public float getT(int iteration) {
			int k = iteration / l;
			return (float)Math.pow(alpha, k) * t0;
		}
	}
	
	public class User {
		public int i, m, t;
		
		public User(int i, int m, int t) {
			this.i = i;
			this.m = m;
			this.t = t;
		}
		
		@Override
		public String toString() {
			return String.format("(%d, %d, %d)", i, m, t);
		}
	}
	
	public class Move {
		public int j0, j1;
		public User user1, user2;
		public float newCost;
		
		public Move(int j0, int j1, User user1, User user2, float newCost) {
			super();
			this.j0 = j0;
			this.j1 = j1;
			this.user1 = user1;
			this.user2 = user2;
			this.newCost = newCost;
		}
	}

	public class Solution {
		private ArrayList<ArrayList<User>> solMatrix;
		private ArrayList<User> unassignedUsers;
		private int[] fullfilled;
		private int totalCustomers;
		private float totalCost;
		private float elapsedMillis;
		private ArrayList<Move> neighborhood;

		private Solution() {
			allocate();
			
			unassignedUsers.addAll(customers);
			totalCustomers = customers.size();
		}
		
		private void allocate() {
			solMatrix = new ArrayList<ArrayList<User>>();
			unassignedUsers = new ArrayList<User>();
			fullfilled = new int[nCells];
			totalCustomers = 0;
			totalCost = 0;
			elapsedMillis = 0;
			neighborhood = new ArrayList<Move>();
			
			for (int i = 0 ; i < nCells ; i++) {
				fullfilled[i] = 0;
				solMatrix.add(new ArrayList<User>());
			}
		}
		
		private Solution(Solution source) {
			allocate();
			
			for (ArrayList<User> l : source.solMatrix) {
				ArrayList<User> newList = new ArrayList<User>();
				newList.addAll(l);
				solMatrix.add(newList);
			}

			totalCustomers = source.totalCustomers;
			totalCost = source.totalCost;
			unassignedUsers.addAll(source.unassignedUsers);
			
			for (int i = 0 ; i < nCells ; i++) {
				fullfilled[i] = source.fullfilled[i];
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
			
			for (ArrayList<User> j : solMatrix) {
				if (j.size() == 0)
					continue;
				
				output += "Cell " + solMatrix.indexOf(j) + "\n";
				for (User u : j) {
					output += u.toString() + "\n";
				}
			}
			
			return output;
		}
		
		public void generateRandomNeighborhood() {
			// TODO	
		}
		
		public Solution apply(Move m) {
			Solution s = new Solution(this);
			
			ArrayList<User> from = s.solMatrix.get(m.j0);
			ArrayList<User> to;
			
			if (m.j1 == -1) {
				// -1 is a coding to say "Use the unassigned pool"
				to = s.unassignedUsers;
			} else {
				to = s.solMatrix.get(m.j1);
			}
			
			from.remove(m.user1);
			from.add(m.user2);
			to.remove(m.user2);
			to.add(m.user1);
			
			s.totalCost = m.newCost;
			return s;
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
			in = new BufferedReader(new FileReader(inputFile));

			// Read and parse first line, containing the cardinalities of the problem
			String[] line = in.readLine().split(" ");
			nCells = Integer.parseInt(line[0]);
			nPeriods = Integer.parseInt(line[1]);
			nTypes = Integer.parseInt(line[2]);

			typeTasks = new int[nTypes];
			costs = new float[nCells][nCells][nTypes][nPeriods];
			tasksToDo = new int[nCells];
			customers = new ArrayList<User>();

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
					int count = Integer.parseInt(line[i]);
					for (int j = 0 ; j < count ; j++)
						customers.add(new User(i, m, t));
				}
			}

			in.close();			
		} catch (IOException ioe) {
			System.err.println("Unable to read file: " + inputFile);
			ioe.printStackTrace();
			System.exit(1);
		}
		
		bestSolution = null;
		random = new Random();
	}

	public Solution solveProblem(){
		// solveProblem is the only function called by Main
		// Inside we do all computation
		long start = System.nanoTime();

		bestSolution = randomStartGreedySearch();
		//simulateAnnealing(AnnealingParams.DEFAULT_MAX_ITERATIONS, SIMULATION_THRESHOLD);

		bestSolution.elapsedMillis = (System.nanoTime() - start) / 1E+6f;
		return bestSolution;
	}
	
	private void simulateAnnealing(int maxIterations, int maxMillis) {
		//long start = System.currentTimeMillis();
		
		Solution currentSolution = bestSolution;
		currentSolution.generateRandomNeighborhood();
		AnnealingParams params = new AnnealingParams(bestSolution);
		
		for (int c = 0 ; c < maxIterations ; c++) {
			//if (System.currentTimeMillis() - start >= maxMillis)
				//break;
			
			if (currentSolution.neighborhood.isEmpty())
				currentSolution.generateRandomNeighborhood();
			
			for (Move m : currentSolution.neighborhood) {
				if (m.newCost < currentSolution.totalCost) {
					currentSolution = currentSolution.apply(m);
					bestSolution = currentSolution;
				} else {
					double acceptance = Math.exp((currentSolution.totalCost - m.newCost) / params.getT(c));
					if (random.nextDouble() < acceptance) {
						currentSolution = currentSolution.apply(m);
					}
				}
			}
		}
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
				User bestUser = null;
				
				for (User u : greedy.unassignedUsers) {
					if (u.i == j)
						continue;
					
					float wCost = costs[u.i][j][u.m][u.t] / typeTasks[u.m];
					if (wCost < wBestCost) {
						wBestCost = wCost;
						bestUser = u;
					}
				}

				// Actually assign the correct number of users to cell J
				greedy.unassignedUsers.remove(bestUser);
				greedy.fullfilled[j] += typeTasks[bestUser.m];
				greedy.totalCustomers--;
				greedy.totalCost += costs[bestUser.i][j][bestUser.m][bestUser.t];
				greedy.solMatrix.get(j).add(bestUser);
				System.out.println(String.format("CHEAPEST: %s | ASSIGNED TO %d: %d", bestUser, j));
			}
		}

		return greedy;
	}

	public Solution randomSolveFast(){
		Solution random = new Solution();
		
		LinkedList<Integer> listJ = new LinkedList<>();
		for(int i = 0; i < nCells; i++)
			listJ.add(i);	
  		Collections.shuffle(listJ);

	    while (!listJ.isEmpty()) {
	    	int j = listJ.pop();
	        int demand = tasksToDo[j];
	        
	        for (Iterator<User> it = random.unassignedUsers.iterator() ; it.hasNext() ;) {
	        	User u = it.next();
	        	int tasksPerUser = typeTasks[u.m];
	        	
	        	if (u.i == j)
	        		continue;
	        	
	        	random.solMatrix.get(j).add(u);
	        	random.totalCustomers--;
	        	random.fullfilled[j] += tasksPerUser;
	        	demand -= tasksPerUser;
	        	it.remove();
	        	
	        	if (demand <= 0)
	        		break;
	        }	
	    }
	    return random;
	}

	public Solution randomStartGreedySearch(){
		Solution random = new Solution();
		LinkedList<Integer> listJ = new LinkedList<Integer>();
		
		for(int i = 0; i < nCells; i++)
			listJ.add(i);	
  		Collections.shuffle(listJ);
  		while(!listJ.isEmpty() && random.totalCustomers > 0){
  			int j = listJ.pop();

			while(random.fullfilled[j] < tasksToDo[j] && random.totalCustomers > 0) {
				float wBestCost = Float.POSITIVE_INFINITY;
				User bestUser = null;
				
				for (User u : random.unassignedUsers) {
					if (u.i == j)
						continue;
					
					float wCost = costs[u.i][j][u.m][u.t] / typeTasks[u.m];
					if (wCost < wBestCost) {
						wBestCost = wCost;
						bestUser = u;
					}
				}

				// Actually assign the correct number of users to cell J
				random.unassignedUsers.remove(bestUser);
				random.fullfilled[j] += typeTasks[bestUser.m];
				random.totalCustomers--;
				random.totalCost += costs[bestUser.i][j][bestUser.m][bestUser.t];
				random.solMatrix.get(j).add(bestUser);
			}
  		}
		return random;
	}
	
	public Solution randomSearch() {
		Solution randomic = new Solution();
		ArrayList<Integer> destinations = new ArrayList<Integer>();
		
		for (int i = 0 ; i < nCells ; i++) {
			destinations.add(i);
		}
		
		Collections.shuffle(destinations);
		
		for (int j : destinations) {
			while(randomic.fullfilled[j] < tasksToDo[j] && randomic.totalCustomers > 0) {
				// Randomize type, source and timeframe
				int k = random.nextInt(randomic.unassignedUsers.size());
				User u = randomic.unassignedUsers.get(k);
				
				if (u.i == j) {
					continue;		// WARNING! This may cause an infinite loop if all users are in j!
				}
				
				randomic.unassignedUsers.remove(k);
				randomic.fullfilled[j] += typeTasks[u.m];
				randomic.totalCustomers--;
				randomic.totalCost += costs[u.i][j][u.m][u.t];
				randomic.solMatrix.get(j).add(u);
			}
		}		
		return randomic;
	}

	public Feasibility checkFeasibility(Solution sol) {
		for (int i = 0 ; i < nCells ; i++) {
			// Check against Task Demand constraint
			if (sol.fullfilled[i] < tasksToDo[i])
				return Feasibility.UNF_DEMAND;
			
			/*	Using a list of users it is impossible to have a customer unfeasibility
			 * 
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
			*/
		}

		return Feasibility.FEASIBLE;
	}
}
