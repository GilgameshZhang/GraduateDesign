import AlgorithmFrame.machineChoice.ga.BatchGa;
import AlgorithmFrame.machineChoice.ga.BatchGaAblation;
import ProblemFrame.Input;
import ProblemFrame.Result;
import util.ReadDataUtil;
import util.ExperimentResultWriter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 田口实验运行器（第一章 GA-TS 参数校准）
 * 因素：种群数量、交叉概率、变异概率、局部搜索次数、禁忌搜索迭代次数、领域大小（禁忌表长度）
 * 算例：2M80J-01；每试验号运行10次，每次5分钟；多线程；输出格式与消融实验一致
 * 支持暂停/恢复：进度保存在 taguchi_progress.csv，再次运行同一输出目录时会跳过已完成条目并继续。
 */
public class TaguchiExperiment {

    /** 进度文件名（相对 baseOut），用于暂停/恢复 */
    static final String PROGRESS_FILE = "taguchi_progress.csv";

    /** 6 因素 3 水平：水平索引 0/1/2 对应水平 1/2/3 */
    static final int[] POP_SIZE_LEVELS = { 50, 100, 150 };
    static final double[] CROSSOVER_LEVELS = { 0.7, 0.8, 0.9 };
    static final double[] MUTATION_LEVELS = { 0.1, 0.15, 0.2 };
    static final int[] LOCAL_SEARCH_N_LEVELS = {10, 20, 30};   // 局部搜索次数 decodeMaxN
    static final int[] TABU_ITER_LEVELS = {50, 100, 150};     // 禁忌搜索迭代 decodeMaxGen
    static final int[] TABU_SIZE_LEVELS = {10, 20, 30};       // 领域大小/禁忌表长度 decodeTabuSize

    /** L27(3^13) 正交表取前 6 列，27 次试验（标准田口 L27 前 6 列） */
    static int[][] L27_FIRST_6_COLUMNS = buildL27First6Columns();

    static int[][] buildL27First6Columns() {
        // 标准 L27 正交表前 6 列（参见 Taguchi L27(3^13)）
        int[][] L = new int[][] {
            {1,1,1,1,1,1}, {1,1,1,1,2,2}, {1,1,1,1,3,3},
            {1,2,2,2,1,1}, {1,2,2,2,2,2}, {1,2,2,2,3,3},
            {1,3,3,3,1,1}, {1,3,3,3,2,2}, {1,3,3,3,3,3},
            {2,1,2,3,1,2}, {2,1,2,3,2,3}, {2,1,2,3,3,1},
            {2,2,3,1,1,2}, {2,2,3,1,2,3}, {2,2,3,1,3,1},
            {2,3,1,2,1,2}, {2,3,1,2,2,3}, {2,3,1,2,3,1},
            {3,1,3,2,1,3}, {3,1,3,2,2,1}, {3,1,3,2,3,2},
            {3,2,1,3,1,3}, {3,2,1,3,2,1}, {3,2,1,3,3,2},
            {3,3,2,1,1,3}, {3,3,2,1,2,1}, {3,3,2,1,3,2}
        };
        return L;
    }

    /** 单次试验的参数组合 */
    static class TaguchiTrial {
        int trialNo;
        int popSize;
        double crossoverRate;
        double mutationRate;
        int localSearchN;
        int tabuIter;
        int tabuSize;

        TaguchiTrial(int trialNo, int[] levels) {
            this.trialNo = trialNo;
            this.popSize = POP_SIZE_LEVELS[levels[0] - 1];
            this.crossoverRate = CROSSOVER_LEVELS[levels[1] - 1];
            this.mutationRate = MUTATION_LEVELS[levels[2] - 1];
            this.localSearchN = LOCAL_SEARCH_N_LEVELS[levels[3] - 1];
            this.tabuIter = TABU_ITER_LEVELS[levels[4] - 1];
            this.tabuSize = TABU_SIZE_LEVELS[levels[5] - 1];
        }

