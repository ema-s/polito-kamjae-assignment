package com.kamjae.coiote;
import java.util.*;

public class GASolution
{
    public int nCells;
    public int nTypes;
    public int nPeriods;
    /** array of genes that comprise the chromosome */
    public byte[][][][] genes;
    /** absolute (not relative) fitness value */
    public double fitness; 
    /** 0 = worst fit, PopDim = best fit */
    public int fitnessRank; 

    public GASolution(int cells, int types, int periods)
    {
        this.nCells = cells;
        this.nTypes = types;
        this.nPeriods = periods;
        this.genes = new byte[nCells][nCells][nTypes][nPeriods];

        if(Math.random() < 0.3)
            initGreedy();
        else
            initRandom();
        // for (int i = 0; i < nCells; i++)
        //     for (int j = 0; j < nCells; j++)
        //         for (int m = 0; m < nTypes; m++)
        //             for (int t = 0; t < nPeriods; t++)
        //                 if(this.genes[i][j][m][t] != 0)
        //                     System.out.println("GENE_SETT " + i + " " + j + " " + m + " " + t + " = " + this.genes[i][j][m][t]);
        this.setFitness();
    }

    public GASolution(int cells, int types, int periods, boolean dummy){
        this.nCells = cells;
        this.nTypes = types;
        this.nPeriods = periods;
        this.genes = new byte[nCells][nCells][nTypes][nPeriods];
    }

    public void initGreedy(){
        LinkedList<Integer> listJ = new LinkedList<>();
        for(int i = 0; i < nCells; i++)
            listJ.add(i);   
        Collections.shuffle(listJ);

        byte[][][] availableUsers = new byte[nCells][nTypes][nPeriods];
        for(int i = 0; i < nCells; i++)
            for(int m = 0; m < nTypes; m++)
                for(int t = 0; t < nPeriods; t++)
                    availableUsers[i][m][t] = GA.customers[i][m][t];

        for(int j : listJ){
            byte tasksLeft = GA.tasksToDo[j];
            while(tasksLeft > 0) {
                float wBestCost = Float.POSITIVE_INFINITY;
                int[] bestUser = {-1, -1, -1};
                for (int i = 0 ; i < nCells ; i++) {
                    if (i != j){
                        for (int m = 0 ; m < nTypes ; m++) {
                            for (int t = 0 ; t < nPeriods ; t++) {
                                if (availableUsers[i][m][t] > 0 && GA.costs[i][j][m][t] / GA.typeTasks[m] < wBestCost) {
                                    wBestCost = GA.costs[i][j][m][t] / GA.typeTasks[m];
                                    bestUser[0] = i;
                                    bestUser[1] = m;
                                    bestUser[2] = t;
                                }
                            }
                        }
                    }               
                }
                int iBest = bestUser[0];
                int mBest = bestUser[1];
                int tBest = bestUser[2];      
                // Find minimum amount of cheap users to cover all required tasks
                byte required = (byte)(Math.ceil((float)tasksLeft / GA.typeTasks[mBest]));
                // Clamp required users to the max available
                byte assigned = (byte)(Math.min(required, availableUsers[iBest][mBest][tBest]));

                // Actually assign the correct number of users to cell J
                this.genes[iBest][j][mBest][tBest] += assigned;
                availableUsers[iBest][mBest][tBest] -= assigned;
                tasksLeft -= GA.typeTasks[mBest] * assigned;
            }
        }
    }

    public void initRandom(){
        byte[][][] usersAvailable = new byte[nCells][nTypes][nPeriods];
        for(int i = 0; i < nCells; i++)
            for(int m = 0; m < nTypes; m++)
                for(int t = 0; t < nPeriods; t++)
                    usersAvailable[i][m][t] = GA.customers[i][m][t];

        for(int j = 0; j < nCells; j++){
            byte tasksLeft = GA.tasksToDo[j];
            while(tasksLeft > 0){
                int i;
                do{
                    i = GA.getRandom(nCells);
                } while (i == j);
                int m = GA.getRandom(nTypes);
                int t = GA.getRandom(nPeriods);
                if (usersAvailable[i][m][t] > 0) {
                    byte required = (byte)(Math.ceil((float)tasksLeft / GA.typeTasks[m]));
                    byte assignable = (byte)(Math.min(required, usersAvailable[i][m][t]));
                    byte assigned = assignable == 1 ? 1 : (byte)(GA.getRandom(assignable));
                    this.genes[i][j][m][t] += assigned;
                    usersAvailable[i][m][t] -= assigned;
                    tasksLeft -= GA.typeTasks[m] * assigned;
                }
            }
        }
    }
    
