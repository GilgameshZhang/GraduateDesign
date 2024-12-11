package ProblemFrame;

import java.util.List;

public class BatchResult {
    public List<Double> startTimes;
    public List<Double> endTimes;
    public List<Solution> solutions;
    public double fitness;

    public BatchResult(List<Solution> solutions, List<Double> startTimes, List<Double> endTimes, double fitness) {
        this.startTimes = startTimes;
        this.endTimes = endTimes;
        this.solutions = solutions;
        this.fitness = fitness;
    }
}
