package ProgramEntity;

public class Operation {
    public int jobNo;
    public int task;
    public double startTime;
    public double endTime;
    public double aStartTime;//预计最早开工时间
    public int machineNo;
    public double span;
    public String id;

    public Operation() {
        jobNo = -1;
        task = -1;
        startTime = -1;
        endTime = -1;
        aStartTime = -1;
        machineNo = -1;
    }

    public Operation(String id, double span, int machine, int job) {
        this.id = id;
        this.span = span;
        this.machineNo = machine;
        this.jobNo = job;
    }

    public Operation(int machine, int job, double start, double end, int task) {
        this.id = "J" + Integer.toString(job) + "T" + Integer.toString(task);
        this.span = end - start;
        this.machineNo = machine;
        this.jobNo = job;
        this.task = task;
        this.startTime = start;
        this.endTime = end;
    }

    public Operation(Operation o) {
        this.id = o.id;
        this.span = o.span;
        this.jobNo = o.jobNo;
        this.task = o.task;
        this.startTime = o.startTime;
        this.endTime = o.endTime;
        this.aStartTime = o.aStartTime;
        this.machineNo = o.machineNo;
    }

    public void initOperation() {
        jobNo = -1;
        task = -1;
        startTime = -1;
        endTime = -1;
        aStartTime = -1;
        machineNo = -1;
    }

    public String toString(){
        return " " + "J" + this.jobNo + ";" +
                "T" + this.task + ";" +
                " s:" + this.startTime +";"+
                " e:" + this.endTime +" ";
    }

    public boolean equals(Operation op) {
        return this.jobNo == op.jobNo && this.task == op.task;
    }
}
