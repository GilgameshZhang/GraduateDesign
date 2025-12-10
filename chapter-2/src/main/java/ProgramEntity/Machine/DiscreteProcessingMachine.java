package ProgramEntity.Machine;

import java.util.List;

public class DiscreteProcessingMachine extends Machine {
    public List<Double> startTime;
    //各批次加工结束时间
    public List<Double> endTime;

    public DiscreteProcessingMachine() {
    }

    public DiscreteProcessingMachine(String name, List<Double> startTime, List<Double> endTime) {
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
