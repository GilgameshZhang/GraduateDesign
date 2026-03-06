package util.java;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 算例验证器
 * 验证算例文件的格式正确性和数据合理性
 */
public class InstanceValidator {

    private List<String> errors;
    private List<String> warnings;

    public InstanceValidator() {
        this.errors = new ArrayList<>();
        this.warnings = new ArrayList<>();
    }

    /**
     * 验证算例文件
     */
    public boolean validateFile(String filepath) {
        errors.clear();
        warnings.clear();

        System.out.println("\n🔍 验证算例文件: " + getFilename(filepath));

        try (BufferedReader reader = new BufferedReader(new FileReader(filepath))) {
            List<String> lines = new ArrayList<>();
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line.trim());
            }

            if (lines.size() < 3) {
                errors.add("文件行数不足，至少需要3行");
                return false;
            }

            return validateContent(lines);

        } catch (IOException e) {
            errors.add("文件读取失败: " + e.getMessage());
            return false;
        }
    }

    private boolean validateContent(List<String> lines) {
        // 验证第一行：总机器数 工件数
        if (!validateHeader(lines.get(0))) {
            return false;
        }

        String[] headerParts = lines.get(0).split("\\s+");
        int totalMachines = Integer.parseInt(headerParts[0]);
        int expectedJobs = Integer.parseInt(headerParts[1]);

        // 验证第二行：打印机配置
        int printerCount = validatePrinterLine(lines.get(1));
        if (printerCount == -1) {
            return false;
        }

        // 验证第三行：批处理机配置
        int batchCount = validateBatchLine(lines.get(2));
        if (batchCount == -1) {
            return false;
        }

        // 计算离散机数量
        int discreteCount = totalMachines - printerCount - batchCount;
        if (discreteCount <= 0) {
            errors.add("离散机数量计算错误: " + discreteCount);
            return false;
        }

        // 验证工件数量
        int actualJobs = lines.size() - 3;
        if (actualJobs != expectedJobs) {
            errors.add(String.format("工件数量不匹配: 期望%d个，实际%d个", expectedJobs, actualJobs));
            return false;
        }

        // 机器编号范围
        Map<String, int[]> machineRanges = new HashMap<>();
        machineRanges.put("printer", new int[]{1, printerCount});
        machineRanges.put("batch", new int[]{printerCount + 1, printerCount + batchCount});
        machineRanges.put("discrete", new int[]{printerCount + batchCount + 1, totalMachines});

        // 验证每个工件
        for (int jobIdx = 0; jobIdx < expectedJobs; jobIdx++) {
            String jobLine = lines.get(3 + jobIdx);
            if (!validateJobLine(jobLine, jobIdx, machineRanges)) {
                return false;
            }
        }

        return reportResults();
    }

    private boolean validateHeader(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length != 2) {
            errors.add("文件头格式错误: " + line);
            return false;
        }

        try {
            int totalMachines = Integer.parseInt(parts[0]);
            int jobCount = Integer.parseInt(parts[1]);

            if (totalMachines <= 0 || jobCount <= 0) {
                errors.add("机器数或工件数不能为负数或零");
                return false;
            }
        } catch (NumberFormatException e) {
            errors.add("文件头包含非数字: " + line);
            return false;
        }

        return true;
    }

    private int validatePrinterLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 1) {
            errors.add("打印机行格式错误: " + line);
            return -1;
        }

        try {
            int printerCount = Integer.parseInt(parts[0]);
            if (printerCount <= 0) {
                errors.add("打印机数量不能为负数或零: " + printerCount);
                return -1;
            }

            // 验证每个打印机的参数（7个参数：编号 + 6个属性）
            int expectedParams = 1 + printerCount * 7;
            if (parts.length != expectedParams) {
                errors.add(String.format("打印机参数数量错误: 期望%d个，实际%d个", expectedParams, parts.length));
                return -1;
            }

        } catch (NumberFormatException e) {
            errors.add("打印机行包含非数字: " + line);
            return -1;
        }

        return Integer.parseInt(parts[0]);
    }

    private int validateBatchLine(String line) {
        String[] parts = line.split("\\s+");
        if (parts.length < 1) {
            errors.add("批处理机行格式错误: " + line);
            return -1;
        }

        try {
            int batchCount = Integer.parseInt(parts[0]);
            if (batchCount < 0) {
                errors.add("批处理机数量不能为负数: " + batchCount);
                return -1;
            }

            // 验证每个批处理机的参数（2个参数：编号 + 加工时间）
            int expectedParams = 1 + batchCount * 2;
            if (parts.length != expectedParams) {
                errors.add(String.format("批处理机参数数量错误: 期望%d个，实际%d个", expectedParams, parts.length));
                return -1;
            }

        } catch (NumberFormatException e) {
            errors.add("批处理机行包含非数字: " + line);
            return -1;
        }

        return Integer.parseInt(parts[0]);
    }

    private boolean validateJobLine(String line, int jobIdx, Map<String, int[]> machineRanges) {
        String[] parts = line.split("\\s+");
        if (parts.length < 8) {
            errors.add(String.format("J%d: 工件行格式错误，至少需要8个参数", jobIdx + 1));
            return false;
        }

        try {
            int discreteOps = Integer.parseInt(parts[0]);
            int length = Integer.parseInt(parts[1]);
            int width = Integer.parseInt(parts[2]);
            int height = Integer.parseInt(parts[3]);
            int discreteOpsConfirm = Integer.parseInt(parts[4]);

            if (discreteOps != discreteOpsConfirm) {
                errors.add(String.format("J%d: 离散工序数量不一致 (%d != %d)", jobIdx + 1, discreteOps, discreteOpsConfirm));
                return false;
            }

            // 验证尺寸
            if (length <= 0 || width <= 0 || height <= 0) {
                errors.add(String.format("J%d: 工件尺寸不能为负数或零: %dx%dx%d", jobIdx + 1, length, width, height));
                return false;
            }

            int idx = 5;
            int operationCount = 0;

            // 打印工序
            if (idx + 2 >= parts.length) {
                errors.add(String.format("J%d: 打印工序数据不完整", jobIdx + 1));
                return false;
            }

            int printOps = Integer.parseInt(parts[idx]);
            int printMachine = Integer.parseInt(parts[idx + 1]);
            int printTime = Integer.parseInt(parts[idx + 2]);

            if (printOps != 1) {
                errors.add(String.format("J%d: 打印工序机器数量错误，应该是1台", jobIdx + 1));
                return false;
            }

            int[] printerRange = machineRanges.get("printer");
            if (printMachine < printerRange[0] || printMachine > printerRange[1]) {
                errors.add(String.format("J%d: 打印机编号超出范围: M%d (应为M%d-M%d)",
                        jobIdx + 1, printMachine, printerRange[0], printerRange[1]));
                return false;
            }

            if (printTime != 0) {
                errors.add(String.format("J%d: 打印时间应该是0: %d", jobIdx + 1, printTime));
                return false;
            }

            idx += 3;
            operationCount++;

            // 批处理工序
            if (idx + 2 >= parts.length) {
                errors.add(String.format("J%d: 批处理工序数据不完整", jobIdx + 1));
                return false;
            }

            int batchOps = Integer.parseInt(parts[idx]);
            int batchMachine = Integer.parseInt(parts[idx + 1]);
            int batchTime = Integer.parseInt(parts[idx + 2]);

            if (batchOps != 1) {
                errors.add(String.format("J%d: 批处理工序机器数量错误，应该是1台", jobIdx + 1));
                return false;
            }

            int[] batchRange = machineRanges.get("batch");
            if (batchMachine < batchRange[0] || batchMachine > batchRange[1]) {
                errors.add(String.format("J%d: 批处理机编号超出范围: M%d (应为M%d-M%d)",
                        jobIdx + 1, batchMachine, batchRange[0], batchRange[1]));
                return false;
            }

            if (batchTime != 0) {
                errors.add(String.format("J%d: 批处理时间应该是0: %d", jobIdx + 1, batchTime));
                return false;
            }

            idx += 3;
            operationCount++;

            // 离散工序
            int[] discreteRange = machineRanges.get("discrete");
            for (int opIdx = 0; opIdx < discreteOps; opIdx++) {
                if (idx >= parts.length) {
                    errors.add(String.format("J%d 离散工序%d: 数据不完整", jobIdx + 1, opIdx + 1));
                    return false;
                }

                int machineCount = Integer.parseInt(parts[idx]);
                idx += 1;

                // 验证离散工序的机器
                for (int m = 0; m < machineCount; m++) {
                    if (idx + 1 >= parts.length) {
                        errors.add(String.format("J%d 离散工序%d: 机器-时间对不完整", jobIdx + 1, opIdx + 1));
                        return false;
                    }

                    int machineId = Integer.parseInt(parts[idx]);
                    int processTime = Integer.parseInt(parts[idx + 1]);

                    if (machineId < discreteRange[0] || machineId > discreteRange[1]) {
                        errors.add(String.format("J%d 离散工序%d: 离散机编号超出范围: M%d (应为M%d-M%d)",
                                jobIdx + 1, opIdx + 1, machineId, discreteRange[0], discreteRange[1]));
                        return false;
                    }

                    if (processTime <= 0) {
                        errors.add(String.format("J%d 离散工序%d: 加工时间不能为负数或零: %d",
                                jobIdx + 1, opIdx + 1, processTime));
                        return false;
                    }

                    if (processTime < 600 || processTime > 1200) {
                        warnings.add(String.format("J%d 离散工序%d: 加工时间超出推荐范围: %d (建议600-1200)",
                                jobIdx + 1, opIdx + 1, processTime));
                    }

                    idx += 2;
                }

                operationCount++;
            }

            // 验证工序总数
            int expectedTotalOps = 2 + discreteOps;
            if (operationCount != expectedTotalOps) {
                errors.add(String.format("J%d: 工序总数不匹配，期望%d个，实际%d个",
                        jobIdx + 1, expectedTotalOps, operationCount));
                return false;
            }

        } catch (NumberFormatException e) {
            errors.add(String.format("J%d: 包含非数字参数 - %s", jobIdx + 1, e.getMessage()));
            return false;
        } catch (Exception e) {
            errors.add(String.format("J%d: 验证出错 - %s", jobIdx + 1, e.getMessage()));
            return false;
        }

        return true;
    }

    private boolean reportResults() {
        if (!errors.isEmpty()) {
            System.out.println("❌ 发现错误:");
            for (int i = 0; i < Math.min(errors.size(), 10); i++) {
                System.out.println("   " + errors.get(i));
            }
            if (errors.size() > 10) {
                System.out.println("   ... 还有" + (errors.size() - 10) + "个错误");
            }
            return false;
        }

        if (!warnings.isEmpty()) {
            System.out.println("⚠️ 发现警告:");
            for (int i = 0; i < Math.min(warnings.size(), 5); i++) {
                System.out.println("   " + warnings.get(i));
            }
            if (warnings.size() > 5) {
                System.out.println("   ... 还有" + (warnings.size() - 5) + "个警告");
            }
        }

        System.out.println("✅ 算例文件验证通过");
        return true;
    }

    private String getFilename(String filepath) {
        return filepath.substring(filepath.lastIndexOf('\\') + 1);
    }

    /**
     * 批量验证目录中的所有算例文件
     */
    public void validateDirectory(String directoryPath) {
        java.io.File directory = new java.io.File(directoryPath);
        if (!directory.exists() || !directory.isDirectory()) {
            System.out.println("❌ 目录不存在: " + directoryPath);
            return;
        }

        java.io.File[] files = directory.listFiles((dir, name) -> name.endsWith(".txt"));
        if (files == null || files.length == 0) {
            System.out.println("❌ 目录中没有找到算例文件");
            return;
        }

        System.out.println("开始验证 " + files.length + " 个算例文件\n");

        int validCount = 0;
        int totalErrors = 0;
        int totalWarnings = 0;

        for (java.io.File file : files) {
            if (validateFile(file.getAbsolutePath())) {
                validCount++;
            }
            totalErrors += errors.size();
            totalWarnings += warnings.size();
        }

        System.out.println("\n" + "====================================");
        System.out.println("批量验证结果汇总");
        System.out.println("================================");
        System.out.println("总文件数: " + files.length);
        System.out.println("验证通过: " + validCount);
        System.out.println("验证失败: " + (files.length - validCount));
        System.out.println("总错误数: " + totalErrors);
        System.out.println("总警告数: " + totalWarnings);

        if (validCount == files.length) {
            System.out.println("\n🎉 所有算例文件验证通过！");
        } else {
            System.out.println("\n⚠️ 有 " + (files.length - validCount) + " 个文件需要修复");
        }
    }

    /**
     * 主函数示例
     */
    public static void main(String[] args) {
        InstanceValidator validator = new InstanceValidator();

        // 验证单个文件
        boolean isValid = validator.validateFile("instance/J20P3B2D5_01.txt");

        // 批量验证目录
        validator.validateDirectory("instance");

        System.out.println("\n验证完成！");
    }
}
