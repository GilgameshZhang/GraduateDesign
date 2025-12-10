package AlgorithmFrame.machineChoice.ga;

import ProblemFrame.*;
import util.ReadDataUtil;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;

public class Start extends Thread{
    int maxGen;
    int populationSize;
    double mutationRate;
    double crossoverRate;
    int tabuSize;
    int maxN;
    int decodeMaxGen;

    public Start(int maxGen, int populationSize, double mutationRate, double crossoverRate, int decodeMaxGen, int tabuSize, int maxN) {
        this.maxGen = maxGen;
        this.populationSize = populationSize;
        this.mutationRate = mutationRate;
        this.crossoverRate = crossoverRate;
        this.tabuSize = tabuSize;
        this.maxN = maxN;
        this.decodeMaxGen = decodeMaxGen;
    }
    @Override
    public void run() {
        String itemPath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\PrintItem\\printItem_80\\printItem_80_01";
        String machinePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\Machine\\machine_2";
        String[] pathList = new String[2];
        pathList[0] = itemPath;
        pathList[1] = machinePath;
        //根据txt中的文件获取input对象
        Input input = null;
        try {
            input = new ReadDataUtil().getInput(pathList);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        //记录算法开始时间
        long startTime = System.currentTimeMillis();
        //实例化遗传算法对象
//        BatchGa batchGa = new BatchGa(100, 20, 5, 10, 0.95, 0.9, input, true, "GA");
//        BatchGa batchGa1 = new BatchGa(100, 20, 5, 5, 0.95, 0.9, input, false, "ACO");
        //创建线程池并执行任务
        BatchGa batchGa2 = new BatchGa(maxGen, populationSize, 5, 5,  mutationRate,  crossoverRate, input, true, "TabuSearch"
                , decodeMaxGen,  tabuSize, maxN);
//        List<BatchResult> batchGenomeList = batchGa.solve();
//        List<BatchResult> batchGenomeList = batchGa1.solve();

        Result result = batchGa2.solve();
        //记录算法结束时间
        long endTime = System.currentTimeMillis();
        System.out.println("------------------------------------------------------------------------------------");
        String Thread = "Thread" + currentThread().getName();
//        System.out.println(Thread + "求解用时:" + (endTime - startTime) / 1000.0 + " s");
        double max = 0.0;
//        for (BatchResult batchResult : result.getSolutionList()) {
//            max = Math.max(max, batchResult.endTimes.get(batchResult.endTimes.size() - 1));
//            //输出画图数据
//            for (Solution solution : batchResult.solutions) {
//                System.out.println("共放置了矩形" + solution.placeItemList.size() + "个");
//                System.out.println("利用率为" + solution.rate);
//                System.out.println("零件最高" + solution.maxG);
//                String[] strings0 = new String[solution.placeItemList.size()];
//                String[] strings1 = new String[solution.placeItemList.size()];
//                String[] strings2 = new String[solution.placeItemList.size()];
//                for (int i = 0; i < solution.placeItemList.size(); i++) {
//                    PlaceItem placeItem = solution.placeItemList.get(i);
//                    strings0[i] = "name:" + placeItem.name;
//                    strings1[i] = "{x:" + placeItem.x + ",y:" + placeItem.y + ",l:" + placeItem.l + ",w:" + placeItem.w + "}";
//                    strings2[i] = placeItem.isRotate ? "1" : "0";
//                }
//                System.out.println("name:" + Arrays.toString(strings0) + ",");
//                System.out.println("data:" + Arrays.toString(strings1) + ",");
//                System.out.println("isRotate:" + Arrays.toString(strings2) + ",");
//            }
//            for(int i = 0; i < batchResult.startTimes.size(); i++) {
//                System.out.println(batchResult.startTimes.get(i) + " " + batchResult.endTimes.get(i));
//            }
//            System.out.println("----------------------------------------------------------------------------------------------");
//        }
//
//        System.out.println(Thread + "最好结果" + max);

        //拼文件名
        String fileName = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\output\\田口实验\\" + Thread + ".txt";
        //Output results to a text file
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(fileName))) {
            for (int i = 0; i < result.getIreatorList().size(); i++) {
                writer.write("第" + (i + 1) + "代 :" + "\t" + result.getIreatorList().get(i) + "\n");
            }
            for (BatchResult batchResult : result.getSolutionList()) {
                max = Math.max(max, batchResult.endTimes.get(batchResult.endTimes.size() - 1));
                // Output drawing data
                for (Solution solution : batchResult.solutions) {
                    writer.write("共放置了矩形: " + solution.placeItemList.size() + "\n");
                    writer.write("利用率为: " + solution.rate + "\n");
                    writer.write("零件最高: " + solution.maxG + "\n");
                    String[] strings0 = new String[solution.placeItemList.size()];
                    String[] strings1 = new String[solution.placeItemList.size()];
                    String[] strings2 = new String[solution.placeItemList.size()];
                    for (int i = 0; i < solution.placeItemList.size(); i++) {
                        PlaceItem placeItem = solution.placeItemList.get(i);
                        strings0[i] = "name:" + placeItem.name;
                        strings1[i] = "{x:" + placeItem.x + ",y:" + placeItem.y + ",l:" + placeItem.l + ",w:" + placeItem.w + "}";
                        strings2[i] = placeItem.isRotate ? "1" : "0";
                    }
                    writer.write("name:" + Arrays.toString(strings0) + ",\n");
                    writer.write("data:" + Arrays.toString(strings1) + ",\n");
                    writer.write("isRotate:" + Arrays.toString(strings2) + ",\n");
                }
                for (int i = 0; i < batchResult.startTimes.size(); i++) {
                    writer.write(batchResult.startTimes.get(i) + " " + batchResult.endTimes.get(i) + "\n");
                }
                writer.write("----------------------------------------------------------------------------------------------\n");
            }
            writer.write("求解用时: " + (endTime - startTime) / 1000.0 + " s\n");
            writer.write("最好结果为: " + max + "\n");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