        String configName() {
            return String.format("T%02d_pop%d_pc%.1f_pm%.1f_N%d_TS%d_L%d",
                    trialNo, popSize, crossoverRate, mutationRate, localSearchN, tabuIter, tabuSize);
        }
    }

    static class RunResult {
        int trialNo;
        int runId;
        double cmax;
        double avgUtilization;
        long timeMs;
        List<Double> iterationHistory;
    }

    static class TrialStatistics {
        int trialNo;
        TaguchiTrial trial;
        double avgCmax;
        double minCmax;
        double maxCmax;
        double stdDevCmax;
        double avgUtilization;
        double avgTimeMs;
        double snr;  // 望小特性 SNR = -10 * lg(mean(y^2))
    }

    public static void main(String[] args) {
        String baseOut = "chapter-1/src/main/output/田口实验";
        String instanceName = "2M80J";
        if (args.length > 0 && "--table-only".equals(args[0])) {
            new File(baseOut).mkdirs();
            generateCmaxTableFromFiles(baseOut, instanceName);
            return;
        }

        System.out.println("================================================================================");
        System.out.println("                         田口实验（GA-TS 参数校准）");
        System.out.println("================================================================================");
        System.out.println("因素：种群数量、交叉概率、变异概率、局部搜索次数、禁忌搜索迭代次数、领域大小");
        System.out.println("算例：2M80J-01（2台机器，80工件，实例01）");
        System.out.println("设计：L27 正交表，27 个试验号，每号 10 次重复，每次 5 分钟");
        System.out.println("输出：与消融实验一致（实验报告、detailed_results、summary、boxplot、信噪比）");
        System.out.println("================================================================================");
        System.out.println();

        String machinePath = "chapter-1/src/main/resources/Machine/machine_2";
        String itemPath = "chapter-1/src/main/resources/PrintItem/printItem_80/printItem_80_01";
        long timeLimitMs = 5 * 60 * 1000;
        int runsPerTrial = 10;
        int numThreads = Math.min(Runtime.getRuntime().availableProcessors(), 3);

        List<TaguchiTrial> trials = new ArrayList<TaguchiTrial>();
        for (int i = 0; i < 27; i++) {
            trials.add(new TaguchiTrial(i + 1, L27_FIRST_6_COLUMNS[i]));
        }

        new File(baseOut).mkdirs();

        System.setProperty("ablation.useTabuSearch", "true");
        System.setProperty("ablation.useComplexScore", "true");

        Map<Integer, List<RunResult>> allRunResults = Collections.synchronizedMap(new HashMap<Integer, List<RunResult>>());
        for (TaguchiTrial t : trials) {
            allRunResults.put(t.trialNo, Collections.synchronizedList(new ArrayList<RunResult>()));
        }

        int totalRuns = 27 * runsPerTrial;
        int alreadyDone = loadProgress(baseOut, allRunResults);
        if (alreadyDone > 0) {
            System.out.println("[田口] 检测到已有进度，已完成 " + alreadyDone + "/" + totalRuns + "，将跳过并继续。");
        }

        final Object progressLock = new Object();
        AtomicInteger completedInThisRun = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        List<Future<?>> futures = new ArrayList<Future<?>>();

        for (final TaguchiTrial trial : trials) {
            for (int runId = 1; runId <= runsPerTrial; runId++) {
                final int r = runId;
                if (hasResult(allRunResults, trial.trialNo, r)) continue;
                futures.add(executor.submit(new Runnable() {
                    @Override
                    public void run() {
                        RunResult res = runSingle(machinePath, itemPath, instanceName, trial, r, timeLimitMs);
                        if (res != null) {
                            synchronized (progressLock) {
                                appendProgress(baseOut, res);
                                allRunResults.get(trial.trialNo).add(res);
                            }
                        }
                        int done = alreadyDone + completedInThisRun.incrementAndGet();
                        if (done % 30 == 0 || done == totalRuns) {
                            System.out.println("[田口] 进度 " + done + "/" + totalRuns);
                        }
                    }
                }));
            }
        }

        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        executor.shutdown();

        List<TrialStatistics> statsList = new ArrayList<TrialStatistics>();
        for (TaguchiTrial trial : trials) {
            TrialStatistics st = computeTrialStatistics(trial, allRunResults.get(trial.trialNo));
            if (st != null) statsList.add(st);
        }

        saveTrialResults(baseOut, instanceName, trials, allRunResults, statsList);
        saveTaguchiSummary(baseOut, instanceName, statsList);
        printL27TableWithResults(baseOut, instanceName, trials, statsList);
        writeCmaxTable(baseOut, instanceName, trials, allRunResults);

        System.out.println();
        System.out.println("田口实验完成。结果目录: " + baseOut);
    }

