package AlgorithmFrame.dataGenerate;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Random;
import java.util.Scanner;

public class machineGenerate {
    public void generateMachine(int m) {
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < m; i++) {
            sb.append(600.0);
            sb.append(" ");
            sb.append(400.0);
            sb.append(" ");
            sb.append(random.nextBoolean() ? 300.0 : 400.0);
            sb.append(" ");
            sb.append(String.format("%.3f", adjustToMultipleOfFive(0.025 + (0.1 - 0.025) * random.nextDouble())));
            sb.append(" ");
            sb.append(String.format("%.1f", 300 + (600 - 300) * random.nextDouble()));
            sb.append(" ");
            sb.append(String.format("%.1f", 10 + (30 - 10) * random.nextDouble()));
            sb.append(System.lineSeparator());
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter("C:\\Users\\Zhang Hailong\\Desktop\\毕设相关\\毕设程序\\Article\\chapter-1\\src\\main\\resources\\Machine\\machine_5"))) {
            writer.write(sb.toString());
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Random numbers generated and written to output.txt");
    }

    private double adjustToMultipleOfFive(double value) {
        int lastDigit = (int) (value * 1000) % 10;
        int adjustment = (5 - lastDigit % 5) % 5;
        return (Math.floor(value * 1000) + adjustment) / 1000.0;
    }

    public static void main(String[] args) {
        machineGenerate machineGenerate = new machineGenerate();
        Scanner scanner = new Scanner(System.in);
        System.out.println("请输入机器数量：");
        int m = scanner.nextInt();
        machineGenerate.generateMachine(m);
    }
}
