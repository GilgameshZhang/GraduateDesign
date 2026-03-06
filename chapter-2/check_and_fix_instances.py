import os
import re
import random

def parse_instance_file(filename):
    """解析算例文件并检查离散处理部分"""
    print(f"\n{'='*80}")
    print(f"检查文件: {filename}")
    print(f"{'='*80}")
    
    with open(filename, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # 第1行：机器数 工件数
    line1 = lines[0].strip().split()
    total_machines = int(line1[0])
    job_count = int(line1[1])
    
    # 第2行：打印机信息
    line2 = lines[1].strip().split()
    print_machine_count = int(line2[0])
    
    # 第3行：批处理机信息
    line3 = lines[2].strip().split()
    batch_machine_count = int(line3[0])
    
    # 计算离散处理机数量
    discrete_machine_count = total_machines - print_machine_count - batch_machine_count
    
    print(f"机器配置: 总数={total_machines}, 打印={print_machine_count}, 批处理={batch_machine_count}, 离散={discrete_machine_count}")
    print(f"工件数量: {job_count}")
    
    # 离散处理机器编号范围
    discrete_start = print_machine_count + batch_machine_count + 1
    discrete_end = total_machines
    
    print(f"离散处理机器编号范围: M{discrete_start} - M{discrete_end}")
    
    issues = []
    fixed_lines = lines[:3]  # 保留前3行
    
    # 检查每个工件
    for job_idx in range(job_count):
        line_idx = 3 + job_idx
        if line_idx >= len(lines):
            issues.append(f"❌ 工件{job_idx+1}: 缺失数据")
            continue
        
        parts = lines[line_idx].strip().split()
        
        # 解析工件信息
        discrete_oper_count = int(parts[0])  # 离散工序数量
        length = int(parts[1])
        width = int(parts[2])
        height = int(parts[3])
        discrete_oper_count_confirm = int(parts[4])  # 确认离散工序数量
        
        if discrete_oper_count != discrete_oper_count_confirm:
            issues.append(f"❌ 工件{job_idx+1}: 离散工序数量不一致 ({discrete_oper_count} != {discrete_oper_count_confirm})")
        
        print(f"\n工件J{job_idx+1}: 尺寸({length}, {width}, {height}), 离散工序数={discrete_oper_count}")
        
        # 解析离散工序
        idx = 5  # 从第6个元素开始
        new_parts = parts[:5]  # 保留前5个元素
        
        for oper_idx in range(discrete_oper_count):
            if idx >= len(parts):
                issues.append(f"❌ 工件J{job_idx+1} 离散工序{oper_idx+1}: 数据不完整")
                break
            
            # 读取备选机器数量
            machine_count = int(parts[idx])
            idx += 1
            
            print(f"  离散工序{oper_idx+1}: 声明{machine_count}台备选机器", end="")
            
            # 读取机器-时间对
            machines_times = []
            valid_machines_times = []
            
            for _ in range(machine_count):
                if idx + 1 >= len(parts):
                    issues.append(f"❌ 工件J{job_idx+1} 离散工序{oper_idx+1}: 机器-时间对不完整")
                    break
                
                machine_no = int(parts[idx])
                time = int(parts[idx + 1])
                idx += 2
                machines_times.append((machine_no, time))
                
                # 检查机器编号是否在离散处理机范围内
                if machine_no < discrete_start or machine_no > discrete_end:
                    issues.append(f"❌ 工件J{job_idx+1} 离散工序{oper_idx+1}: 机器M{machine_no}不是离散处理机 (应在M{discrete_start}-M{discrete_end})")
                
                # 检查加工时间是否在600-1200范围内（且不为0）
                if time == 0:
                    issues.append(f"⚠️  工件J{job_idx+1} 离散工序{oper_idx+1}: 机器M{machine_no}的加工时间为0")
                    # 修复：生成600-1200的随机时间
                    time = random.randint(600, 1200)
                    print(f"\n    [修复] M{machine_no}: 0 → {time}", end="")
                elif time < 600 or time > 1200:
                    issues.append(f"⚠️  工件J{job_idx+1} 离散工序{oper_idx+1}: 机器M{machine_no}的加工时间{time}不在600-1200范围内")
                    # 修复：调整到600-1200范围
                    time = max(600, min(1200, time))
                    print(f"\n    [修复] M{machine_no}: {parts[idx-1]} → {time}", end="")
                
                valid_machines_times.append((machine_no, time))
            
            # 检查实际机器数量是否与声明一致
            actual_count = len(valid_machines_times)
            if actual_count != machine_count:
                issues.append(f"❌ 工件J{job_idx+1} 离散工序{oper_idx+1}: 声明{machine_count}台机器，实际{actual_count}台")
                # 修复：更新机器数量
                machine_count = actual_count
                print(f"\n    [修复] 更新备选机器数量为{actual_count}", end="")
            
            # 添加到新行
            new_parts.append(str(machine_count))
            for machine_no, time in valid_machines_times:
                new_parts.append(str(machine_no))
                new_parts.append(str(time))
            
            print(f" → 实际{actual_count}台: {', '.join([f'M{m}:{t}' for m, t in valid_machines_times])}")
        
        # 构建修复后的行
        fixed_lines.append(' '.join(new_parts) + '\n')
    
    return fixed_lines, issues

def main():
    instance_dir = 'src/main/resources/instance'
    
    if not os.path.exists(instance_dir):
        print(f"❌ 目录不存在: {instance_dir}")
        return
    
    files = sorted([f for f in os.listdir(instance_dir) if f.endswith('.txt')])
    
    print(f"找到{len(files)}个算例文件")
    
    all_issues = {}
    
    for filename in files:
        filepath = os.path.join(instance_dir, filename)
        try:
            fixed_lines, issues = parse_instance_file(filepath)
            
            if issues:
                all_issues[filename] = issues
                
                # 写回修复后的文件
                with open(filepath, 'w', encoding='utf-8') as f:
                    f.writelines(fixed_lines)
                
                print(f"\n✅ 已修复并保存: {filename}")
            else:
                print(f"\n✅ 无需修复: {filename}")
        
        except Exception as e:
            print(f"\n❌ 处理文件{filename}时出错: {e}")
            import traceback
            traceback.print_exc()
    
    # 汇总所有问题
    print(f"\n\n{'='*80}")
    print("问题汇总")
    print(f"{'='*80}")
    
    if all_issues:
        for filename, issues in all_issues.items():
            print(f"\n{filename}:")
            for issue in issues:
                print(f"  {issue}")
    else:
        print("✅ 所有算例文件都正确！")

if __name__ == '__main__':
    random.seed(42)  # 设置随机种子以保证可重复性
    main()

