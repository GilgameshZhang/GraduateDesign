package util;

import ProblemFrame.Input;
import ProblemFrame.Item;
import ProblemFrame.Machine;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ReadDataUtil {
    public Input getInput(String[] pathList) throws IOException {
        BufferedReader printBufferedReader = new BufferedReader(new FileReader(pathList[0]));
        BufferedReader machineBufferedReader = new BufferedReader(new FileReader(pathList[1]));
        String temp = null;
        Input input = new Input();
        List<Item> itemList = new ArrayList<>();
        List<Machine> machineList = new ArrayList<>();
        int conter = 1;
        while ((temp = machineBufferedReader.readLine()) != null) {
            String[] split = temp.split(" ");
            machineList.add(new Machine(String.valueOf(conter), Double.parseDouble(split[0]),
                    Double.parseDouble(split[1]), Double.parseDouble(split[2]),
                    Double.parseDouble(split[3]), Double.parseDouble(split[4]), Double.parseDouble(split[5])));
            conter++;
        }
        conter = 1;
        while ((temp = printBufferedReader.readLine()) != null) {
            String[] split = temp.split(" ");
            itemList.add(new Item(String.valueOf(conter), Double.parseDouble(split[0]),
                    Double.parseDouble(split[1]), Double.parseDouble(split[2])));
            conter++;
        }
        return new Input(machineList, itemList, true);
    }
    
    /**
     * 静态方法：读取数据
     * @param machinePath 机器文件路径
     * @param itemPath 零件文件路径
     * @param isRotateEnable 是否允许旋转
     * @return Input对象
     * @throws IOException
     */
    public static Input readData(String machinePath, String itemPath, boolean isRotateEnable) throws IOException {
        ReadDataUtil util = new ReadDataUtil();
        String[] pathList = {itemPath, machinePath};
        return util.getInput(pathList);
    }
}
