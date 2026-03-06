#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os
import glob

def check_discrete_operation_times(file_path):
    """精确检查离散工序的加工时间"""
    issues = []
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        # 解析机器配置
        first_line = lines[0].strip().split()
        total_machines = int(first_line[0])
        total_jobs = int(first_line[1])
        
        printer_line = lines[1].strip().split()
        num_printers = int(printer_line[0])
        
        batch_line = lines[2].strip().split()
        num_batch = int(batch_line[0])
        
        num_discrete = total_machines - num_printers - num_batch
        discrete_start_id = num_printers + num_batch + 1
        
        # 检查每个工件
        for job_idx in range(total_jobs):
            job_line = lines[3 + job_idx].strip().split()
            
            if len(job_line) < 5:
                continue
            
            ops_total = int(job_line[0])
            ops_total_repeat = int(job_line[4])
            
            # 跳过前4个字段：工序数，L，W，H
            # 第5个字段是重复的工序数
            pos = 5
            
            # 跳过打印和批处理工序（每个工序3个字段：机器号，0，空闲标记）
            # 打印工序：num_printers个
            # 批处理工序：num_batch个
            for _ in range(num_printers + num_batch):
                if pos + 2 < len(job_line):
                    pos += 3  # 机器号，0（占位符），空闲标记
                else:
                    break
            
            # 现在处理离散工序
            discrete_op_idx = 0
            while pos < len(job_line) and discrete_op_idx < num_discrete:
                if pos >= len(job_line):
                    break
                
                # 读取备选机器数
                machines_count = int(job_line[pos])
                pos += 1
                
                # 读取每台备选机器的编号和加工时间
                for m_idx in range(machines_count):
                    if pos + 1 >= len(job_line):
                        issues.append(f'工件{job_idx+1}离散工序{discrete_op_idx+1}: 数据不完整')
                        break
                    
                    machine_id = int(job_line[pos])
                    time_val = int(job_line[pos + 1])
                    
                    # 检查时间值
                    if time_val == 0:
                        issues.append(f'工件{job_idx+1}离散工序{discrete_op_idx+1}机器{machine_id}: 时间为0')
                    elif time_val < 600 or time_val > 1200:
                        issues.append(f'工件{job_idx+1}离散工序{discrete_op_idx+1}机器{machine_id}: 时间{time_val}超范围')
                    
                    pos += 2
                
                discrete_op_idx += 1
    
    except Exception as e:
        issues.append(f'解析错误: {e}')
    
    return issues

def main():
    """检查所有算例文件"""
    instance_dir = "src/main/resources/instance"
    pattern = os.path.join(instance_dir, "*.txt")
    
    print("检查所有算例文件的离散工序加工时间...")
    print("=" * 70)
    
    problem_files = {}
    total_issues = 0
    
    for file_path in sorted(glob.glob(pattern)):
        issues = check_discrete_operation_times(file_path)
        if issues:
            problem_files[os.path.basename(file_path)] = issues
            total_issues += len(issues)
            print(f"\n❌ {os.path.basename(file_path)}: {len(issues)}个问题")
            for issue in issues[:5]:  # 显示前5个问题
                print(f"  • {issue}")
            if len(issues) > 5:
                print(f"  ... 还有 {len(issues) - 5} 个问题")
    
    print("\n" + "=" * 70)
    if problem_files:
        print(f"发现 {len(problem_files)} 个有问题的文件")
        print(f"共 {total_issues} 个离散工序时间问题")
    else:
        print("✅ 所有算例文件的离散工序时间都符合要求！")

if __name__ == "__main__":
    main()

