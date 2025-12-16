package ProgramEntity;

import ProgramEntity.Machine.Machine;

public class Problem {
    //Problem describe
    private int[] machineCountArr;// how many machines can choose for every operation
    private int[] operationCountArr;// how many operations for every job
    private double proDesMatrix[][];// the machine no and time for every operation 第i道工序在第j台机器上的时间
    private int machineCount; // total machine count
    private int jobCount; // total job count
    private int maxOperationCount = 0;// the max operation count for the whole job
    private int totalOperationCount = 0;// the total operation count for the whole job
    private int[][] operationToIndex;// the index of some operation of some job 第i个工件第j道工序对应的index
    private Machine[] machines;// machine list
    private Item[] items;// item list
    private int printMachineCount;// the machine count for printing
    private int batchMachineCount;

    public int getBatchMachineCount() {
        return batchMachineCount;
    }

    public void setBatchMachineCount(int batchMachineCount) {
        this.batchMachineCount = batchMachineCount;
    }

    public int[] getMachineCountArr() {
        return machineCountArr;
    }

    public void setMachineCountArr(int[] machineCountArr) {
        this.machineCountArr = machineCountArr;
    }

//	public Random getRandom()
//	{
//		return random;
//	}

    public int[] getOperationCountArr() {
        return operationCountArr;
    }

    public void setOperationCountArr(int[] operationCountArr) {
        this.operationCountArr = operationCountArr;
    }

    public int[][] getOperationToIndex() {
        return operationToIndex;
    }

    public void setOperationToIndex(int[][] operationToIndex) {
        this.operationToIndex = operationToIndex;
    }

    public int getMaxOperationCount() {
        return maxOperationCount;
    }

    public void setMaxOperationCount(int maxOperationCount) {
        this.maxOperationCount = maxOperationCount;
    }

    public double[][] getProDesMatrix() {
        return proDesMatrix;
    }

    public void setProDesMatrix(double[][] proDesMatrix) {
        this.proDesMatrix = proDesMatrix;
    }

    public int getMachineCount() {
        return machineCount;
    }

    public void setMachineCount(int machineCount) {
        this.machineCount = machineCount;
    }

    public int getJobCount() {
        return jobCount;
    }

    public void setJobCount(int jobCount) {
        this.jobCount = jobCount;
    }

    public int getTotalOperationCount() {
        return totalOperationCount;
    }

    public void setTotalOperationCount(int totalOperationCount) {
        this.totalOperationCount = totalOperationCount;
    }

    public Machine[] getMachines() {
        return machines;
    }

    public void setMachines(Machine[] machines) {
        this.machines = machines;
    }

    public Item[] getItems() {
        return items;
    }

    public void setItems(Item[] items) {
        this.items = items;
    }

    public int getPrintMachineCount() {
        return printMachineCount;
    }

    public void setPrintMachineCount(int printMachineCount) {
        this.printMachineCount = printMachineCount;
    }
    
    /**
     * 打印问题模型的详细信息，用于调试
     */
    public void printProblemInfo() {
        System.out.println("\n" + "============================");
        System.out.println("                    问题模型详细信息");
        System.out.println("=======================================");
        
        // 基本信息
        System.out.println("\n【基本信息】");
        System.out.println("  机器总数: " + machineCount);
        System.out.println("  打印机数: " + printMachineCount);
        System.out.println("  批处理机数: " + batchMachineCount);
        System.out.println("  离散加工机数: " + (machineCount - printMachineCount - batchMachineCount));
        System.out.println("  工件总数: " + jobCount);
        System.out.println("  总工序数: " + totalOperationCount);
        System.out.println("  最大工序数: " + maxOperationCount);
        
        // 工件信息
        System.out.println("\n【工件信息】");
        if (items != null) {
            for (int i = 0; i < items.length; i++) {
                Item item = items[i];
                System.out.println("  工件" + i + ": name=" + item.name + 
                    ", 尺寸(L×W×H)=" + item.l + "×" + item.w + "×" + item.h);
            }
        }
        
        // 每个工件的工序数
        System.out.println("\n【工件工序数】");
        if (operationCountArr != null) {
            for (int i = 0; i < operationCountArr.length; i++) {
                System.out.println("  工件" + i + ": " + operationCountArr[i] + "道工序 " +
                    "(打印1 + 批处理1 + 离散" + (operationCountArr[i] - 2) + ")");
            }
        }
        
        // 工序到索引的映射
        System.out.println("\n【工序索引映射 operationToIndex[job][oper]】");
        if (operationToIndex != null) {
            for (int i = 0; i < operationToIndex.length; i++) {
                System.out.print("  工件" + i + ": ");
                for (int j = 0; j < operationCountArr[i]; j++) {
                    System.out.print("[工序" + j + "→索引" + operationToIndex[i][j] + "] ");
                }
                System.out.println();
            }
        }
        
        // 每道工序的备选机器数
        System.out.println("\n【备选机器数 machineCountArr】");
        if (machineCountArr != null) {
            System.out.print("  ");
            for (int i = 0; i < machineCountArr.length; i++) {
                System.out.print("工序" + i + ":" + machineCountArr[i] + "台 ");
                if ((i + 1) % 10 == 0) System.out.print("\n  ");
            }
            System.out.println();
        }
        
        // 加工时间矩阵
        System.out.println("\n【加工时间矩阵 proDesMatrix[工序][机器]】");
        if (proDesMatrix != null) {
            System.out.print("         ");
            for (int m = 0; m < machineCount; m++) {
                System.out.printf("机器%-3d ", m + 1);
            }
            System.out.println();
            for (int i = 0; i < proDesMatrix.length; i++) {
                System.out.printf("  工序%-2d: ", i);
                for (int j = 0; j < proDesMatrix[i].length; j++) {
                    if (proDesMatrix[i][j] > 0) {
                        System.out.printf("%-7.1f ", proDesMatrix[i][j]);
                    } else {
                        System.out.print("  -     ");
                    }
                }
                System.out.println();
            }
        }
        
        // 机器信息
        System.out.println("\n【机器信息】");
        if (machines != null) {
            for (int i = 0; i < machines.length; i++) {
                System.out.println("  机器" + (i + 1) + ": " + machines[i].getClass().getSimpleName() + 
                    " - " + machines[i].toString());
            }
        }
        
        System.out.println("\n" + "===================================");
    }
}
