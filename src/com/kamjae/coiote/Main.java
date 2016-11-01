package com.kamjae.coiote;

public class Main {

	public static void main(String[] args) {
		Problem p;
		String source = null;
		
		for (int i = 0  ; i < args.length ; i++) {
			if (args[i].equals("-s")) {
				source = args[++i];
			}
		}
		
		if (source == null) {
			printHelp();
			return;
		} else {
			p = new Problem(source);
		}
	}
	
	public static void printHelp() {
		System.out.println("Usage: java coiote -s sourceFile");
	}
}
