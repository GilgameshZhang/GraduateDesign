package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSEvaluator;
import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.List;

/**
 * WRJ - Worst Removal Job (改为基于Cmax)
 * 移除后使Cmax下降最大的作业
 */
public class WorstRemovalJob implements DestroyOperator {
    private ALNSEvaluator evaluator;
    
    public WorstRemovalJob(ALNSEvaluator evaluator) {
        this.evaluator = evaluator;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<Integer> removedJobs = new ArrayList<>();
        double currentCmax = solution.cmax;
        
        for (int i = 0; i < numToRemove; i++) {
            List<Integer> allJobs = solution.getAllJobIds();
            if (allJobs.isEmpty()) {
                break;
            }
            
            int worstJob = -1;
            double maxCmaxReduction = -Double.MAX_VALUE;
            
            // 对每个作业，评估移除它后的Cmax变化
            for (int jobId : allJobs) {
                ALNSSolution tempSol = solution.copy();
                tempSol.removeJob(jobId);
                double newCmax = evaluator.evaluate(tempSol);
                double cmaxReduction = currentCmax - newCmax;
                
                if (cmaxReduction > maxCmaxReduction) {
                    maxCmaxReduction = cmaxReduction;
                    worstJob = jobId;
                }
            }
            
            if (worstJob != -1) {
                removedJobs.add(worstJob);
                solution.removeJob(worstJob);
                currentCmax = evaluator.evaluate(solution);
            }
        }
        
        return removedJobs;
    }
    
    @Override
    public String getName() {
        return "WRJ";
    }
}
