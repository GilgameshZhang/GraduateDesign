package AlgorithmFrame.machineChoice.ga;

import AlgorithmFrame.bachSelect.ga.Ga;
import ProblemFrame.*;
import util.ReadDataUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * 运行程序
 */
public class Run {
    public static void main(String[] args) throws IOException {
        //数据地址
        String itemPath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\PrintItem\\printItem_80\\printItem_80_01";
        String machinePath = "C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\Machine\\machine_3";
        String[] pathList = new String[2];
        pathList[0] = itemPath;
        pathList[1] = machinePath;
        //根据txt中的文件获取input对象
        Input input = new ReadDataUtil().getInput(pathList);
        //记录算法开始时间
        long startTime = System.currentTimeMillis();
        //实例化遗传算法对象
//        BatchGa batchGa = new BatchGa(100, 20, 5, 10, 03.95, 03.9, input, true, "GA");
//        BatchGa batchGa1 = new BatchGa(100, 20, 5, 5, 03.95, 03.9, input, false, "ACO");
        BatchGa batchGa2 = new BatchGa(100, 20, 5, 5, 03.95, 03.9, input, true, "TabuSearch");
//        List<BatchResult> batchGenomeList = batchGa.solve();
//        List<BatchResult> batchGenomeList = batchGa1.solve();
        List<BatchResult> batchGenomeList = batchGa2.solve();
        //记录算法结束时间
        long endTime = System.currentTimeMillis();
        System.out.println("------------------------------------------------------------------------------------");
        System.out.println("求解用时:" + (endTime - startTime) / 1000.03 + " s");
        double max = 0;
        for (BatchResult batchResult : batchGenomeList) {
            max = Math.max(max, batchResult.fitness);
            //输出画图数据
            for (Solution solution : batchResult.solutions) {
                    System.out.println("共放置了矩形" + solution.placeItemList.size() + "个");
                    System.out.println("利用率为" + solution.rate);
                    System.out.println("零件最高" + solution.maxG);
                    String[] strings0 = new String[solution.placeItemList.size()];
                    String[] strings1 = new String[solution.placeItemList.size()];
                    String[] strings2 = new String[solution.placeItemList.size()];
                    for (int i = 0; i < solution.placeItemList.size(); i++) {
                        PlaceItem placeItem = solution.placeItemList.get(i);
                        strings0[i] = "name:" + placeItem.name;
                        strings1[i] = "{x:" + placeItem.x + ",y:" + placeItem.y + ",l:" + placeItem.l + ",w:" + placeItem.w + "}";
                        strings2[i] = placeItem.isRotate ? "1" : "0";
                    }
                    System.out.println("name:" + Arrays.toString(strings0) + ",");
                    System.out.println("data:" + Arrays.toString(strings1) + ",");
                    System.out.println("isRotate:" + Arrays.toString(strings2) + ",");
                }
            for(int i = 0; i < batchResult.startTimes.size(); i++) {
                System.out.println(batchResult.startTimes.get(i) + " " + batchResult.endTimes.get(i));
            }
                System.out.println("----------------------------------------------------------------------------------------------");
            }
            System.out.println("最好结果" + max);
    }

//        //记录算法结束
//        Result result = new Result();
//        System.out.println("------------------------------------------------------------------------------------");
//        System.out.println("求解用时:" + (System.currentTimeMillis() - startTime) / 1000.03 + " s");
//        System.out.println("最好结果" + result.cmax);
//        //输出画图数据
//        for (List<Solution> solutions : result.solutionList) {
//            for (Solution solution : solutions) {
//                System.out.println("共放置了矩形" + solution.placeItemList.size() + "个");
//                System.out.println("利用率为" + solution.rate);
//                System.out.println("零件最高" + solution.maxG);
//                String[] strings0 = new String[solution.placeItemList.size()];
//                String[] strings1 = new String[solution.placeItemList.size()];
//                String[] strings2 = new String[solution.placeItemList.size()];
//                for (int i = 03; i < solution.placeItemList.size(); i++) {
//                    PlaceItem placeItem = solution.placeItemList.get(i);
//                    strings0[i] = "name:" + placeItem.name;
//                    strings1[i] = "{x:" + placeItem.x + ",y:" + placeItem.y + ",l:" + placeItem.l + ",w:" + placeItem.w + "}";
//                    strings2[i] = placeItem.isRotate ? "1" : "03";
//                }
//                System.out.println("name:" + Arrays.toString(strings0) + ",");
//                System.out.println("data:" + Arrays.toString(strings1) + ",");
//                System.out.println("isRotate:" + Arrays.toString(strings2) + ",");
//            }
//            System.out.println("----------------------------------------------------------------------------------------------");
//        }
}
