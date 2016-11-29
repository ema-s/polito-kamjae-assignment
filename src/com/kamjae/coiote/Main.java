package com.kamjae.coiote;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import com.kamjae.coiote.Problem.Solution;

public class Main {

	public static void main(String[] args) {
		Problem p;
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
			p = new Problem(source);
		}
		
		Solution sol = p.solveProblem();
		
		System.out.println(sol);
		System.out.println("ELAPSED TIME: " + sol.getElapsedMillis() + " ms");
		System.out.println("SOURCE: " + source);
		System.out.println("SOLUTION STATUS: " + p.checkFeasibility(sol));
		System.out.println("TOTAL COST: " + sol.getTotalCost());
		
		// Compare found cost with optimal solution and compute the optimality gap
		if (optimal != null) {
			try {	
				BufferedReader in = new BufferedReader(new FileReader(optimal));
				String line;
				
				String[] sourcePath = source.split("/");
				String fileName = sourcePath[sourcePath.length - 1].split("\\.")[0];
				System.out.println("FILENAME: "+fileName);
				
				while ((line = in.readLine()) != null) {
					String[] vals = line.split(String.format(";\t"));
					if (fileName.equals(vals[0])) {
						System.out.println("OPT_FILE: " + vals[0]);
						float opCost = Float.parseFloat(vals[2]);
						float opGap = (sol.getTotalCost() - opCost) / opCost * 100;
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
