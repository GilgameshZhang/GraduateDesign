#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import glob

def check_processing_times(file_path):
    """检查算例文件中的加工时间"""
    print(f"检查文件: {os.path.basename(file_path)}")

    issues = []
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # 解析第一行获取机器信息
        first_line = lines[0].strip().split()
        total_machines = int(first_line[0])

        # 解析机器配置
        printer_line = lines[1].strip().split()
        num_printers = int(printer_line[0])

        batch_line = lines[2].strip().split()
        num_batch = int(batch_line[0])

        # 离散机范围
        discrete_start = num_printers + num_batch + 1
        discrete_end = total_machines

        # 检查每个工件
        total_jobs = int(first_line[1])
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()

            ops_total = int(job_line[0])
            pos = 5  # 从工序数据开始

            for op_idx in range(ops_total):
                if pos >= len(job_line):
                    break

                machines_count = int(job_line[pos])
                pos += 1

                # 收集该工序的机器和时间
                for m_idx in range(machines_count):
                    if pos + 1 >= len(job_line):
                        issues.append(f"工件{job_idx+1}工序{op_idx+1}: 数据不完整")
                        break

                    machine_id = int(job_line[pos])
                    time_val = int(job_line[pos + 1])

                    # 检查是否为离散工序（机器编号在离散机范围内）
                    if discrete_start <= machine_id <= discrete_end:
                        if time_val == 0:
                            issues.append(f"工件{job_idx+1}工序{op_idx+1}机器{machine_id}: 加工时间为0")
                        elif time_val < 600 or time_val > 1200:
                            issues.append(f"工件{job_idx+1}工序{op_idx+1}机器{machine_id}: 加工时间{time_val}超出范围(600-1200)")

                    pos += 2

    except Exception as e:
        issues.append(f"文件解析错误: {e}")

    if issues:
        print(f"❌ 发现 {len(issues)} 个问题:")
        for issue in issues[:10]:  # 只显示前10个
            print(f"  • {issue}")
        if len(issues) > 10:
            print(f"  ... 还有 {len(issues) - 10} 个问题")
        return issues
    else:
        print("✅ 所有加工时间检查通过")
        return []

def fix_processing_times(file_path, issues):
    """修复加工时间问题"""
    print(f"修复文件: {os.path.basename(file_path)}")

    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()

        # 解析机器信息
        first_line = lines[0].strip().split()
        total_machines = int(first_line[0])

        printer_line = lines[1].strip().split()
        num_printers = int(printer_line[0])

        batch_line = lines[2].strip().split()
        num_batch = int(batch_line[0])

        discrete_start = num_printers + num_batch + 1

        # 修复每个工件
        total_jobs = int(first_line[1])
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()

            ops_total = int(job_line[0])
            pos = 5

            for op_idx in range(ops_total):
                if pos >= len(job_line):
                    break

                machines_count = int(job_line[pos])
                pos += 1

                for m_idx in range(machines_count):
                    if pos + 1 >= len(job_line):
                        break

                    machine_id = int(job_line[pos])

                    # 如果是离散工序，修复时间
                    if discrete_start <= machine_id <= total_machines:
                        current_time = int(job_line[pos + 1])
                        if current_time == 0 or current_time < 600 or current_time > 1200:
                            # 生成新的随机时间
                            import random
                            new_time = random.randint(600, 1200)
                            job_line[pos + 1] = str(new_time)
                            print(f"  修复 工件{job_idx+1}工序{op_idx+1}机器{machine_id}: {current_time} → {new_time}")

                    pos += 2

            # 更新行
            lines[3 + job_idx] = ' '.join(job_line) + '\n'

        # 写回文件
        with open(file_path, 'w', encoding='utf-8') as f:
            f.writelines(lines)

        print("✅ 修复完成")

    except Exception as e:
        print(f"❌ 修复失败: {e}")

def main():
    """检查并修复所有算例文件"""
    instance_dir = "src/main/resources/instance"
    pattern = os.path.join(instance_dir, "*.txt")

    print("🔍 检查所有算例文件的加工时间...")
    print("=" * 60)

    all_issues = {}
    for file_path in glob.glob(pattern):
        issues = check_processing_times(file_path)
        if issues:
            all_issues[file_path] = issues
        print()

    if all_issues:
        print("🔧 开始修复问题...")
        print("=" * 60)

        for file_path, issues in all_issues.items():
            fix_processing_times(file_path, issues)
            print()

        print("🎯 修复总结:")
        print(f"修复了 {len(all_issues)} 个文件")
        total_issues = sum(len(issues) for issues in all_issues.values())
        print(f"共修复了 {total_issues} 个加工时间问题")
    else:
        print("🎉 所有算例文件的加工时间都符合要求！")

if __name__ == "__main__":
    main()
