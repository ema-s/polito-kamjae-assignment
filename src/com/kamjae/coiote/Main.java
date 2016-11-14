package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class Main {

	public static void main(String[] args) {
		GAProblem p;
		String source = null;
		String optimal = null;
		
		for (int i = 0  ; i < args.length ; i++) {
			if (args[i].equals("-s")) {
				source = args[++i];
			}
			else if (args[i].equals("-os")) {
				optimal = args[++i];
			}
		}
		
		if (source == null) {
			printHelp();
			return;
		} else {
			p = new GAProblem(source);
		}

		GASolution sol = p.solveProblem();

		//System.out.println(sol);
		System.out.println("SOURCE: " + source);
		System.out.println("TOTAL COST: " + sol.getCost());
		
		// Compare found cost with optimal solution and compute the optimality gap
		if (optimal != null) {
			try {	
				BufferedReader in = new BufferedReader(new FileReader(optimal));
				String line;
				String instance = ((source.split("/")[1]).split(".txt"))[0];
				while ((line = in.readLine()) != null) {
					String[] vals = line.split(String.format(";\t"));
					if (vals[0].equals(instance)) {
						float opCost = Float.parseFloat(vals[2]);
						System.out.println("FILE: " + instance);
						System.out.println("OPTIMAL COST: " + opCost);
						System.out.println("MY COST: " + sol.getCost());
						float opGap = (sol.getCost() - opCost) / opCost * 100;
						System.out.println("OPTIMALITY GAP: " + opGap + "%");
						break;
					}
				}
				
				in.close();
			} catch (IOException ioe) {
				ioe.printStackTrace();
				System.exit(1);
			}
		}
	}
	
	public static void printHelp() {
		System.out.println("Usage: java -jar coiote.jar -s sourceFile [-os optimalSolution]");
	}
}
