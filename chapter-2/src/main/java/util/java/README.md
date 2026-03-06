# Java算例生成和验证工具

## 📁 文件说明

- `InstanceGenerator.java` - 算例生成器
- `InstanceValidator.java` - 算例验证器
- `README.md` - 使用说明

## 🚀 使用方法

### 1. 编译Java文件

```bash
cd chapter-2/src/main/resources/java
javac *.java
```

### 2. 生成算例

#### 生成单个算例
```java
InstanceGenerator generator = new InstanceGenerator(42);
generator.generateInstance(20, 3, 2, 5, "instance/J20P3B2D5_test.txt");
```

#### 生成多个算例
```java
InstanceGenerator generator = new InstanceGenerator(42);
generator.generateMultipleInstances(20, 3, 2, 5, 10, "instance/J20P3B2D5");
```

### 3. 验证算例

#### 验证单个文件
```java
InstanceValidator validator = new InstanceValidator();
boolean isValid = validator.validateFile("instance/J20P3B2D5_01.txt");
```

#### 批量验证目录
```java
InstanceValidator validator = new InstanceValidator();
validator.validateDirectory("instance");
```

## 📋 算例文件格式

### 文件结构
```
第1行: [总机器数] [工件数]
第2行: [打印机数] [打印机1参数...] [打印机2参数...] ...
第3行: [批处理机数] [批处理机1参数...] [批处理机2参数...] ...
第4行开始: 每个工件的工序信息
```

### 工件行格式
```
[离散工序总数] [长] [宽] [高] [离散工序数确认]
[打印工序机器数] [打印机编号] [打印时间(0)]
[批处理工序机器数] [批处理机编号] [批处理时间(0)]
[离散工序1机器数] [机器1编号] [时间1] [机器2编号] [时间2] ...
[离散工序2机器数] [机器1编号] [时间1] ...
...
```

### 示例
```
19 50
5 1 1100 500 400 0.045 380 15 2 900 600 500 0.072 450 28 3 1000 400 600 0.038 520 19 4 800 500 500 0.095 420 24 5 950 550 500 0.051 490 24
4 1 900 2 1100 3 700 4 800
6 73 100 41 6 1 1 0 1 6 0 2 10 850 11 920 1 12 780 3 13 950 14 880 15 720 2 16 690 17 810 1 18 730 1 19 860
```

## 🔧 参数说明

### InstanceGenerator 参数

- `jobCount`: 工件数量
- `printerCount`: 打印机数量
- `batchCount`: 批处理机数量
- `discreteCount`: 离散处理机数量
- `seed`: 随机种子（确保结果可重现）

### 机器配置范围

| 机器类型 | 数量范围 | 加工时间范围 |
|----------|----------|---------------|
| 打印机 | 1-N | - |
| 批处理机 | 0-N | 600-1200 |
| 离散机 | 1-N | 600-1200 |

### 工件参数范围

| 参数 | 范围 |
|------|------|
| 长度 | 10-600 |
| 宽度 | 10-600 |
| 高度 | 10-400 |
| 离散工序数 | 1-8 |

## ✅ 验证内容

InstanceValidator 会检查以下方面：

1. **文件格式正确性**
   - 文件头格式
   - 行数和参数数量

2. **数据合理性**
   - 机器数量和编号范围
   - 工序数据完整性
   - 时间参数合理性

3. **业务规则**
   - 打印时间必须为0
   - 批处理时间必须为0
   - 离散加工时间在600-1200范围内
   - 机器编号不重复且在正确范围内

## 🎯 示例代码

### 生成测试算例
```java
public class TestGenerator {
    public static void main(String[] args) {
        try {
            InstanceGenerator generator = new InstanceGenerator(42);

            // 生成小规模测试算例
            generator.generateInstance(5, 2, 1, 3, "test_small.txt");

            // 生成中等规模算例
            generator.generateInstance(10, 3, 2, 5, "test_medium.txt");

            System.out.println("测试算例生成完成！");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

### 验证算例质量
```java
public class TestValidator {
    public static void main(String[] args) {
        InstanceValidator validator = new InstanceValidator();

        // 验证生成的算例
        boolean isValid1 = validator.validateFile("test_small.txt");
        boolean isValid2 = validator.validateFile("test_medium.txt");

        if (isValid1 && isValid2) {
            System.out.println("所有测试算例验证通过！");
        } else {
            System.out.println("部分算例存在问题，请检查错误信息。");
        }
    }
}
```

## 📊 输出结果

### 生成器输出
```
已生成算例：instance/J20P3B2D5_01.txt
已生成算例：instance/J20P3B2D5_02.txt
...
✅ 所有算例生成完成！
```

### 验证器输出
```
🔍 验证算例文件: J20P3B2D5_01.txt
✅ 算例文件验证通过

批量验证结果汇总
================================================================================
总文件数: 25
验证通过: 25
验证失败: 0
总错误数: 0
总警告数: 0

🎉 所有算例文件验证通过！
```

## 🔄 集成使用

```java
public class InstanceManager {
    public static void main(String[] args) {
        // 1. 生成算例
        InstanceGenerator generator = new InstanceGenerator(42);
        generator.generateInstance(20, 3, 2, 5, "my_instance.txt");

        // 2. 验证算例
        InstanceValidator validator = new InstanceValidator();
        boolean isValid = validator.validateFile("my_instance.txt");

        if (isValid) {
            System.out.println("算例生成并验证成功，可以用于算法测试！");
        } else {
            System.out.println("算例存在问题，请检查并修复。");
        }
    }
}
```

## 📝 注意事项

1. **随机种子**: 使用固定种子可以确保结果可重现
2. **文件路径**: 确保输出目录存在
3. **参数合理性**: 机器数量和工件数量应该匹配实际需求
4. **编码格式**: 文件使用UTF-8编码保存

## 🐛 故障排除

### 常见错误及解决方法

1. **文件读取失败**
   - 检查文件路径是否正确
   - 确保文件未被其他程序占用

2. **格式验证失败**
   - 检查参数数量是否符合要求
   - 验证机器编号范围是否正确

3. **内存不足**
   - 对于大规模算例，适当增加JVM内存
   - 分批生成大型算例

## 📞 技术支持

如遇到问题，请检查：
1. Java版本 (推荐 JDK 8+)
2. 文件权限
3. 磁盘空间
4. 随机种子设置
