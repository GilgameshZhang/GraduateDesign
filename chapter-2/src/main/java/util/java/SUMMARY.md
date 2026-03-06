# Java算例工具包 - 完整解决方案

## 📦 工具包内容

### 核心文件
- `InstanceGenerator.java` - 算例生成器
- `InstanceValidator.java` - 算例验证器
- `TestExample.java` - 使用示例
- `README.md` - 详细使用说明
- `SUMMARY.md` - 本文件

### 功能特性

#### 🎯 InstanceGenerator（算例生成器）
- ✅ 支持生成任意规模的算例
- ✅ 可配置机器数量和类型
- ✅ 自动生成合理的参数范围
- ✅ 支持批量生成多个算例
- ✅ 使用随机种子确保结果可重现

#### 🔍 InstanceValidator（算例验证器）
- ✅ 全面验证文件格式正确性
- ✅ 检查数据合理性和一致性
- ✅ 验证机器编号范围
- ✅ 检查时间参数合理性
- ✅ 支持批量验证目录

#### 📊 TestExample（测试示例）
- ✅ 演示如何使用生成器和验证器
- ✅ 生成不同规模的测试算例
- ✅ 自动验证生成结果

## 🚀 快速开始

### 1. 编译
```bash
cd chapter-2/src/main/resources/java
javac *.java
```

### 2. 运行测试
```bash
java TestExample
```

### 3. 自定义使用
```java
// 生成算例
InstanceGenerator gen = new InstanceGenerator(42);
gen.generateInstance(20, 3, 2, 5, "my_instance.txt");

// 验证算例
InstanceValidator val = new InstanceValidator();
boolean isValid = val.validateFile("my_instance.txt");
```

## 📋 算例格式规范

### 文件结构
```
第1行: [总机器数] [工件数]
第2行: [打印机数] [打印机参数...]
第3行: [批处理机数] [批处理机参数...]
第4行+: [离散工序总数] [长] [宽] [高] [离散工序数确认] [打印数据] [批处理数据] [离散数据...]
```

### 参数范围
| 参数类型 | 范围 | 说明 |
|----------|------|------|
| 工件尺寸 | 10-600 | 长宽高 |
| 离散工序数 | 1-8 | 每个工件 |
| 机器可选数 | 1-4 | 每个离散工序 |
| 加工时间 | 600-1200 | 离散工序 |
| 打印时间 | 0 | 占位符 |
| 批处理时间 | 0 | 占位符 |

## 🎯 使用场景

### 1. 算法测试
```java
// 生成测试算例
generator.generateInstance(50, 5, 4, 10, "test_case.txt");

// 验证算例质量
validator.validateFile("test_case.txt");

// 运行遗传算法...
```

### 2. 参数调优
```java
// 生成不同规模的算例进行对比测试
generator.generateMultipleInstances(20, 3, 2, 5, 10, "J20_test");
generator.generateMultipleInstances(50, 5, 4, 10, 10, "J50_test");
```

### 3. 批量验证
```java
// 验证整个算例库
validator.validateDirectory("instance");
```

## 🔧 高级功能

### 自定义参数
```java
// 使用特定随机种子
InstanceGenerator gen = new InstanceGenerator(12345);

// 生成大规模算例
gen.generateInstance(100, 8, 6, 15, "large_instance.txt");
```

### 错误处理
```java
try {
    generator.generateInstance(params...);
} catch (IOException e) {
    System.err.println("生成失败: " + e.getMessage());
}
```

## 📈 性能特性

- **内存效率**: 流式文件处理，不占用大量内存
- **速度优化**: 批量生成时复用随机数生成器
- **错误恢复**: 单个文件错误不影响批量处理
- **可扩展性**: 易于添加新的验证规则

## 🐛 常见问题

### Q: 生成的算例无法通过验证？
A: 检查参数范围，确保机器数量配置正确

### Q: 内存不足？
A: 对于超大规模算例，考虑分批生成

### Q: 结果不一致？
A: 使用相同的随机种子可以确保结果可重现

## 📚 技术栈

- **语言**: Java 8+
- **特性**: 面向对象设计，异常处理
- **依赖**: 仅使用标准库，无外部依赖
- **兼容性**: 支持Windows/Linux/macOS

## 🎉 总结

这个Java工具包提供了完整的算例生成和验证解决方案：

1. **高质量生成**: 自动生成符合规范的算例文件
2. **严格验证**: 多维度验证算例的正确性和合理性
3. **易于使用**: 简洁的API和清晰的文档
4. **可扩展**: 支持自定义参数和验证规则

使用这个工具包，您可以快速生成高质量的测试算例，大大提高遗传算法开发的效率！

---

**最后更新**: 2024年12月
**版本**: 1.0.0
**作者**: 算例工具包开发团队
