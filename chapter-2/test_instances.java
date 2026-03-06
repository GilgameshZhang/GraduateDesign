import java.io.File;

public class TestInstances {
    public static void main(String[] args) {
        // 测试算例文件
        String[] instanceFiles = {
            "src/main/resources/instance/J20P3B2D5.txt",
            "src/main/resources/instance/J50P5B4D10.txt"
        };

        for (String instancePath : instanceFiles) {
            File instanceFile = new File(instancePath);
            if (!instanceFile.exists()) {
                System.out.println("❌ 算例文件不存在: " + instancePath);
                continue;
            }

            System.out.println("\n🧪 测试算例: " + instancePath);

            try {
                // 创建输入解析器
                ProgramEntity.Input input = new ProgramEntity.Input(instanceFile);
                ProblemFrame.Problem problem = input.getProblemDesFromFile();

                // 显示算例信息
                System.out.println("✅ 算例解析成功!");
                System.out.println("   总机器数: " + problem.getMachineCount());
                System.out.println("   打印机数: " + problem.getPrintMachineCount());
                System.out.println("   批处理机数: " + problem.getBatchMachineCount());
                System.out.println("   离散处理机数: " + problem.getDiscreteMachineCount());
                System.out.println("   工件数: " + problem.getJobCount());
                System.out.println("   总工序数: " + problem.getTotalOperationCount());

            } catch (Exception e) {
                System.out.println("❌ 算例解析失败: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}
