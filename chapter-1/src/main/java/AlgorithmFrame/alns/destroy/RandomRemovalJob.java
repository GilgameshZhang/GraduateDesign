package AlgorithmFrame.alns.destroy;

import AlgorithmFrame.alns.ALNSJob;
import AlgorithmFrame.alns.ALNSMachine;
import AlgorithmFrame.alns.ALNSSolution;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * RRJ - Random Removal Job
 * 随机移除作业
 */
public class RandomRemovalJob implements DestroyOperator {
    private Random random;
    
    public RandomRemovalJob(Random random) {
        this.random = random;
    }
    
    @Override
    public List<Integer> destroy(ALNSSolution solution, ALNSJob[] jobs, ALNSMachine[] machines, int numToRemove) {
        List<Integer> allJobs = solution.getAllJobIds();
        List<Integer> removedJobs = new ArrayList<>();
        
        // 随机选择要移除的作业
        Collections.shuffle(allJobs, random);
        for (int i = 0; i < Math.min(numToRemove, allJobs.size()); i++) {
            removedJobs.add(allJobs.get(i));
        }
        
        // 从解中移除
        for (int jobId : removedJobs) {
            solution.removeJob(jobId);
        }
        
        return removedJobs;
    }
    
    @Override
    public String getName() {
        return "RRJ";
    }
}
