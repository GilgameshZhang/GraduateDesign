package ProblemFrame;

import ProgramEntity.Job;
import ProgramEntity.Solution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class Chromosome implements Comparable<Chromosome> {
    public int[] gene_OS;
    public int[] gene_MS;

    public Random r;
    public double fitness;
    public List<Solution>[] printSolution;

    public Chromosome(Random r) {
        this.r = r;
    }

    public Chromosome(Job[] entries, Random r) {
        this.r = r;
        ArrayList<Integer> os = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            for (int j = 0; j < entries[i].opsNr; j++)
                os.add(entries[i].index);//未+1
        }
        Collections.shuffle(os, this.r);

        ArrayList<Integer> ms = new ArrayList<>();
        for (int i = 0; i < entries.length; i++) {
            for (int j = 0; j < entries[i].opsNr; j++)
                ms.add(r.nextInt(entries[i].opsMacNr[j]) + 1); //有+1
        }

        this.gene_OS = new int[os.size()];
        for (int i = 0; i < os.size(); i++) {
            this.gene_OS[i] = os.get(i);
        }
        this.gene_MS = new int[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            this.gene_MS[i] = ms.get(i);
        }

        this.fitness = 0;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    public Chromosome(int[] OS, int[] MS, Random r) {
        this.gene_OS = OS;
        this.gene_MS = MS;
        this.r = r;
        this.fitness = -1;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    public Chromosome(Chromosome c) {
        this.gene_MS = new int[c.gene_MS.length];
        System.arraycopy(c.gene_MS, 0, this.gene_MS, 0, c.gene_MS.length);
        this.gene_OS = new int[c.gene_OS.length];
        System.arraycopy(c.gene_OS, 0, this.gene_OS, 0, c.gene_OS.length);
        this.r = c.r;
        this.fitness = c.fitness;
        // printSolution将在evaluate时根据打印机数量初始化
        this.printSolution = null;
    }

    @Override
    public int compareTo(Chromosome o) {
        Chromosome s = (Chromosome) o;
        if (s.fitness > this.fitness) {
            return 1;
        } else if (this.fitness == s.fitness) {
            return 0;
        } else {
            return -1;
        }
    }
}
