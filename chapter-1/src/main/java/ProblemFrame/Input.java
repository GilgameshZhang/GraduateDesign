package ProblemFrame;

import java.util.List;

public class Input {
    //机器列表
    public List<Machine> machineList;
    //打印件列表
    public List<Item> itemList;
    //是否允许旋转
    public boolean isRotateEnable;

    public Input() {
    }

    public Input(List<Machine> machineList, List<Item> itemList, boolean isRotateEnable) {
        this.machineList = machineList;
        this.itemList = itemList;
        this.isRotateEnable = isRotateEnable;
    }
}
