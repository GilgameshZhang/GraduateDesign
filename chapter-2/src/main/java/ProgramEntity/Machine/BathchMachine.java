package ProgramEntity.Machine;

import java.util.List;

public class BathchMachine extends Machine {

    public double processingTime;

    public BathchMachine() {
    }

    public BathchMachine(String name, double processingTime) {
        super(name);
        this.processingTime = processingTime;
    }
    
    @Override
    public String toString() {
        return "批处理机[加工时间:" + processingTime + "]";
    }
}