    /**
     * Return the genes as a string
     * @return String
     */
    public String toString(){
        return (getGenesAsString());
    }

    /**
     * Get the number of genes in common
     * @param chromosome
     * @return int
     */
    public int getNumGenesInCommon(GASolution chromosome){
        //@@ need to write some code to see how many float genes are within 10% of each other
        //if they are within 10%, they are considered to be the "same" gene
        return (chromosome.genes.length);
    }

    /**
     * Return the array of genes as a string
     * @return String 
     */
    public String getGenesAsString(){
        String sGenes = "";

        for (int i = 0; i < nCells; i++)
            for(int j = 0; j < nCells; j++)
                for(int m = 0; m < nTypes; m++)
                    for(int t = 0; t < nPeriods; t++)
                        sGenes += " " + genes[i][j][m][t] + ",";
        return (sGenes);
    }

    /**
     * Copy the genes from the given chromosome over the existing genes
     * @param Chromosome 
     */
    public void copyChromGenes(GASolution chromosome){
        for (int i = 0; i < nCells; i++)
            for(int j = 0; j < nCells; j++)
                for(int m = 0; m < nTypes; m++)
                    for(int t = 0; t < nPeriods; t++)
                        this.genes[i][j][m][t] = chromosome.genes[i][j][m][t];
    }

    /**
     * return the array of genes as a string
     * @return String
     */
    public String getGenesAsStr(){
        System.out.println("As STR");
        String sGenes = "";
        for (int i = 0; i < nCells; i++)
            for(int j = 0; j < nCells; j++)
                for(int m = 0; m < nTypes; m++)
                    for(int t = 0; t < nPeriods; t++)
                        sGenes += this.genes[i][j][m][t] + ",";
        return (sGenes);
    }

    /** get the fitness value for the given chromosome */
    public boolean isFeasible(){
        for (int j = 0; j < nCells; j++) {
            if(GA.tasksToDo[j] > 0){
                int tasksDoneOnJ = 0;
                for (int i = 0; i < nCells; i++)
                    for (int m = 0; m < nTypes; m++)
                        for (int t = 0; t < nPeriods; t++)
                            if(this.genes[i][j][m][t] != 0){
                                //System.out.println("GENE_READ " + i + " " + j + " " + m + " " + t + " = " + this.genes[i][j][m][t]);
                                tasksDoneOnJ += (GA.typeTasks[m] * this.genes[i][j][m][t]);
                            }

                //System.out.println(j + " " + tasksDoneOnJ + " " + GA.tasksToDo[j]);
                if (tasksDoneOnJ < GA.tasksToDo[j]){
                    System.out.println("UNF_DEMAND");
                    return false;
                }
            }
        }

        // Max Number of users
        for (int i = 0; i < nCells; i++)
            for (int m = 0; m < nTypes; m++)
                for (int t = 0; t < nPeriods; t++) {
                    int assignedUsersIMT = 0;
                    for (int j = 0; j < nCells; j++)
                        assignedUsersIMT += this.genes[i][j][m][t];
                    if (assignedUsersIMT > GA.customers[i][m][t]){
                        System.out.println("UNF_USERS");
                        return false;
                    }
                }
        //System.out.println("FEASIBLE");
        return true;
    }

    public float getCost(){
        float cost = 0;
        for (int i = 0; i < nCells; i++)
            for (int j = 0; j < nCells; j++)
                for (int m = 0; m < nTypes; m++)
                    for (int t = 0; t < nPeriods; t++)
                        cost += GA.costs[i][j][m][t] * this.genes[i][j][m][t];
        return cost;
    }

    public void setFitness(){
        // LA FITNESS E' INVERSA, A BASSO FITNESS CORRISPONDE ALTO RANKING
        if(!this.isFeasible()){
            fitness = Double.MAX_VALUE;
        } else{
            this.fitness =  this.getCost();
        }
    }
}
