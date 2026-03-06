#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
运行遗传算法并可视化结果的完整流程脚本
"""

import os
import subprocess
import sys
import time

def run_java_program(instance_file: str, output_file: str):
    """运行Java遗传算法程序"""
    print(f"正在运行遗传算法...")
    print(f"算例文件: {instance_file}")
    print(f"输出文件: {output_file}")

    # 编译Java文件
    print("\n1. 编译Java文件...")
    compile_cmd = ["javac", "-cp", "src/main/java", "-d", "build",
                   "src/test/java/RunAlgorithmExample.java",
                   "src/main/java/AlgorthmFrame/ga/GA.java",
                   "src/main/java/ProblemFrame/CaculateFitness.java",
                   "src/main/java/ProgramEntity/Input.java",
                   "src/main/java/AlgorthmFrame/ga/ChromosomeOperation.java"]

    try:
        result = subprocess.run(compile_cmd, capture_output=True, text=True, cwd="chapter-2")
        if result.returncode != 0:
            print("编译失败:")
            print(result.stderr)
            return False
    except FileNotFoundError:
        print("错误：找不到javac，请确保已安装JDK")
        return False

    # 运行程序
    print("\n2. 运行遗传算法...")
    run_cmd = ["java", "-cp", "build:src/main/java", "RunAlgorithmExample"]

    try:
        with open(output_file, 'w', encoding='utf-8') as f:
            result = subprocess.run(run_cmd, capture_output=True, text=True, cwd="chapter-2")

            # 写入所有输出
            f.write("=== 程序输出 ===\n")
            f.write(result.stdout)
            if result.stderr:
                f.write("\n=== 错误输出 ===\n")
                f.write(result.stderr)

        if result.returncode == 0:
            print("遗传算法运行成功！")
            return True
        else:
            print("遗传算法运行失败:")
            print(result.stderr)
            return False

    except FileNotFoundError:
        print("错误：找不到java，请确保已安装JRE")
        return False

def run_visualization(output_file: str):
    """运行可视化脚本"""
    print("
3. 生成可视化图表..."    vis_cmd = [sys.executable, "visualize_results.py"]

    try:
        # 修改可视化脚本中的输入文件路径
        with open("chapter-2/visualize_results.py", 'r', encoding='utf-8') as f:
            content = f.read()

        # 替换默认输入文件路径
        modified_content = content.replace(
            'default_output_file = "algorithm_output.txt"',
            f'default_output_file = r"{output_file}"'
        )

        with open("chapter-2/visualize_results.py", 'w', encoding='utf-8') as f:
            f.write(modified_content)

        # 运行可视化
        result = subprocess.run(vis_cmd, cwd="chapter-2", capture_output=True, text=True)

        if result.returncode == 0:
            print("可视化生成成功！")
            print(result.stdout)
            return True
        else:
            print("可视化生成失败:")
            print(result.stderr)
            return False

    except Exception as e:
        print(f"运行可视化时出错: {e}")
        return False

def main():
    """主函数"""
    print("=== 遗传算法调度与可视化完整流程 ===\n")

    # 配置参数
    instance_file = "src/main/resources/instance/J20P3B2D5_01.txt"  # 相对路径
    output_file = "algorithm_output.txt"
    full_output_path = f"chapter-2/{output_file}"

    # 检查算例文件是否存在
    if not os.path.exists(f"chapter-2/{instance_file}"):
        print(f"错误：找不到算例文件 '{instance_file}'")
        print("请检查文件路径是否正确")
        return

    start_time = time.time()

    # 步骤1：运行遗传算法
    if not run_java_program(instance_file, full_output_path):
        print("流程终止：遗传算法运行失败")
        return

    # 步骤2：生成可视化
    if not run_visualization(output_file):
        print("流程终止：可视化生成失败")
        return

    end_time = time.time()

    print(f"\n✅ 完整流程执行成功！总耗时: {end_time - start_time:.2f}秒")
    print("
生成的文件:"    print("  - algorithm_output.txt: 算法详细输出")
    print("  - visualization_results/gantt_chart.png: 甘特图")
    print("  - visualization_results/printer_layout.png: 打印机零件排布图")
    print("  - visualization_results/summary_report.json: 汇总报告")

    print("
🎯 可视化结果说明:"    print("  • 甘特图：展示每个机器上工序的时间安排")
    print("  • 打印机排布图：展示打印批次中零件在打印平台上的位置")
    print("  • 汇总报告：包含makespan、机器利用率等关键指标")

if __name__ == "__main__":
    main()
