package ProblemFrame;

import AlgorithmFrame.bachSelect.ga.Genome;
import AlgorithmFrame.machineChoice.ga.BatchGenome;

import java.util.List;

public class Result {
    //拿到machine中的startTimeList和endTimeList
    //用于制作甘特图
    public Machine[] machineList;
    //零件排布结果
    public List<BatchResult> solutionList;
    //Cmax
    public double cmax;

    public Result() {

    }
    public Result(BatchGenome bestGenome) {
        this.machineList = bestGenome.machines;
        this.solutionList = bestGenome.solutions;
        this.cmax = bestGenome.fitness;
    }
}
