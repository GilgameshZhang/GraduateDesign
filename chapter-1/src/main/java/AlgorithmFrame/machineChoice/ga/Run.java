package AlgorithmFrame.machineChoice.ga;

import AlgorithmFrame.bachSelect.ga.Ga;
import ProblemFrame.*;
import util.ReadDataUtil;

import java.io.*;
import java.util.Arrays;
import java.util.List;

/**
 * 运行程序
 */
public class Run {


    public static void main(String[] args) throws IOException {
        int[] maxGen = new int[]{100, 300, 500};
        int[] populationSize = new int[]{50, 75, 100};
        double[] mutationRate = new double[]{0.85, 0.9, 0.95};
        double[] crossoverRate = new double[]{0.85, 0.9, 0.95};
        int[] decodeMaxGen = new int[]{300, 400, 500};
        int[] tabuSize = new int[]{20, 30, 40};
        int[] maxN = new int[]{20, 30, 40};
        //读文件
        int count = 0;
        BufferedReader br = new BufferedReader(new FileReader(new File("C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\data")));
        while (br.ready()) {
            count++;
            String line = br.readLine();
            String[] split = line.split("\t");
            Thread thread = new Thread(new Start(maxGen[Integer.parseInt(split[0]) - 1], populationSize[Integer.parseInt(split[1]) - 1], mutationRate[Integer.parseInt(split[2]) - 1], crossoverRate[Integer.parseInt(split[3]) - 1], decodeMaxGen[Integer.parseInt(split[4]) - 1], tabuSize[Integer.parseInt(split[5]) - 1],  maxN[Integer.parseInt(split[6]) - 1]), "thread" + count);
            thread.start();
        }
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
