package ProgramEntity;

import ProblemFrame.Batch;

import java.util.List;

public class BatchOperation {
    public int batchNo;
    List<Solution> batchPlan;
    public int task;
    public int startTime;
    public int endTime;
    public int aStartTime;//预计最早开工时间
    public int machineNo;
    public int span;
    public String id;

    public BatchOperation() {
        batchNo = -1;
        task = -1;
        startTime = -1;
        endTime = -1;
        aStartTime = -1;
        machineNo = -1;
    }

    public BatchOperation(String id, int span, int machine, int job) {
        this.id = id;
        this.span = span;
        this.machineNo = machine;
        this.batchNo = job;
    }

    public BatchOperation(int machine, int job, int start, int end, int task, List<Solution> solutions) {
        this.id = "J" + Integer.toString(job) + "T" + Integer.toString(task);
        this.span = end - start;
        this.machineNo = machine;
        this.batchNo = job;
        this.task = task;
        this.startTime = start;
        this.endTime = end;
        this.batchPlan = solutions;
    }


    public void initBatchOperation() {
        batchNo = -1;
        task = -1;
        batchPlan = null;
        startTime = -1;
        endTime = -1;
        aStartTime = -1;
        machineNo = -1;
    }

    public String toString(){
        return " " + "J" + this.batchNo + ";" +
                "T" + this.task + ";" +
                " s:" + this.startTime +";"+
                " e:" + this.endTime +" ";
    }

    public boolean equals(BatchOperation op) {
        return this.batchNo == op.batchNo && this.task == op.task;
    }
}
