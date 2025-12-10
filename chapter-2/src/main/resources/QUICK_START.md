# 测试算例快速使用指南

## 📁 已生成的文件

### 算例文件 (8个)
位置: `chapter-2/src/main/resources/`

1. **test_instance_small_2m_5j.txt** - 小型基础测试
2. **test_instance_medium_3m_10j.txt** - 中型标准测试
3. **test_instance_large_4m_20j.txt** - 大型性能测试
4. **test_instance_xlarge_5m_30j.txt** - 超大规模测试
5. **test_instance_complex_3m_15j_5ops.txt** - 复杂工序测试
6. **test_instance_large_parts_2m_8j.txt** - 大零件装箱测试
7. **test_instance_small_parts_3m_12j.txt** - 小零件装箱测试
8. **test_instance_multi_ops_4m_25j_6ops.txt** - 多工序调度测试

### 文档文件 (4个)
位置: `chapter-2/src/main/resources/`

1. **算例格式说明.txt** - 详细的数据格式定义
2. **README_测试算例说明.txt** - 完整的使用指南
3. **测试算例索引.md** - 快速索引和分类
4. **算例生成汇总.txt** - 完整的汇总信息

### 工具程序 (3个)
位置: `chapter-2/src/test/java/`

1. **TestInstanceValidator.java** - 算例验证工具
2. **InstanceStatistics.java** - 统计分析工具
3. **SimpleInstanceReader.java** - 读取示例程序

---

## 🚀 5分钟上手

### 步骤1: 验证算例
```bash
cd chapter-2
javac -cp src/main/java src/test/java/TestInstanceValidator.java
java -cp src/main/java:src/test/java TestInstanceValidator
```

### 步骤2: 查看统计
```bash
javac -cp src/main/java src/test/java/InstanceStatistics.java
java -cp src/main/java:src/test/java InstanceStatistics
```

### 步骤3: 在代码中使用
```java
import ProgramEntity.Input;
import ProgramEntity.Problem;
import java.io.File;

// 读取算例
File file = new File("src/main/resources/test_instance_small_2m_5j.txt");
Input input = new Input(file);
Problem problem = input.getProblemDesFromFile();

// 获取基本信息
int jobCount = problem.getJobCount();
int machineCount = problem.getMachineCount();
System.out.println("工件数: " + jobCount);
System.out.println("机器数: " + machineCount);
```

---

## 📊 算例选择建议

| 使用场景 | 推荐算例 | 预期时间 |
|---------|---------|----------|
| **快速调试** | small_2m_5j | < 5秒 |
| **功能测试** | medium_3m_10j | < 30秒 |
| **性能评估** | large_4m_20j | < 2分钟 |
| **装箱优化** | large_parts_2m_8j | < 20秒 |
| **调度优化** | multi_ops_4m_25j_6ops | < 3分钟 |
| **极限测试** | xlarge_5m_30j | < 5分钟 |

---

## 🔍 算例数据结构

每个算例包含：
- ✅ 打印机配置 (尺寸、层高、时间参数)
- ✅ 批处理机配置 (处理时间)
- ✅ 零件信息 (长宽高)
- ✅ 工序信息 (机器选择、加工时间)

---

## 💡 常见问题

**Q: 如何验证算例是否正确？**
A: 运行 `TestInstanceValidator.java`，会自动检查所有算例。

**Q: 如何查看算例的详细统计？**
A: 运行 `InstanceStatistics.java`，会生成详细报告和CSV文件。

**Q: 如何在代码中读取算例？**
A: 参考 `SimpleInstanceReader.java` 中的示例代码。

**Q: 算例的时间单位是什么？**
A: 统一使用分钟，长度单位为毫米。

**Q: 打印工序的时间为什么是0？**
A: 打印时间由装箱结果动态计算，格式中设为0是正常的。

---

## 📖 详细文档

需要更多信息？请查看：
- **格式说明**: `算例格式说明.txt`
- **完整指南**: `README_测试算例说明.txt`
- **快速索引**: `测试算例索引.md`
- **完整汇总**: `算例生成汇总.txt`

---

## ✅ 完成清单

- [x] 生成8个不同规模的测试算例
- [x] 创建格式说明文档
- [x] 编写完整使用指南
- [x] 提供算例索引和分类
- [x] 开发验证工具
- [x] 开发统计分析工具
- [x] 提供读取示例代码
- [x] 生成汇总文档

---

**现在你可以开始使用这些算例测试你的算法了！** 🎉

建议流程:
1. 先用 `test_instance_small_2m_5j.txt` 调试
2. 再用 `test_instance_medium_3m_10j.txt` 测试
3. 最后用全部算例进行完整评估

