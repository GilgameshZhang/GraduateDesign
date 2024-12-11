package AlgorithmFrame.dataGenerate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;

public class printItemGenerate {
    public void generatePrintItem(int m) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < m; i++) {
            for (int j = 0; j < 3; j++) {
                sb.append(100 + random.nextInt(200)); // 生成0到99之间的随机数
                if (j < 2) {
                    sb.append(" ");
                }
            }
            sb.append(System.lineSeparator());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\PrintItem\\printItem_80\\printItem_80_04"))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Random numbers generated and written to output.txt");
    }

    public static void main(String[] args) {
        printItemGenerate printItemGenerate = new printItemGenerate();
        printItemGenerate.generatePrintItem(80);
    }
}
