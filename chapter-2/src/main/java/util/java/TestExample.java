package util.java;

/**
 * 算例生成和验证测试示例
 */
public class TestExample {

    public static void main(String[] args) {
        try {
            System.out.println("=== 算例生成和验证测试 ===\n");

            // 1. 创建生成器
            InstanceGenerator generator = new InstanceGenerator(42);

            // 2. 生成几个不同规模的测试算例
            System.out.println("正在生成测试算例...");

            generator.generateInstance(5, 2, 1, 2, "test_small.txt");
            generator.generateInstance(10, 3, 2, 3, "test_medium.txt");
            generator.generateInstance(20, 3, 2, 5, "test_large.txt");

            System.out.println("\n测试算例生成完成！\n");

            // 3. 创建验证器
            InstanceValidator validator = new InstanceValidator();

            // 4. 验证生成的算例
            System.out.println("正在验证测试算例...");
            boolean valid1 = validator.validateFile("test_small.txt");
            boolean valid2 = validator.validateFile("test_medium.txt");
            boolean valid3 = validator.validateFile("test_large.txt");

            // 5. 输出结果
            System.out.println("\n=== 验证结果 ===");
            System.out.println("小型算例 (5工件): " + (valid1 ? "✅ 通过" : "❌ 失败"));
            System.out.println("中型算例 (10工件): " + (valid2 ? "✅ 通过" : "❌ 失败"));
            System.out.println("大型算例 (20工件): " + (valid3 ? "✅ 通过" : "❌ 失败"));

            if (valid1 && valid2 && valid3) {
                System.out.println("\n🎉 所有测试算例生成并验证成功！");
                System.out.println("可以用于遗传算法测试。");
            } else {
                System.out.println("\n⚠️ 部分算例存在问题，请检查错误信息。");
            }

        } catch (Exception e) {
            System.err.println("测试过程中出现错误: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
