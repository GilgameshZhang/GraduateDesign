package AlgorthmFrame.ga;

import ProblemFrame.Chromosome;
import ProgramEntity.Item;
import ProgramEntity.Machine.Machine;
import ProgramEntity.Machine.PrintMachine;
import ProgramEntity.Problem;
import ProgramEntity.Solution;

import java.util.*;

public class ChromosomeOperation {
    Random r;
    Problem input;

    public ChromosomeOperation(Random r, Problem input) {
        this.r = r;
        this.input = input;
    }

    public Chromosome[] Selection(Chromosome[] parents, double pr) {
        int popNr = parents.length;
        Chromosome[] children = new Chromosome[popNr];

        // 选择策略一：精英选择。
        int num = (int) (pr * popNr);
        ArrayList<Chromosome> p = new ArrayList<>();
        Collections.addAll(p, parents);
        Collections.sort(p);
        for (int i = 0; i < num; i++) {
            children[i] = p.get(i);
        }

        // 选择策略二：锦标赛选择。
        for (int i = num; i < popNr; i++) {
            int n1 = r.nextInt(popNr);
            int n2 = r.nextInt(popNr);
            if (parents[n1].fitness < parents[n2].fitness)
                children[i] = parents[n2];
            else
                children[i] = parents[n1];
        }

//        // 选择策略三：轮盘赌选择。
//        double[] prob = new double[popNr];
//        double sum = 0.0d;
//
//        for (Chromosome i : parents)
//            sum += i.fitness;
//
//        prob[0] = parents[0].fitness / sum;
//        for (int i = 1; i < parents.length; i++) {
//            prob[i] = prob[i - 1] + parents[i].fitness / sum;
//        }
//
//        for (int i = num; i < popNr; i++) {
//            double rand_num = r.nextDouble();
//            for (int j = 0; j < parents.length; j++) {
//                if (prob[j] > rand_num) {
//                    children[i] = parents[j];
//                    break;
//                }
//            }
//        }

        return children;
    }

    // 轮盘赌选择父代个体
    public Chromosome[] Selection2(Chromosome[] parents) {
        int len = parents.length;
        double[] prob = new double[len];
        double sum = 0.0d;
        Chromosome[] chromosomes = new Chromosome[len];

        for (Chromosome i : parents) {
            sum += i.fitness;
        }

        prob[0] = parents[0].fitness / sum;
        for (int i = 1; i < parents.length; i++) {
            prob[i] = prob[i - 1] + parents[i].fitness / sum;
        }

        for (int i = 0; i < len; i++) {
            double rand_num = r.nextDouble();
            for (int j = 0; j < parents.length; j++) {
                if (prob[j] > rand_num) {
                    chromosomes[i] = parents[j];
                    break;
                }
            }
        }

        return chromosomes;
    }


    public void Crossover(Chromosome c1, Chromosome c2) {
        //
        if (r.nextDouble() < 0.5) {
            operSeqCrossoverPOX(c1.gene_OS, c1.gene_MS, c2.gene_OS, c2.gene_OS, 0);
        } else {
//            operSeqCrossoverJBX(c1.gene_OS, c2.gene_OS);
            operSeqCrossoverPOX(c1.gene_OS, c1.gene_MS, c2.gene_OS, c2.gene_OS, 1);
        }
//        machineSeqCrossover(c1.gene_MS, c2.gene_MS);
    }

    public void operSeqCrossoverZLL(int o1[], int o2[]) {
        // ZLL自己尝试的cross方法
        int len = o1.length;
        int jobCount = input.getJobCount();
        int[] operationCountArr = input.getOperationCountArr();

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);

        // 生成随机的交叉起点和终点
        int start = r.nextInt(len);
        int end = r.nextInt(len);
//		start = 3;
//		end = 4;
        int temp = 0;
        if (start > end) {
            temp = start;
            start = end;
            end = temp;
        }

        // 统计
        int[] operCount = new int[jobCount];// 在标记区域内每个工件已经执行的工序数
        Arrays.fill(operCount, 0);
        for (int i = start; i <= end; i++)
            operCount[p1[i]]++;

