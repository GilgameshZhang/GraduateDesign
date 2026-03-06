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
    
    // 能耗参数（原始字符串，供EnergyAwareInput解析）
    private String printerEnergyParams;
    private String batchEnergyParams;
    private String discreteEnergyParams;

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
     * 获取工件数组（用于初始化染色体）
     */
    public Job[] getJobs() {
        return Job.getJobs(this);
    }
    
    // ========== 能耗参数的getter/setter ==========
    
    public String getPrinterEnergyParams() {
        return printerEnergyParams;
    }
    
    public void setPrinterEnergyParams(String printerEnergyParams) {
        this.printerEnergyParams = printerEnergyParams;
    }
    
    public String getBatchEnergyParams() {
        return batchEnergyParams;
    }
    
    public void setBatchEnergyParams(String batchEnergyParams) {
        this.batchEnergyParams = batchEnergyParams;
    }
    
    public String getDiscreteEnergyParams() {
        return discreteEnergyParams;
    }
    
    public void setDiscreteEnergyParams(String discreteEnergyParams) {
        this.discreteEnergyParams = discreteEnergyParams;
    }
    
    /**
     * 检查是否包含能耗参数
     */
    public boolean hasEnergyParams() {
        return printerEnergyParams != null || 
               batchEnergyParams != null || 
               discreteEnergyParams != null;
    }
}

