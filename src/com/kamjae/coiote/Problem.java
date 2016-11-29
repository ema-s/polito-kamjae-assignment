package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
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
	private int[][][][] costs;
	private int[] tasksToDo;
	private HashMap<User, Integer> customers;

	// Data to keep track of solution evolution
	private Solution bestSolution;
	
	private class AnnealingParams {
		public static final int DEFAULT_MAX_ITERATIONS = 5000;
		
		private static final float DEFAULT_ALPHA = 0.90f;
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
		private ArrayList<HashMap<User, Integer>> solMatrix;
		private HashMap<User, Integer> unassignedUsers;
		private int[] fullfilled;
		private int totalCustomers;
		private float totalCost;
		private float elapsedMillis;
		private ArrayList<Move> neighborhood;

		private Solution() {
			allocate();
			
			unassignedUsers.putAll(customers);
			totalCustomers = customers.size();
		}
		
		private void allocate() {
			solMatrix = new ArrayList<HashMap<User, Integer>>();
			unassignedUsers = new HashMap<User, Integer>();
			fullfilled = new int[nCells];
			totalCustomers = 0;
			totalCost = 0;
			elapsedMillis = 0;
			neighborhood = new ArrayList<Move>();
			
			for (int i = 0 ; i < nCells ; i++) {
				fullfilled[i] = 0;
				solMatrix.add(new HashMap<User, Integer>());
			}
		}
		
		private Solution(Solution source) {
			allocate();
			
			for (int i = 0 ; i < nCells ; i++) {
				solMatrix.get(i).putAll(source.solMatrix.get(i));
			}

			totalCustomers = source.totalCustomers;
			totalCost = source.totalCost;
			unassignedUsers.putAll(source.unassignedUsers);
			
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
			
			for (HashMap<User, Integer> j : solMatrix) {
				if (j.size() == 0)
					continue;
				
				output += "Cell " + solMatrix.indexOf(j) + "\n";
				for (User u : j.keySet()) {
					output += u.toString() + ": " + j.get(u) + "\n";
				}
			}
			
			return output;
		}
		
		public void generateRandomNeighborhood() {
			for (int i = 0 ; i < nCells ; i++) {
				HashMap<User, Integer> from = solMatrix.get(i);
				
				if (tasksToDo[i] == 0)
					continue;
				
				for (int j = i + 1 ; j < nCells ; j++) {
					if (tasksToDo[j] == 0)
						continue;
					
					HashMap<User, Integer> to = solMatrix.get(j);
					neighborhood.add(generateMove(from, to, i, j));
				}
				
				neighborhood.add(generateMove(from, unassignedUsers, i, -1));
			}
		}
		
		private Move generateMove(HashMap<User, Integer> from, HashMap<User, Integer> to, int i, int j) {
			User u1Best = null;
			float wCost = 0;
			float wBestCost = Float.POSITIVE_INFINITY;
			
			if (j == -1) {
				// Find most expensive in this list to get out
				wBestCost = 0;
				for (User u1 : from.keySet()) {
					wCost = costs[u1.i][i][u1.m][u1.t] / typeTasks[u1.m];
					if (wCost > wBestCost) {
						wCost = wBestCost;
						u1Best = u1;
					}
				}
			} else {
				// Find cheapest for to list to get out
				for (User u1 : from.keySet()) {
					wCost = costs[u1.i][j][u1.m][u1.t] / typeTasks[u1.m];
					if (wCost < wBestCost) {
						wCost = wBestCost;
						u1Best = u1;
					}
				}
			}
			
			User u2Best = null;
			wCost = 0;
			wBestCost = Float.POSITIVE_INFINITY;
			
			for (User u2 : to.keySet()) {
				wCost = costs[u2.i][i][u2.m][u2.t] / typeTasks[u2.m];
				if (wCost < wBestCost) {
					wCost = wBestCost;
					u2Best = u2;
				}
			}
			
			float newCost = 0;
			
			if (j == -1) {
				newCost = totalCost - costs[u1Best.i][i][u1Best.m][u1Best.t]
									+ costs[u2Best.i][i][u2Best.m][u2Best.t];
			} else {
				newCost = totalCost - costs[u1Best.i][i][u1Best.m][u1Best.t]
									- costs[u2Best.i][j][u2Best.m][u2Best.t]
									+ costs[u1Best.i][j][u1Best.m][u1Best.t]
									+ costs[u2Best.i][i][u2Best.m][u2Best.t];
			}
			
			return new Move(i, j, u1Best, u2Best, newCost);
		}
		
		public Solution apply(Move m) {
			Solution s = new Solution(this);
			
			HashMap<User, Integer> from = s.solMatrix.get(m.j0);
			HashMap<User, Integer> to;
			
			if (m.j1 == -1) {
				// -1 is a coding to say "Use the unassigned pool"
				to = s.unassignedUsers;
			} else {
				to = s.solMatrix.get(m.j1);
			}
			
			Integer fromCnt1 = from.get(m.user1);
			Integer fromCnt2 = from.get(m.user2);
			Integer toCnt1 = to.get(m.user1);
			Integer toCnt2 = to.get(m.user2);
			
			if (fromCnt1 == 1) {
				// Source has only ONE User1. Remove mapping
				from.remove(m.user1);
			} else {
				// Source has > 1 User1. Decrease.
				from.put(m.user1, fromCnt1 - 1);
			}
			
			if (fromCnt2 == null) {
				// No mapping in source for User2. Add mapping
				from.put(m.user2, 1);
			} else {
				// There is a previous mapping in source for User2. Update count.
				from.put(m.user2, fromCnt2 + 1);
			}
			
			if (toCnt1 == null) {
				// No mapping in destination for User1. Add mapping.
				to.put(m.user1, 1);
			} else {
				// Existing mapping for User1 in destination. Update count.
				to.put(m.user2, toCnt1 + 1);
			}
			
			if (toCnt2 == 1) {
				// Destination has only ONE User2. Remove mapping.
				to.remove(m.user2);
			} else {
				// Destination has >1 User2. Update count.
				to.put(m.user2, toCnt2 - 1);
			}
			
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
			costs = new int[nCells][nCells][nTypes][nPeriods];
			tasksToDo = new int[nCells];
			customers = new HashMap<User, Integer>();

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
						costs[i][j][m][t] = (int)Math.floor(Double.parseDouble(line[j]));
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
					if (count == 0) continue;
					customers.put(new User(i, m, t), count);
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

		bestSolution = greedySearch();
		//bestSolution = randomSearch();
		//bestSolution = randomStartGreedySearch();
		//bestSolution = randomSolveFast();
		//simulateAnnealing(AnnealingParams.DEFAULT_MAX_ITERATIONS, SIMULATION_THRESHOLD);

		bestSolution.elapsedMillis = (System.nanoTime() - start) / 1E+6f;
		return bestSolution;
	}
	
	private void simulateAnnealing(int maxIterations, int maxMillis) {
		long start = System.currentTimeMillis();
		
		Solution currentSolution = bestSolution;
		currentSolution.generateRandomNeighborhood();
		AnnealingParams params = new AnnealingParams(bestSolution);
		
		for (int c = 0 ; c < maxIterations ; c++) {
			if (System.currentTimeMillis() - start >= maxMillis)
				break;
			
			if (currentSolution.neighborhood.isEmpty()) {
				System.err.println("Dafuq empty neighborhood");
				System.exit(1);
			}
				
			
			for (Move m : currentSolution.neighborhood) {
				if (m.newCost < bestSolution.totalCost) {
					currentSolution = currentSolution.apply(m);
					currentSolution.generateRandomNeighborhood();
					bestSolution = currentSolution;
					break;
				} else {
					double acceptance = Math.exp((currentSolution.totalCost - m.newCost) / params.getT(c));
					if (random.nextDouble() < acceptance) {
						currentSolution = currentSolution.apply(m);
						currentSolution.generateRandomNeighborhood();
						break;
					}
				}
			}
		}
	}
	
	private void assignAllPossible(User u, Solution s, int cell, Iterator<User> it) {
		int required = (int)Math.ceil(((double)(tasksToDo[cell] - s.fullfilled[cell])) / typeTasks[u.m]);
		int assignable = s.unassignedUsers.get(u);
		int assigned = 0;
		
		if (assignable <= required) {
			// Assign all available users
			assigned = assignable;
			if (it != null) {
				it.remove();
			} else {
				s.unassignedUsers.remove(u);
			}
		} else {
			// Assign only required users
			assigned = required;
			s.unassignedUsers.put(u, assignable - required);
		}
		
		HashMap<User, Integer> cellMap = s.solMatrix.get(cell);
		Integer currentAssignmentInCell = cellMap.get(u);
		
		if (currentAssignmentInCell == null) {
			// No previous assignment for best user
			cellMap.put(u, assigned);
		} else {
			// Existing assignment for best user
			cellMap.put(u, currentAssignmentInCell + assigned);
		}

		// Actually assign the correct number of users to cell J
		s.fullfilled[cell] += typeTasks[u.m] * assigned;
		s.totalCustomers -= assigned;
		s.totalCost += costs[u.i][cell][u.m][u.t] * assigned;
		System.out.println(String.format("CHEAPEST: %s | ASSIGNED TO %d: %d", u, cell, assigned));
	}
	
	private void assignAllPossible(User u, Solution s, int cell){
		assignAllPossible(u, s, cell, null);
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
				
				for (User u : greedy.unassignedUsers.keySet()) {
					if (u.i == j)
						continue;
					
					float wCost = costs[u.i][j][u.m][u.t] / typeTasks[u.m];
					if (wCost < wBestCost) {
						wBestCost = wCost;
						bestUser = u;
					}
				}
				
				assignAllPossible(bestUser, greedy, j);
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
	        
	        for (Iterator<User> it = random.unassignedUsers.keySet().iterator() ; it.hasNext() ;) {
	        	if (random.fullfilled[j] < tasksToDo[j] && random.totalCustomers > 0) {
		        	User u = it.next();
		        	assignAllPossible(u, random, j, it);	
	        	} else {
	        		break;
	        	}	        	
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
				
				for (User u : random.unassignedUsers.keySet()) {
					if (u.i == j)
						continue;
					
					float wCost = costs[u.i][j][u.m][u.t] / typeTasks[u.m];
					if (wCost < wBestCost) {
						wBestCost = wCost;
						bestUser = u;
					}
				}

				assignAllPossible(bestUser, random, j);
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
				int k = random.nextInt(randomic.unassignedUsers.keySet().size());
				User u = (User)randomic.unassignedUsers.keySet().toArray()[k];
				
				if (u.i == j) {
					continue;		// WARNING! This may cause an infinite loop if all users are in j!
				}
				
				assignAllPossible(u, randomic, j);
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