        // 赋值
        int index = 0;// p2的下标,同时作用于p1的赋值
        for (int i = 0; i < len; i++) {
            if (i >= start && i <= end) {
                o1[i] = p1[i];
            } else {
                while (true) {
                    if (operCount[p2[index]] < operationCountArr[p2[index]]) {
                        o1[i] = p2[index];
                        operCount[p2[index]]++;
                        break;
                    } else {
                        o2[index] = p2[index];
                        index++;
                    }
                }
                o2[index] = p1[i];
                index++;
            }
        }

    }

    public void operSeqCrossoverPOX(int o1[], int o2[], int num) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount);
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);

//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);
        // 分配
        int len = o1.length;
        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(o2, -1);
        if (num == 0) {
            for (int i = 0; i < jobCount; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    p1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    p2[i] = -1;
                }
            }

            int index1 = 0;
            int index2 = 0;
            for (int i = 0; i < jobCount; i++) {
                if (o2[i] == -1) {
                    while (p1[index1] == -1) {
                        index1++;
                    }
                    o2[i] = p1[index1];
                    index1++;
                }
                if (o1[i] == -1) {
                    while (p2[index2] == -1) {
                        index2++;
                    }
                    o1[i] = p2[index2];
                    index2++;
                }
            }
        } else {
            for (int i = jobCount; i < len; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    p1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    p2[i] = -1;
                }
            }

            int index1 = 0;
            int index2 = 0;
            for (int i = jobCount; i < len; i++) {
                if (o2[i] == -1) {
                    while (p1[index1] == -1) {
                        index1++;
                    }
                    o2[i] = p1[index1];
                    index1++;
                }
                if (o1[i] == -1) {
                    while (p2[index2] == -1) {
                        index2++;
                    }
                    o1[i] = p2[index2];
                    index2++;
                }
            }
        }
    }

    public void operSeqCrossoverJBX(int o1[], int o2[]) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);

        int len1 = r.nextInt(jobCount);
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);

