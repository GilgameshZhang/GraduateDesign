package ProgramEntity;

public class Job {
    public int index;// 工件编号 jobNo
    public int opsNr;// 工序数 procedureNo
    public int[] opsIndex;// 工件工序对应的index
    public int[] opsMacNr;// 工序对应备选机器数

    public Job(int index, int opsNr, int[] opsIndex, int[] opsMacNr) {
        this.index = index;
        this.opsNr = opsNr;
        this.opsIndex = opsIndex;
        this.opsMacNr = opsMacNr;
    }
    
    /**
     * 获取该工件的所有Job信息（用于问题实例生成）
     */
    public static Job[] getJobs(Problem problem) {
        int jobCount = problem.getJobCount();
        int[] operationCountArr = problem.getOperationCountArr();
        int[] machineCountArr = problem.getMachineCountArr();
        int[][] operationToIndex = problem.getOperationToIndex();
        
        Job[] jobs = new Job[jobCount];
        int totalOperIndex = 0;
        
        for (int i = 0; i < jobCount; i++) {
            int opsNr = operationCountArr[i];
            int[] opsIndex = new int[opsNr];
            int[] opsMacNr = new int[opsNr];
            
            for (int j = 0; j < opsNr; j++) {
                opsIndex[j] = operationToIndex[i][j];
                opsMacNr[j] = machineCountArr[totalOperIndex++];
            }
            
            jobs[i] = new Job(i, opsNr, opsIndex, opsMacNr);
        }
        
        return jobs;
    }
}

