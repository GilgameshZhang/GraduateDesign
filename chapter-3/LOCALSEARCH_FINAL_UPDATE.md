# 局部搜索集成 - 最终更新说明

## 更新时间
2026-01-15

## 问题修复

### 问题1: 找不到 ReadDataUtil 类

**错误信息**:
```
java: 找不到符号
  符号:   类 ReadDataUtil
  位置: 程序包 util
```

**原因**: 
测试代码使用了不存在的 `util.ReadDataUtil` 类。

**解决方案**:
第三章使用 `Input` 类读取算例文件，而不是 `ReadDataUtil`。

**修改前**:
```java
import util.ReadDataUtil;
...
problem = ReadDataUtil.getData(instancePath);
```

**修改后**:
```java
import ProgramEntity.Input;
import java.io.File;
...
problem = new Input(new File(instancePath)).getProblemDesFromFile();
```

### 问题2: Solution 对象复制

**问题描述**:
`Delta` 类需要深拷贝 `Solution` 对象，但原构造函数不包含所有字段。

**解决方案**:
手动复制所有字段，包括 `startTime` 和 `endTime`。

**修改代码**:
```java
public void snapshotPrinterBatches(int printerNo, List<Solution> batches) {
    List<Solution> copy = new ArrayList<>();
    if (batches != null) {
        for (Solution batch : batches) {
            // 手动复制所有字段
            Solution batchCopy = new Solution(batch.placeItemList, batch.maxG, batch.totalS, batch.rate);
            batchCopy.startTime = batch.startTime;
            batchCopy.endTime = batch.endTime;
            copy.add(batchCopy);
        }
    }
    printerBatchSnapshots.put(printerNo, copy);
}
```

## 最终修改文件清单

### 1. 测试文件修改

**文件**: `src/test/java/LocalSearchNSGAIITest.java`

修改内容：
- 导入 `ProgramEntity.Input` 和 `java.io.File`
- 移除 `util.ReadDataUtil` 导入
- 使用 `new Input(new File(instancePath)).getProblemDesFromFile()` 读取算例

### 2. Delta类修改

**文件**: `src/main/java/ProblemFrame/localsearch/Delta.java`

修改内容：
- 在 `snapshotPrinterBatches()` 方法中手动复制 `startTime` 和 `endTime` 字段

### 3. PlaceItem类修改（已完成）

**文件**: `src/main/java/ProgramEntity/PlaceItem.java`

添加内容：
- 拷贝构造函数

## 编译和测试

### 编译命令
```bash
cd chapter-3
mvn clean compile
```

### 运行测试
```bash
# 编译测试类
mvn test-compile

# 运行测试（如果是Maven项目）
mvn test -Dtest=LocalSearchNSGAIITest

# 或者直接运行main方法
cd target/classes
java -cp .:../test-classes LocalSearchNSGAIITest
```

### Windows PowerShell 运行
```powershell
cd chapter-3
mvn clean compile test-compile
java -cp "target/classes;target/test-classes" LocalSearchNSGAIITest
```

## 验证清单

- [x] ✅ 编译通过（无错误）
- [x] ✅ 测试文件正确导入类
- [x] ✅ Solution 对象正确复制所有字段
- [x] ✅ PlaceItem 拷贝构造函数已添加
- [x] ✅ 局部搜索功能集成完成

## 完整的使用示例

```java
import AlgorithmFrame.nsgaii.NSGAII;
import ProblemFrame.MOEvaluator;
import ProgramEntity.Input;
import ProgramEntity.Problem;
import java.io.File;
import java.util.Arrays;

public class QuickExample {
    public static void main(String[] args) throws Exception {
        // 1. 读取算例（第三章标准方式）
        Problem problem = new Input(
            new File("src/main/resources/instance/J20/J20P3B2D5_01.txt")
        ).getProblemDesFromFile();
        
        // 2. 创建NSGA-II实例
        NSGAII nsgaii = new NSGAII(problem, Arrays.asList(
            new MOEvaluator.MaximumCompletionTime(),
            new MOEvaluator.TotalEnergyConsumption(problem)
        ));
        
        // 3. 配置参数
        nsgaii.setPopulationSize(100);
        nsgaii.setMaxGenerations(500);
        nsgaii.setMaxRunTimeMinutes(5.0);
        
        // 4. 启用局部搜索
        nsgaii.enableLocalSearch(true, 10);
        
        // 5. 运行算法
        nsgaii.solve();
        
        // 6. 获取结果
        System.out.println("Pareto前沿: " + nsgaii.getParetoFront().size());
    }
}
```

## 算例路径说明

第三章的算例位于：
```
chapter-3/src/main/resources/instance/
├── J10/
│   ├── J10P2B1D2_01.txt
│   └── J10P2B1D2_02.txt
├── J20/
│   ├── J20P2B2D5_01.txt
│   ├── J20P3B2D5_01.txt
│   └── J20P3B2D5_02.txt
├── J50/
│   ├── J50P2B2D5_01.txt
│   └── ...
└── J100/
    └── ...
```

使用相对路径：
```java
String path = "src/main/resources/instance/J20/J20P3B2D5_01.txt";
```

或使用绝对路径：
```java
String path = "C:\\Users\\...\\chapter-3\\src\\main\\resources\\instance\\J20\\J20P3B2D5_01.txt";
```

## 注意事项

### 1. 路径分隔符
- Windows: 使用 `\\` 或 `/`
- Linux/Mac: 使用 `/`

### 2. 文件存在性检查
```java
File file = new File(instancePath);
if (!file.exists()) {
    System.err.println("算例文件不存在: " + instancePath);
    return;
}
```

### 3. 异常处理
```java
try {
    Problem problem = new Input(new File(instancePath)).getProblemDesFromFile();
} catch (Exception e) {
    System.err.println("读取算例失败: " + e.getMessage());
    e.printStackTrace();
    return;
}
```

## 性能建议

### 小规模测试（快速验证）
```java
nsgaii.setPopulationSize(30);
nsgaii.setMaxGenerations(30);
nsgaii.enableLocalSearch(true, 10);
```

### 中等规模实验
```java
nsgaii.setPopulationSize(50);
nsgaii.setMaxGenerations(100);
nsgaii.enableLocalSearch(true, 10);
```

### 正式实验（高质量结果）
```java
nsgaii.setPopulationSize(100);
nsgaii.setMaxGenerations(500);
nsgaii.enableLocalSearch(true, 10);
```

## 相关文档

- [LOCALSEARCH_README.md](LOCALSEARCH_README.md) - 完整使用说明
- [LOCALSEARCH_QUICKSTART.md](LOCALSEARCH_QUICKSTART.md) - 快速入门
- [LOCALSEARCH_IMPLEMENTATION_SUMMARY.md](LOCALSEARCH_IMPLEMENTATION_SUMMARY.md) - 实现总结
- [LOCALSEARCH_BUGFIX.md](LOCALSEARCH_BUGFIX.md) - Bug修复说明

## 状态

✅ **所有问题已解决，代码可以正常编译和运行！**

---

**版本**: v1.0-final  
**日期**: 2026-01-15  
**状态**: ✅ 完成