    /** 是否已有该 (试验号, RunID) 的结果 */
    private static boolean hasResult(Map<Integer, List<RunResult>> allRunResults, int trialNo, int runId) {
        List<RunResult> list = allRunResults.get(trialNo);
        if (list == null) return false;
        for (RunResult r : list) {
            if (r.runId == runId) return true;
        }
        return false;
    }

    /** 加载进度文件，返回已完成的运行数 */
    private static int loadProgress(String baseOut, Map<Integer, List<RunResult>> allRunResults) {
        File f = new File(baseOut, PROGRESS_FILE);
        if (!f.exists()) return 0;
        BufferedReader br = null;
        int count = 0;
        try {
            br = new BufferedReader(new FileReader(f));
            String line = br.readLine();
            if (line == null || !line.startsWith("TrialNo")) return 0;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String[] parts = line.split(",");
                if (parts.length < 5) continue;
                try {
                    int trialNo = Integer.parseInt(parts[0].trim());
                    int runId = Integer.parseInt(parts[1].trim());
                    double cmax = Double.parseDouble(parts[2].trim());
                    double avgUtil = Double.parseDouble(parts[3].trim());
                    long timeMs = Long.parseLong(parts[4].trim());
                    RunResult rr = new RunResult();
                    rr.trialNo = trialNo;
                    rr.runId = runId;
                    rr.cmax = cmax;
                    rr.avgUtilization = avgUtil;
                    rr.timeMs = timeMs;
                    rr.iterationHistory = null;
                    List<RunResult> list = allRunResults.get(trialNo);
                    if (list != null) list.add(rr);
                    count++;
                } catch (Exception e) {
                    // 跳过格式异常行
                }
            }
        } catch (IOException e) {
            System.err.println("读取进度文件失败: " + e.getMessage());
        } finally {
            if (br != null) try { br.close(); } catch (IOException ignored) {}
        }
        return count;
    }

    /** 追加一条结果到进度文件（调用方需已持有 progressLock） */
    private static void appendProgress(String baseOut, RunResult rr) {
        File f = new File(baseOut, PROGRESS_FILE);
        try {
            boolean exists = f.exists();
            PrintWriter w = new PrintWriter(new FileWriter(f, true));
            if (!exists) w.println("TrialNo,RunID,Cmax,AvgUtilization,TimeMs");
            w.println(String.format("%d,%d,%.4f,%.4f,%d", rr.trialNo, rr.runId, rr.cmax, rr.avgUtilization, rr.timeMs));
            w.close();
        } catch (IOException e) {
            System.err.println("写入进度失败: " + e.getMessage());
        }
    }

    private static RunResult runSingle(String machinePath, String itemPath, String instanceName,
                                       TaguchiTrial trial, int runId, long timeLimitMs) {
        try {
            Input input = ReadDataUtil.readData(machinePath, itemPath, true);
            BatchGa batchGa = new BatchGaAblation(
                    1000000000,
                    trial.popSize,
                    trial.localSearchN,
                    (int)(0.1 * trial.popSize),
                    trial.mutationRate,
                    trial.crossoverRate,
                    input,
                    true,
                    "TabuSearch",
                    trial.tabuIter,
                    10,
                    trial.tabuSize,
                    timeLimitMs,
                    0.5
            );
            long start = System.currentTimeMillis();
            Result result = batchGa.solve();
            long end = System.currentTimeMillis();

            RunResult rr = new RunResult();
            rr.trialNo = trial.trialNo;
            rr.runId = runId;
            rr.cmax = batchGa.bestGenome.cMax;
            rr.avgUtilization = calcAvgUtilization(batchGa);
            rr.timeMs = end - start;
            rr.iterationHistory = result != null ? result.getIreatorList() : null;

            String expName = String.format("田口实验/Trial%02d", trial.trialNo);
            try {
                ExperimentResultWriter writer = new ExperimentResultWriter(expName, runId, instanceName);
                writer.writeExperimentInfo(
                        "田口试验" + trial.trialNo + " - " + trial.configName(),
                        runId, instanceName,
                        1000000, trial.popSize, trial.mutationRate, trial.crossoverRate
                );
                writer.writeFinalResults(
                        batchGa.bestGenome.cMax,
                        end - start,
                        batchGa.t,
                        rr.iterationHistory != null ? rr.iterationHistory : Collections.<Double>emptyList(),
                        batchGa.bestGenome,
                        batchGa.machines
                );
                writer.writeMachineLoads(batchGa.bestGenome);
                writer.close();
            } catch (Exception e) {
                System.err.println("写报告失败: " + e.getMessage());
            }
            return rr;
        } catch (Exception e) {
            System.err.println("运行失败 Trial" + trial.trialNo + " Run" + runId + ": " + e.getMessage());
            return null;
        }
    }

    private static double calcAvgUtilization(BatchGa batchGa) {
        double sum = 0.0;
        int count = 0;
        if (batchGa.bestGenome != null && batchGa.bestGenome.solutions != null) {
            for (ProblemFrame.BatchResult br : batchGa.bestGenome.solutions) {
                if (br != null && br.solutions != null) {
                    for (ProblemFrame.Solution s : br.solutions) {
                        sum += s.rate;
                        count++;
                    }
                }
            }
        }
        return count > 0 ? sum / count : 0.0;
    }

    private static TrialStatistics computeTrialStatistics(TaguchiTrial trial, List<RunResult> results) {
        if (results == null || results.isEmpty()) return null;
        TrialStatistics st = new TrialStatistics();
        st.trialNo = trial.trialNo;
        st.trial = trial;
        double sum = 0, sumSq = 0;
        double sumUtil = 0, sumTime = 0;
        double minC = Double.MAX_VALUE, maxC = -Double.MAX_VALUE;
        for (RunResult r : results) {
            sum += r.cmax;
            sumSq += r.cmax * r.cmax;
            sumUtil += r.avgUtilization;
            sumTime += r.timeMs;
            if (r.cmax < minC) minC = r.cmax;
            if (r.cmax > maxC) maxC = r.cmax;
        }
        int n = results.size();
        st.avgCmax = sum / n;
        st.minCmax = minC;
        st.maxCmax = maxC;
        st.avgUtilization = sumUtil / n;
        st.avgTimeMs = sumTime / n;
        double var = (sumSq / n) - (st.avgCmax * st.avgCmax);
        st.stdDevCmax = var > 0 ? Math.sqrt(var) : 0;
        double meanSq = sumSq / n;
        st.snr = meanSq > 0 ? -10.0 * Math.log10(meanSq) : 0;
        return st;
    }

    private static void saveTrialResults(String baseOut, String instanceName,
                                         List<TaguchiTrial> trials,
                                         Map<Integer, List<RunResult>> allRunResults,
                                         List<TrialStatistics> statsList) {
        String dir = baseOut + "/" + instanceName;
        new File(dir).mkdirs();

        String detailPath = dir + "/taguchi_detailed_results.csv";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(detailPath));
            w.println("TrialNo,TrialConfig,RunID,Cmax,AvgUtilization,TimeMs");
            for (TaguchiTrial t : trials) {
                List<RunResult> list = allRunResults.get(t.trialNo);
                if (list != null) {
                    for (RunResult r : list) {
                        w.println(String.format("%d,%s,%d,%.4f,%.4f,%d", t.trialNo, t.configName(), r.runId, r.cmax, r.avgUtilization, r.timeMs));
                    }
                }
            }
            w.close();
            System.out.println("已保存: " + detailPath);
        } catch (IOException e) {
            System.err.println("保存详细结果失败: " + e.getMessage());
        }

        String boxPath = dir + "/taguchi_boxplot_data.csv";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(boxPath));
            w.println("TrialConfig,Cmax,AvgUtilization,TimeMs");
            for (TaguchiTrial t : trials) {
                List<RunResult> list = allRunResults.get(t.trialNo);
                if (list != null) {
                    for (RunResult r : list) {
                        w.println(String.format("%s,%.4f,%.4f,%.0f", t.configName(), r.cmax, r.avgUtilization, (double) r.timeMs));
                    }
                }
            }
            w.close();
            System.out.println("已保存: " + boxPath);
        } catch (IOException e) {
            System.err.println("保存箱线图数据失败: " + e.getMessage());
        }

        String summaryPath = dir + "/taguchi_summary_statistics.csv";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(summaryPath));
            w.println("TrialNo,TrialConfig,PopSize,CrossoverRate,MutationRate,LocalSearchN,TabuIter,TabuSize,AvgCmax,MinCmax,MaxCmax,StdDev,AvgUtilization,AvgTimeMs,SNR");
            for (TrialStatistics s : statsList) {
                TaguchiTrial t = s.trial;
                w.println(String.format("%d,%s,%d,%.2f,%.2f,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f,%.2f,%.4f",
                        s.trialNo, t.configName(), t.popSize, t.crossoverRate, t.mutationRate,
                        t.localSearchN, t.tabuIter, t.tabuSize,
                        s.avgCmax, s.minCmax, s.maxCmax, s.stdDevCmax, s.avgUtilization, s.avgTimeMs, s.snr));
            }
            w.close();
            System.out.println("已保存: " + summaryPath);
        } catch (IOException e) {
            System.err.println("保存统计摘要失败: " + e.getMessage());
        }
    }

    private static void saveTaguchiSummary(String baseOut, String instanceName, List<TrialStatistics> statsList) {
        String path = baseOut + "/taguchi_global_summary.csv";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path));
            w.println("TrialNo,PopSize,CrossoverRate,MutationRate,LocalSearchN,TabuIter,TabuSize,AvgCmax,MinCmax,MaxCmax,StdDev,SNR");
            for (TrialStatistics s : statsList) {
                TaguchiTrial t = s.trial;
                w.println(String.format("%d,%d,%.2f,%.2f,%d,%d,%d,%.4f,%.4f,%.4f,%.4f,%.4f",
                        s.trialNo, t.popSize, t.crossoverRate, t.mutationRate,
                        t.localSearchN, t.tabuIter, t.tabuSize,
                        s.avgCmax, s.minCmax, s.maxCmax, s.stdDevCmax, s.snr));
            }
            w.close();
            System.out.println("已保存: " + path);
        } catch (IOException e) {
            System.err.println("保存全局汇总失败: " + e.getMessage());
        }

        String reportPath = baseOut + "/taguchi_report.txt";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(reportPath));
            w.println("================================================================================");
            w.println("                    田口实验汇总报告（2M80J-01）");
            w.println("================================================================================");
            w.println("因素：A=种群数量, B=交叉概率, C=变异概率, D=局部搜索次数, E=禁忌搜索迭代, F=领域大小");
            w.println("设计：L27，每试验号10次，每次5分钟；望小特性 SNR = -10*lg(mean(Cmax^2))");
            w.println("================================================================================");
            w.println();
            w.println(String.format("%-6s %-8s %-8s %-8s %-6s %-6s %-6s %-12s %-12s %-10s",
                    "试验号", "PopSize", "Pc", "Pm", "N", "TSIter", "TabuL", "AvgCmax", "MinCmax", "SNR"));
            w.println(repeat("-", 90));
            for (TrialStatistics s : statsList) {
                TaguchiTrial t = s.trial;
                w.println(String.format("%-6d %-8d %-8.2f %-8.2f %-6d %-6d %-6d %-12.2f %-12.2f %-10.4f",
                        s.trialNo, t.popSize, t.crossoverRate, t.mutationRate,
                        t.localSearchN, t.tabuIter, t.tabuSize,
                        s.avgCmax, s.minCmax, s.snr));
            }
            w.println();
            w.println("================================================================================");
            w.close();
            System.out.println("已保存: " + reportPath);
        } catch (IOException e) {
            System.err.println("保存报告失败: " + e.getMessage());
        }
    }

    private static void printL27TableWithResults(String baseOut, String instanceName,
                                                  List<TaguchiTrial> trials, List<TrialStatistics> statsList) {
        Map<Integer, TrialStatistics> map = new HashMap<Integer, TrialStatistics>();
        for (TrialStatistics s : statsList) map.put(s.trialNo, s);
        String path = baseOut + "/" + instanceName + "/taguchi_L27_table.csv";
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path));
            w.println("TrialNo,A_PopSize,B_Crossover,C_Mutation,D_LocalN,E_TabuIter,F_TabuSize,AvgCmax,MinCmax,StdDev,SNR");
            for (TaguchiTrial t : trials) {
                TrialStatistics s = map.get(t.trialNo);
                String line = String.format("%d,%d,%.2f,%.2f,%d,%d,%d",
                        t.trialNo, t.popSize, t.crossoverRate, t.mutationRate, t.localSearchN, t.tabuIter, t.tabuSize);
                if (s != null) {
                    line += String.format(",%.4f,%.4f,%.4f,%.4f", s.avgCmax, s.minCmax, s.stdDevCmax, s.snr);
                } else {
                    line += ",—,—,—,—";
                }
                w.println(line);
            }
            w.close();
            System.out.println("已保存: " + path);
        } catch (IOException e) {
            System.err.println("保存L27表失败: " + e.getMessage());
        }
    }

    /** 生成 Cmax 汇总表：一行一个配置，列为 Run1～Run10 的 Cmax */
    private static void writeCmaxTable(String baseOut, String instanceName,
                                       List<TaguchiTrial> trials, Map<Integer, List<RunResult>> allRunResults) {
        String dir = baseOut + "/" + instanceName;
        new File(dir).mkdirs();
        String path = dir + "/taguchi_cmax_table.csv";
        int runsPerTrial = 10;
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path));
            StringBuilder header = new StringBuilder("TrialNo,TrialConfig");
            for (int r = 1; r <= runsPerTrial; r++) header.append(",Run").append(r);
            w.println(header);
            for (TaguchiTrial t : trials) {
                List<RunResult> list = allRunResults.get(t.trialNo);
                Map<Integer, Double> runIdToCmax = new HashMap<Integer, Double>();
                if (list != null) {
                    for (RunResult rr : list) runIdToCmax.put(rr.runId, rr.cmax);
                }
                StringBuilder row = new StringBuilder();
                row.append(t.trialNo).append(",").append(t.configName());
                for (int runId = 1; runId <= runsPerTrial; runId++) {
                    Double cmax = runIdToCmax.get(runId);
                    row.append(",").append(cmax != null ? String.format("%.4f", cmax) : "");
                }
                w.println(row);
            }
            w.close();
            System.out.println("已保存 Cmax 汇总表: " + path);
        } catch (IOException e) {
            System.err.println("保存 Cmax 汇总表失败: " + e.getMessage());
        }
    }

    /** 仅从已有结果文件生成 Cmax 汇总表（用于程序跑完后单独生成或重新生成） */
    private static void generateCmaxTableFromFiles(String baseOut, String instanceName) {
        String dir = baseOut + "/" + instanceName;
        File detailFile = new File(dir + "/taguchi_detailed_results.csv");
        File progressFile = new File(baseOut + "/" + PROGRESS_FILE);
        Map<Integer, String> trialConfig = new HashMap<Integer, String>();
        Map<Integer, Map<Integer, Double>> trialRunCmax = new HashMap<Integer, Map<Integer, Double>>();
        for (int i = 1; i <= 27; i++) trialRunCmax.put(i, new HashMap<Integer, Double>());

        if (detailFile.exists()) {
            try {
                BufferedReader br = new BufferedReader(new FileReader(detailFile));
                String line = br.readLine();
                if (line == null || !line.contains("TrialNo")) { br.close(); return; }
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] p = line.split(",", -1);
                    if (p.length < 4) continue;
                    try {
                        int trialNo = Integer.parseInt(p[0].trim());
                        String config = p.length > 2 ? p[1].trim() : "";
                        int runId = Integer.parseInt(p[2].trim());
                        double cmax = Double.parseDouble(p[3].trim());
                        trialConfig.put(trialNo, config);
                        trialRunCmax.get(trialNo).put(runId, cmax);
                    } catch (Exception ignored) {}
                }
                br.close();
            } catch (IOException e) {
                System.err.println("读取详细结果失败: " + e.getMessage());
                return;
            }
        } else if (progressFile.exists()) {
            List<TaguchiTrial> trials = new ArrayList<TaguchiTrial>();
            for (int i = 0; i < 27; i++) trials.add(new TaguchiTrial(i + 1, L27_FIRST_6_COLUMNS[i]));
            for (int i = 1; i <= 27; i++) trialConfig.put(i, trials.get(i - 1).configName());
            try {
                BufferedReader br = new BufferedReader(new FileReader(progressFile));
                String line = br.readLine();
                if (line == null || !line.startsWith("TrialNo")) { br.close(); return; }
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    String[] p = line.split(",", -1);
                    if (p.length < 3) continue;
                    try {
                        int trialNo = Integer.parseInt(p[0].trim());
                        int runId = Integer.parseInt(p[1].trim());
                        double cmax = Double.parseDouble(p[2].trim());
                        if (trialRunCmax.containsKey(trialNo)) trialRunCmax.get(trialNo).put(runId, cmax);
                    } catch (Exception ignored) {}
                }
                br.close();
            } catch (IOException e) {
                System.err.println("读取进度失败: " + e.getMessage());
                return;
            }
        } else {
            System.err.println("未找到 taguchi_detailed_results.csv 或 taguchi_progress.csv");
            return;
        }

        new File(dir).mkdirs();
        String path = dir + "/taguchi_cmax_table.csv";
        int runsPerTrial = 10;
        try {
            PrintWriter w = new PrintWriter(new FileWriter(path));
            StringBuilder header = new StringBuilder("TrialNo,TrialConfig");
            for (int r = 1; r <= runsPerTrial; r++) header.append(",Run").append(r);
            w.println(header);
            for (int trialNo = 1; trialNo <= 27; trialNo++) {
                String config = trialConfig.get(trialNo);
                if (config == null) config = "T" + trialNo;
                Map<Integer, Double> runCmax = trialRunCmax.get(trialNo);
                StringBuilder row = new StringBuilder().append(trialNo).append(",").append(config);
                for (int runId = 1; runId <= runsPerTrial; runId++) {
                    Double cmax = runCmax != null ? runCmax.get(runId) : null;
                    row.append(",").append(cmax != null ? String.format("%.4f", cmax) : "");
                }
                w.println(row);
            }
            w.close();
            System.out.println("已生成 Cmax 汇总表: " + path);
        } catch (IOException e) {
            System.err.println("写入 Cmax 汇总表失败: " + e.getMessage());
        }
    }

    private static String repeat(String s, int n) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < n; i++) sb.append(s);
        return sb.toString();
    }
}
