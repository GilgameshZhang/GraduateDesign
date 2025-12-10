package ProblemFrame;

import AlgorithmFrame.bachSelect.ga.Genome;
import AlgorithmFrame.machineChoice.ga.BatchGenome;
import lombok.Data;

import java.util.List;
@Data
public class Result {
    List<BatchResult> solutionList;
    List<Double> ireatorList;

    public Result() {
    }

    public Result(List<BatchResult> solutionList, List<Double> ireatorList) {
        this.solutionList = solutionList;
        this.ireatorList = ireatorList;
    }


}
