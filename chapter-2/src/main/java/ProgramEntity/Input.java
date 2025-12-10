package ProgramEntity;

import ProgramEntity.Machine.BathchMachine;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Input {
    private File file;

    public Input(File file) {
        this.file = file;
    }

    /**
     * @return the problem description which has been arranged
     */
    public Problem getProblemDesFromFile() {
        Problem input = new Problem();
        BufferedReader reader = getBufferedReader(file);
        String prodesStrArr[] = null;
        double proDesMatrix[][] = null;
        String proDesString;
        int[] operationCountArr = null;
        int[] machineCountArr = null;
        List<Integer> operationCountList = new ArrayList<Integer>();// 存储每个工序的备选机器数目
        try {
            //第一行为问题规模 分别为机器总数，打印件总数
            String firstLine = reader.readLine();
            input.setMachineCount(Integer.valueOf(firstLine.split("\\s+")[0]));
            input.setJobCount(Integer.valueOf(firstLine.split("\\s+")[1]));
            int jobNum = input.getJobCount();// 工件数
            int machineNum = input.getMachineCount();// 机器数
            //第二行为打印机器数 每个打印机器分别由 name L W H printH recordTime parpreTime
            String printMachineStrArr[] = reader.readLine().split("\\s+");
            System.out.println("打印机数：" + printMachineStrArr[0]);
            int printMachineCount = Integer.valueOf(printMachineStrArr[0]);
            input.setPrintMachineCount(printMachineCount);
            String batchMachineStrArrArr[] = reader.readLine().split("\\s");
            System.out.println("批处理机器数：" + batchMachineStrArrArr[0]);
            int batchMachineCount = Integer.valueOf(batchMachineStrArrArr[0]);
            input.setBatchMachineCount(Integer.valueOf(batchMachineStrArrArr[0]));
            Machine[] machines = new Machine[printMachineCount + batchMachineCount];
            int count1 = 1;
            for (int i = 0; i < printMachineCount; i++) {
                machines[i] = new PrintMachine(String.valueOf(count1++), Double.valueOf(printMachineStrArr[count1++]), Double.valueOf(printMachineStrArr[count1++]), Double.valueOf(printMachineStrArr[count1++]), Double.valueOf(printMachineStrArr[count1++]),
                        Double.valueOf(printMachineStrArr[count1++]), Double.valueOf(printMachineStrArr[count1++]));
            }
            count1 = 1;
            for (int i = printMachineCount; i < printMachineCount + batchMachineCount; i++) {
                machines[i] = new BathchMachine(String.valueOf(count1++), Double.valueOf(batchMachineStrArrArr[count1++]));
            }
            input.setMachines(machines);
//            proDesString = reader.readLine();
//            String proDesArr[] = proDesString.split("\\s+");
//            int jobNum = Integer.valueOf(proDesArr[0]);// 工件数
//            int machineNum = Integer.valueOf(proDesArr[1]);// 机器数
            operationCountArr = new int[jobNum];
//            input.setJobCount(jobNum);
//            input.setMachineCount(machineNum);
            prodesStrArr = new String[jobNum];
            int count = 0;// Calculate how many orders in the problem 总工序数
            int index = 0;// store the index of first blank 标记第一个空格位置
            int maxOperationCount = 0, tempCount = 0;
            // find the max operation count of the job arrays 找出工序最多者
            for (int i = 0; i < jobNum; i++) {
                prodesStrArr[i] = reader.readLine().trim();
                index = prodesStrArr[i].indexOf(' ');
                tempCount = Integer.valueOf(prodesStrArr[i].substring(0, index));//每个工件的工序数
                count += tempCount;
                if (maxOperationCount < tempCount)
                    maxOperationCount = tempCount;
            }
            int[][] operationToIndex = new int[jobNum][maxOperationCount];// 用来存储i工件j工序所对应的problemDesMatrix[][]的index
            input.setMaxOperationCount(maxOperationCount);
            proDesMatrix = new double[count][];
            String opeationDesArr[];
            int operationCount = 0;
            int operationTotalIndex = 0;
            int selectedMachineCount = 0;
            int machineNo = 0, operationTime = 0;
            proDesMatrix[0] = new double[machineNum];
            Item[] items = new Item[jobNum];
            for (int i = 0; i < jobNum; i++) {
                opeationDesArr = prodesStrArr[i].split("\\s+");
                //每一个工件开始的前三个分别为该工件的L W H
                items[i] = new Item(String.valueOf(i), Double.valueOf(opeationDesArr[1]), Double.valueOf(opeationDesArr[2]), Double.valueOf(opeationDesArr[3]));
                // the opeartion count of every job 每个工件的工序数
                operationCount = Integer.valueOf(opeationDesArr[4]);
                operationCountArr[i] = operationCount;
                int k = 5;
                for (int j = 0; j < operationCount; j++) {
                    if (k < opeationDesArr.length) {
                        selectedMachineCount = Integer.valueOf(opeationDesArr[k++]);
                        // 存储每个工序的备选机器数目
                        for (int m = 0; m < selectedMachineCount; m++) {
                            machineNo = Integer.valueOf(opeationDesArr[k++]);// 机器编号
                            operationTime = Integer.valueOf(opeationDesArr[k++]);// 加工时间
                            proDesMatrix[operationTotalIndex][machineNo - 1] = operationTime;// 保存在matrix中
                        }
                        operationCountList.add(selectedMachineCount);
                    }
                    // 用来存储i工件j工序所对应的problemDesMatrix[][]的index
                    operationToIndex[i][j] = operationTotalIndex;
                    operationTotalIndex++;
                    if (operationTotalIndex < count) {
                        proDesMatrix[operationTotalIndex] = new double[machineNum];
                    }
                }
            }
            int listSize = operationCountList.size();
            machineCountArr = new int[listSize];
            for (int i = 0; i < listSize; i++)
                machineCountArr[i] = operationCountList.get(i);
            input.setMachineCountArr(machineCountArr);
            input.setProDesMatrix(proDesMatrix);
            input.setTotalOperationCount(proDesMatrix.length);
            input.setOperationToIndex(operationToIndex);
            input.setItems(items);
            input.setOperationCountArr(operationCountArr);
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return input;
    }

    /**
     * @param file a .txt file which contains the time and order information
     * @return BufferedReader of the file
     */
    private BufferedReader getBufferedReader(File file) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return reader;
    }

    /**
     * @param input        the problem description which has been arranged
     * @param prodesMatrix the problem description which has been arranged
     */
    public void storeProdesInfoToDisk(Problem input, int prodesMatrix[][]) {
        int operationCountofEveryJobArr[] = input.getOperationCountArr();
        int len = operationCountofEveryJobArr.length;
        int sum = 0;
        for (int num : operationCountofEveryJobArr)
            sum += num;
        System.out.println(sum);
        File file = new File("proDesMatrixPro1.txt");
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            int index = 0, j = 0, i = 0;
            for (i = 0; i < len; i++) {
                for (j = 0; j < operationCountofEveryJobArr[i]; j++)
                    writer.write((index + 1) + "-(" + i + "," + (j + 1) + "):" + Arrays.toString(prodesMatrix[index++])
                            + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
