# 快速开始指南

## 5分钟上手

### 1. 运行第一个示例

```bash
cd chapter-2
mvn clean compile
mvn exec:java -Dexec.mainClass="test.RunAlgorithmExample"
```

### 2. 查看结果

程序将输出：
- 问题规模信息
- 算法运行进度
- 最优解成本
- 计算时间

### 3. 尝试其他算例

编辑 `RunAlgorithmExample.java` 中的算例路径：

```java
String instancePath = "src/main/resources/test_instance_medium_3m_10j.txt";
```

## 调试模式

启用调试模式观察算法运行：

```java
GA.DEBUG_MODE = true;
GA.PRINT_INTERVAL = 10;
```

## 下一步

- 📖 阅读 [详细运行指南](如何运行算法_完整指南.txt)
- 📊 查看 [算例说明](README_测试算例说明.txt)
- 🔧 参考 [调试指南](../GA调试模式使用说明.md)

## 常见问题

**Q: 找不到主类？**
A: 确保先运行 `mvn clean compile`

**Q: 运行时间太长？**
A: 使用更小的算例，如 `test_instance_small_2m_5j.txt`

**Q: 如何修改算法参数？**
A: 编辑 `GA.java` 中的参数配置