//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);

        // 分配
        int len = o1.length;

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(o2, -1);
        for (int i = 0; i < len; i++) {
            if (jobSet1.contains(p1[i])) {
                o1[i] = p1[i];
            } else {
                p1[i] = -1;
            }
            if (jobSet2.contains(p2[i])) {
                o2[i] = p2[i];
            } else {
                p2[i] = -1;
            }
        }


        int index1 = 0;
        int index2 = 0;
        for (int i = 0; i < len; i++) {
            if (o2[i] == -1) {
                while (p1[index1] == -1) {
                    index1++;
                }
                o2[i] = p1[index1];
                index1++;
            }
            if (o1[i] == -1) {
                while (p2[index2] == -1) {
                    index2++;
                }
                o1[i] = p2[index2];
                index2++;
            }
        }

    }

    public void operSeqCrossoverPOX(int o1[], int[] m1, int[] m2, int o2[], int num) {
        // 随机分配工件集
        int jobCount = input.getJobCount();
        ArrayList<Integer> temp = new ArrayList<>();
        for (int i = 0; i < jobCount; i++) {
            temp.add(i);
        }
        Collections.shuffle(temp);
        int len1 = r.nextInt(jobCount);
        List<Integer> jobSet1 = temp.subList(0, len1);
        List<Integer> jobSet2 = temp.subList(len1, jobCount);

//		List<Integer> jobSet1 = new ArrayList<>();
//		jobSet1.add(1);
//		List<Integer> jobSet2 = new ArrayList<>();
//		jobSet2.add(0);
//		jobSet2.add(2);
        // 分配
        int len = o1.length;
        int[] p1 = new int[len];
        int[] p2 = new int[len];
        int[] pm1 = new int[len];
        int[] pm2 = new int[len];
        System.arraycopy(o1, 0, p1, 0, len);
        System.arraycopy(m1, 0, pm1, 0, len);
        System.arraycopy(o2, 0, p2, 0, len);
        System.arraycopy(m2, 0, pm2, 0, len);
        Arrays.fill(o1, -1);
        Arrays.fill(m1, -1);
        Arrays.fill(o2, -1);
        Arrays.fill(m2, -1);
        if (num == 0) {
            for (int i = 0; i < jobCount; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    m1[i] = pm1[i];
                    p1[i] = -1;
                    pm1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    m2[i] = pm2[i];
                    p2[i] = -1;
                    pm2[i] = -1;
                }
            }
            int index1 = 0;
            int index2 = 0;
            for (int i = 0; i < jobCount; i++) {
                if (o2[i] == -1) {
                    while (index1 < p1.length && p1[index1] == -1) {  // ✓ 添加边界检查
                        index1++;
                    }
                    if (index1 < p1.length) {  // ✓ 确保不越界
                        o2[i] = p1[index1];
                        m2[i] = pm1[index1];
                        index1++;
                    }
                }
                if (o1[i] == -1) {
                    while (index2 < p2.length && p2[index2] == -1) {  // ✓ 添加边界检查
                        index2++;
                    }
                    if (index2 < p2.length) {  // ✓ 确保不越界
                        o1[i] = p2[index2];
                        m1[i] = pm2[index2];
                        index2++;
                    }
                }
            }
        } else {
            for (int i = jobCount * 2; i < len; i++) {
                if (jobSet1.contains(p1[i])) {
                    o1[i] = p1[i];
                    m1[i] = pm1[i];
                    p1[i] = -1;
                    pm1[i] = -1;
                }
                if (jobSet1.contains(p2[i])) {
                    o2[i] = p2[i];
                    m2[i] = pm2[i];
                    p2[i] = -1;
                    pm2[i] = -1;
                }
            }

            int index1 = 0;
            int index2 = 0;
            for (int i = jobCount * 2; i < len; i++) {
                if (o2[i] == -1) {
                    while (p1[index1] == -1) {
                        index1++;
                    }
                    o2[i] = p1[index1];
                    m2[i] = pm1[index1];
                    index1++;
                }
                if (o1[i] == -1) {
                    while (p2[index2] == -1) {
                        index2++;
                    }
                    o1[i] = p2[index2];
                    m1[i] = pm2[index2];
                    index2++;
                }
            }
        }
    }


    public void machineSeqCrossover(int m1[], int m2[]) {
        int len = m1.length;

        int[] p1 = new int[len];
        int[] p2 = new int[len];
        System.arraycopy(m1, 0, p1, 0, len);
        System.arraycopy(m2, 0, p2, 0, len);

        // 生成随机的交叉起点和终点
        int start = r.nextInt(len);
        int end = r.nextInt(len);

//		start = 2;
//		end = 5;

        int temp = 0;
        if (start > end) {
            temp = start;
            start = end;
            end = temp;
        }

        for (int i = start; i < end; i++) {
            m1[i] = p2[i];
            m2[i] = p1[i];
        }
    }

    public void Mutation(Chromosome chromosome) {
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            operPrintSeqMutationSwap(chromosome);
            chromosome.gene_MS = printMachineSeqMutation(chromosome);
        } else {
            operSeqMutationSwap(chromosome);
            chromosome.gene_MS = machineSeqMutation(chromosome);
            //operSeqMutationNeighbor(chromosome.gene_OS);
        }


    }

    private void operPrintSeqMutationSwap(Chromosome chromosome) {
        int[] geneOs = chromosome.gene_OS;
        int[] geneMs = chromosome.gene_MS;
        int printCount = input.getJobCount();
        // 随机查找两点
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            int posa = r.nextInt(printCount);
            int posb = r.nextInt(printCount);
            while (posa == posb)
                posb = r.nextInt(printCount);
            int temp;
            int temp1;
            if (posa > posb) {
                temp = posa;
                posa = posb;
                posb = temp;
            }
            temp = geneOs[posa];
            temp1 = geneMs[posa];
            geneOs[posa] = geneOs[posb];
            geneMs[posa] = geneMs[posb];
            geneOs[posb] = temp;
            geneMs[posb] = temp1;
        }
    }

    public void operSeqMutationSwap(Chromosome chromosome) {
        int jobCount = input.getJobCount();
        int[] os = chromosome.gene_OS;
        int[] ms = chromosome.gene_MS;
        int len = os.length;
        // 随机查找两点
        double posibility = r.nextDouble();
        if (posibility < 0.5) {
            int posa = jobCount + r.nextInt(len - jobCount);
            int posb = jobCount + r.nextInt(len - jobCount);
            while (posa == posb)
                posb = r.nextInt(len);
            int temp;
            int temp1;
            if (posa > posb) {
                temp = posa;
                posa = posb;
                posb = temp;
            }
            temp = os[posa];
            temp1 = ms[posa];
            ms[posa] = ms[posb];
            os[posa] = os[posb];
            os[posb] = temp;
            ms[posb] = temp1;
        }
    }

    public void operSeqMutationNeighbor(int[] os) {
        int len = os.length;
        int pos1 = r.nextInt(len);
        int pos2 = r.nextInt(len);
        while (os[pos1] == os[pos2])
            pos2 = r.nextInt(len);
        int pos3 = r.nextInt(len);
        while (os[pos3] == os[pos2] || os[pos3] == os[pos1])
            pos3 = r.nextInt(len);
        ArrayList<Integer> li = new ArrayList<>();
        li.add(os[pos1]);
        li.add(os[pos2]);
        li.add(os[pos3]);
        Collections.shuffle(li);
        os[pos1] = li.get(0);
        os[pos2] = li.get(1);
        os[pos3] = li.get(2);
    }

    //todo 修改打印机器变异的方法
    public int[] printMachineSeqMutation(Chromosome chromosome) {
        List<Solution>[] batchSolution = chromosome.printSolution;
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        Item[] items = input.getItems();
        Machine[] machines = input.getMachines();
        int printCount = input.getJobCount();
        //找到加工时间最短的机器和加工时间最长的机器
        int maxMachineIndex = 0;
        int minMachineIndex = 0;
        for (int i = 0; i < batchSolution.length; i++) {
            List<Solution> machineISolution = batchSolution[i];
            List<Solution> machineMaxSolution = batchSolution[maxMachineIndex];
            List<Solution> machineMinSolution = batchSolution[minMachineIndex];
            if (machineISolution.get(machineISolution.size() - 1).endTime > machineMaxSolution.get(machineMaxSolution.size() - 1).endTime) {
                maxMachineIndex = i;
            }
            if (machineISolution.get(machineISolution.size() - 1).endTime < machineMinSolution.get(machineMinSolution.size() - 1).endTime) {
                minMachineIndex = i;
            }
        }
        PrintMachine maxMachine = (PrintMachine) machines[maxMachineIndex];
        PrintMachine minMachine = (PrintMachine) machines[minMachineIndex];
        //将加工时间最长的机器上的零件分给加工时间最短的机器
        //1. 找到机器对应零件的位置映射map
        Map<Integer, List<Integer>> machineMap = new HashMap<>();
        for (int i = 0; i < printCount; i++) {
            if (!machineMap.containsKey(ms[i])) {
                machineMap.put(ms[i], new ArrayList<>());
            }
            machineMap.get(ms[i]).add(i);
        }
        //2. 随机一个加工时间最长机器上的零件
        while (true) {
            int r1 = r.nextInt(machineMap.get(maxMachineIndex).size());
            if (items[machineMap.get(maxMachineIndex).get(r1)].l > minMachine.L || items[machineMap.get(maxMachineIndex).get(r1)].w > minMachine.W || items[machineMap.get(maxMachineIndex).get(r1)].h > minMachine.H) {
                continue;
            }
            ms[machineMap.get(maxMachineIndex).get(r1)] = minMachineIndex;
            break;
        }
        return ms;
    }

    public int[] machineSeqMutation(Chromosome chromosome) {
        int printCount = input.getJobCount();
        double[][] proDesMatrix = input.getProDesMatrix();
        int[][] operationToIndex = input.getOperationToIndex();
        int[] ms = chromosome.gene_MS;
        int[] os = chromosome.gene_OS;
        //随机生成一个点在printCount到最后一个点之间的点
        int pos = printCount + r.nextInt(os.length - printCount);
        //寻找这个点是第几次碰到的
        int count = 1;
        for (int i = printCount; i <= pos; i++) {
            if (os[i] == os[pos]) {
                count++;
            }
        }
        //找到该点在proDesMatrix中的位置
        int index = operationToIndex[os[pos]][count];
        List<Integer> list = new ArrayList<>();
        //遍历该点所在列
        for (int i = 0; i < proDesMatrix[0].length; i++) {
            //不为Double.MAX_VALUE的点均可以为加工机器，从中随便挑选一个作为新的机器
            if (proDesMatrix[index][i] != Double.MAX_VALUE) {
                list.add(i);
            }
        }
        if (!list.isEmpty()) {
            while (true) {
                int random = r.nextInt(list.size());
                if (ms[pos] == list.get(random)) {
                    continue;
                }
                ms[pos] = list.get(random);
                break;
            }
        } else {
            throw new RuntimeException("没有找到可以的加工机器");
        }
        return ms;
    }
}
