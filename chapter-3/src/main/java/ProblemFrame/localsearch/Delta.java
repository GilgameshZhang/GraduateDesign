package ProblemFrame.localsearch;

import ProgramEntity.Operation;
import ProgramEntity.Solution;
import java.util.*;

/**
 * Delta快照类 - 用于增量解码和回滚
 * 
 * 记录局部搜索算子修改的局部状态，支持：
 * - 增量解码：只重算受影响的部分
 * - 事务回滚：拒绝邻域解时恢复原状态
 * 
 * @author AI Assistant
 * @version 1.0
 */
public class Delta {
    
    // ==================== 受影响的实体集合 ====================
    
    /** 受影响的工件集合（jobNo） */
    public Set<Integer> affectedJobs;
    
    /** 受影响的工序集合（全局工序索引） */
    public Set<Integer> affectedOps;
    
    /** 受影响的打印机集合（machineNo，0-based） */
    public Set<Integer> affectedPrinters;
    
    /** 受影响的离散机器集合（machineNo，0-based） */
    public Set<Integer> affectedMachines;
    
    // ==================== 打印阶段快照 ====================
    
    /**
     * 打印机批次列表快照
     * Key: 打印机编号（0-based）
     * Value: 该打印机的批次列表（深拷贝）
     */
    public Map<Integer, List<Solution>> printerBatchSnapshots;
    
    /**
     * 工件的打印完成时间快照
     * Key: 工件编号（0-based）
     * Value: 打印完成时刻（releaseToPost）
     */
    public Map<Integer, Double> jobReleaseToPrintSnapshot;
    
    // ==================== 后处理阶段快照 ====================
    
    /**
     * 后处理机器队列快照
     * Key: 后处理机器编号（0-based）
     * Value: 工件队列（深拷贝）
     */
    public Map<Integer, List<Integer>> postMachineQueueSnapshot;
    
    /**
     * 工件的后处理完成时间快照
     * Key: 工件编号（0-based）
     * Value: 后处理完成时刻（releaseToDisc）
     */
    public Map<Integer, Double> jobReleaseToPostSnapshot;
    
    // ==================== 离散阶段快照 ====================
    
    /**
     * 离散机器工序队列快照
     * Key: 离散机器编号（0-based）
     * Value: 工序队列（Operation列表深拷贝）
     */
    public Map<Integer, List<Operation>> discreteMachineQueueSnapshot;
    
    /**
     * 工序的开始和结束时间快照
     * Key: 工序的全局索引（jobNo * maxOps + opNo）
     * Value: [startTime, endTime]
     */
    public Map<Integer, double[]> operationTimeSnapshot;
    
    // ==================== 能耗快照 ====================
    
    /**
     * 机器能耗统计快照（如果需要）
     * 用于回滚能耗计算状态
     */
    public Map<Integer, MachineEnergySnapshot> energySnapshots;
    
    /**
     * 总能耗快照
     */
    public double totalEnergySnapshot;
    
    /**
     * 总Cmax快照
     */
    public double cmaxSnapshot;
    
    // ==================== 构造函数 ====================
    
    public Delta() {
        this.affectedJobs = new HashSet<>();
        this.affectedOps = new HashSet<>();
        this.affectedPrinters = new HashSet<>();
        this.affectedMachines = new HashSet<>();
        
        this.printerBatchSnapshots = new HashMap<>();
        this.jobReleaseToPrintSnapshot = new HashMap<>();
        this.postMachineQueueSnapshot = new HashMap<>();
        this.jobReleaseToPostSnapshot = new HashMap<>();
        this.discreteMachineQueueSnapshot = new HashMap<>();
        this.operationTimeSnapshot = new HashMap<>();
        this.energySnapshots = new HashMap<>();
        
        this.totalEnergySnapshot = 0.0;
        this.cmaxSnapshot = 0.0;
    }
    
    // ==================== 便捷方法 ====================
    
    /**
     * 标记一个工件受影响
     */
    public void markJobAffected(int jobNo) {
        affectedJobs.add(jobNo);
    }
    
    /**
     * 标记一个打印机受影响
     */
    public void markPrinterAffected(int printerNo) {
        affectedPrinters.add(printerNo);
    }
    
    /**
     * 标记一个离散机器受影响
     */
    public void markMachineAffected(int machineNo) {
        affectedMachines.add(machineNo);
    }
    
    /**
     * 快照打印机的批次列表
     */
    public void snapshotPrinterBatches(int printerNo, List<Solution> batches) {
        // 深拷贝批次列表
        List<Solution> copy = new ArrayList<>();
        if (batches != null) {
            for (Solution batch : batches) {
                // 手动复制所有字段
                Solution batchCopy = new Solution(batch.placeItemList, batch.maxG, batch.totalS, batch.rate);
                batchCopy.startTime = batch.startTime;
                batchCopy.endTime = batch.endTime;
                copy.add(batchCopy);
            }
        }
        printerBatchSnapshots.put(printerNo, copy);
    }
    
    /**
     * 快照工件的打印完成时间
     */
    public void snapshotJobReleaseToPrint(int jobNo, double releaseTime) {
        jobReleaseToPrintSnapshot.put(jobNo, releaseTime);
    }
    
    /**
     * 快照工件的后处理完成时间
     */
    public void snapshotJobReleaseToPost(int jobNo, double releaseTime) {
        jobReleaseToPostSnapshot.put(jobNo, releaseTime);
    }
    
    /**
     * 快照离散机器的工序队列
     */
    public void snapshotMachineOperations(int machineNo, List<Operation> operations) {
        // 深拷贝工序列表
        List<Operation> copy = new ArrayList<>();
        if (operations != null) {
            for (Operation op : operations) {
                copy.add(new Operation(op));
            }
        }
        discreteMachineQueueSnapshot.put(machineNo, copy);
    }
    
    /**
     * 快照工序的时间
     */
    public void snapshotOperationTime(int jobNo, int opNo, double startTime, double endTime) {
        int key = jobNo * 100 + opNo;  // 简单的全局索引
        operationTimeSnapshot.put(key, new double[]{startTime, endTime});
    }
    
    /**
     * 检查是否有任何受影响的实体
     */
    public boolean hasAffectedEntities() {
        return !affectedJobs.isEmpty() || 
               !affectedPrinters.isEmpty() || 
               !affectedMachines.isEmpty();
    }
    
    /**
     * 获取受影响实体数量的统计信息
     */
    public String getAffectedSummary() {
        return String.format("受影响: Jobs=%d, Printers=%d, Machines=%d, Ops=%d",
            affectedJobs.size(),
            affectedPrinters.size(),
            affectedMachines.size(),
            affectedOps.size());
    }
}
