#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
为第二章算例添加能耗参数 - 实现向第三章格式的迁移

功能:
1. 读取第二章格式的算例文件
2. 在第3行后插入能耗参数 (第4-6行)
3. 输出第三章格式的算例文件

使用方法:
    python add_energy_params.py input.txt output.txt
    python add_energy_params.py input.txt output.txt --profile ECO
    python add_energy_params.py --batch chapter-2/src/main/resources/instance/J20/

作者: AI Assistant
日期: 2026-01-26
版本: 1.0
"""

import sys
import os
import argparse
from typing import List, Tuple

class EnergyProfile:
    """能耗配置类（已简化为统一配置）"""
    
    # 统一配置 - 所有机器使用相同的参数范围
    UNIFIED_PARAMS = {
        'P_run': (3.0, 5.0),       # 3-5 kW (加工功率)
        'P_idle': (1.0, 2.0),      # 1-2 kW (空载功率)
        'E_switch': (5.0, 10.0),   # 5-10 kWh (开关机能耗)
        'T_warmup': (60, 300),     # 60-300秒 (预热时间)
        'allowSwitch': True        # 所有机器都允许开关机
    }
    
    @staticmethod
    def get_params(index: int = 0) -> dict:
        """获取统一的能耗参数"""
        import random
        random.seed(1000 + index)
        
        prof = EnergyProfile.UNIFIED_PARAMS
        
        return {
            'P_run': random.uniform(*prof['P_run']),
            'P_idle': random.uniform(*prof['P_idle']),
            'E_switch': random.uniform(*prof['E_switch']),
            'T_warmup': random.uniform(*prof['T_warmup']),
            'allowSwitch': prof['allowSwitch']
        }


def parse_machine_counts(lines: List[str]) -> Tuple[int, int, int]:
    """
    解析机器数量
    
    返回: (printMachineCount, batchMachineCount, discreteMachineCount)
    """
    # 第2行: 打印机配置
    printer_line = lines[1].strip().split()
    print_count = int(printer_line[0])
    
    # 第3行: 批处理机配置
    batch_line = lines[2].strip().split()
    batch_count = int(batch_line[0])
    
    # 离散机数量为0
    discrete_count = 0
    
    return print_count, batch_count, discrete_count


def generate_energy_params(print_count: int, batch_count: int, 
                           printer_profile: str = 'UNIFIED',
                           batch_profile: str = 'UNIFIED') -> List[str]:
    """
    生成能耗参数行（已简化为统一配置）
    
    返回: [打印机能耗行, 批处理机能耗行, 离散机能耗行]
    """
    lines = []
    
    # 打印机能耗参数
    printer_params_parts = [str(print_count)]
    for i in range(print_count):
        params = EnergyProfile.get_params(i)
        printer_params_parts.append(
            f"{i+1} {params['P_run']:.2f} {params['P_idle']:.2f} "
            f"{params['E_switch']:.2f} {params['T_warmup']:.0f} "
            f"{1 if params['allowSwitch'] else 0}"
        )
    lines.append(' '.join(printer_params_parts))
    
    # 批处理机能耗参数
    batch_params_parts = [str(batch_count)]
    for i in range(batch_count):
        params = EnergyProfile.get_params(print_count + i)
        batch_params_parts.append(
            f"{i+1} {params['P_run']:.2f} {params['P_idle']:.2f} "
            f"{params['E_switch']:.2f} {params['T_warmup']:.0f} "
            f"{1 if params['allowSwitch'] else 0}"
        )
    lines.append(' '.join(batch_params_parts))
    
    # 离散机能耗参数 (当前为0)
    lines.append('0')
    
    return lines


def add_energy_params(input_path: str, output_path: str,
                     printer_profile: str = 'UNIFIED',
                     batch_profile: str = 'UNIFIED',
                     verbose: bool = True):
    """
    为算例添加能耗参数
    
    参数:
        input_path: 输入文件路径 (第二章格式)
        output_path: 输出文件路径 (第三章格式)
        printer_profile: 打印机能耗配置 (ECO/STANDARD/HEAVY/TEMP)
        batch_profile: 批处理机能耗配置 (ECO/STANDARD/HEAVY/TEMP)
        verbose: 是否输出详细信息
    """
    if verbose:
        print(f"处理: {input_path}")
    
    # 检查输入文件
    if not os.path.exists(input_path):
        print(f"错误: 输入文件不存在 - {input_path}")
        return False
    
    # 读取原始算例
    with open(input_path, 'r', encoding='utf-8') as f:
        lines = f.readlines()
    
    # 检查格式是否已经包含能耗参数
    if len(lines) >= 6:
        # 简单检测: 第4行是否包含多个浮点数
        try:
            tokens = lines[3].strip().split()
            if len(tokens) > 2 and all(is_number(t) for t in tokens[:3]):
                print(f"  跳过: 文件已包含能耗参数")
                return False
        except:
            pass
    
    # 解析机器数量
    try:
        print_count, batch_count, discrete_count = parse_machine_counts(lines)
        if verbose:
            print(f"  检测到: {print_count}台打印机, {batch_count}台批处理机")
    except Exception as e:
        print(f"  错误: 无法解析机器数量 - {e}")
        return False
    
    # 生成能耗参数
    energy_lines = generate_energy_params(print_count, batch_count,
                                         printer_profile, batch_profile)
    
    # 插入能耗参数 (在第3行后插入第4-6行)
    new_lines = lines[:3]  # 前3行保持不变
    new_lines.extend([line + '\n' for line in energy_lines])
    new_lines.extend(lines[3:])  # 工件信息
    
    # 写入输出文件
    os.makedirs(os.path.dirname(os.path.abspath(output_path)), exist_ok=True)
    
    with open(output_path, 'w', encoding='utf-8') as f:
        f.writelines(new_lines)
    
    if verbose:
        print(f"  ✓ 已生成: {output_path}")
        print(f"  能耗配置: Printer={printer_profile}, Batch={batch_profile}")
    
    return True


def is_number(s: str) -> bool:
    """检查字符串是否为数字"""
    try:
        float(s)
        return True
    except ValueError:
        return False


def batch_process(input_dir: str, output_dir: str = None,
                 printer_profile: str = 'UNIFIED',
                 batch_profile: str = 'UNIFIED'):
    """
    批量处理目录下的所有算例
    
    参数:
        input_dir: 输入目录
        output_dir: 输出目录 (默认为 input_dir + '_with_energy')
        printer_profile: 打印机能耗配置
        batch_profile: 批处理机能耗配置
    """
    if output_dir is None:
        output_dir = input_dir.rstrip('/').rstrip('\\') + '_with_energy'
    
    print(f"\n========== 批量处理 ==========")
    print(f"输入目录: {input_dir}")
    print(f"输出目录: {output_dir}")
    print(f"能耗配置: Printer={printer_profile}, Batch={batch_profile}")
    print("=" * 40)
    
    # 收集所有.txt文件
    txt_files = []
    for root, dirs, files in os.walk(input_dir):
        for file in files:
            if file.endswith('.txt'):
                txt_files.append(os.path.join(root, file))
    
    if not txt_files:
        print(f"错误: 未找到任何.txt文件")
        return
    
    print(f"\n找到 {len(txt_files)} 个算例文件\n")
    
    # 批量处理
    success_count = 0
    skip_count = 0
    fail_count = 0
    
    for input_path in txt_files:
        # 构造输出路径 (保持相对目录结构)
        rel_path = os.path.relpath(input_path, input_dir)
        output_path = os.path.join(output_dir, rel_path)
        
        result = add_energy_params(input_path, output_path,
                                   printer_profile, batch_profile,
                                   verbose=True)
        
        if result:
            success_count += 1
        elif result is False:
            skip_count += 1
        else:
            fail_count += 1
        
        print()  # 空行分隔
    
    # 统计信息
    print("=" * 40)
    print(f"批量处理完成:")
    print(f"  成功: {success_count} 个")
    print(f"  跳过: {skip_count} 个")
    print(f"  失败: {fail_count} 个")
    print("=" * 40)


def main():
    """主函数"""
    parser = argparse.ArgumentParser(
        description='为第二章算例添加能耗参数',
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog="""
示例:
  # 单文件处理
  python add_energy_params.py input.txt output.txt
  
  # 批量处理目录
  python add_energy_params.py --batch chapter-2/src/main/resources/instance/J20/
  
  # 批量处理并指定输出目录
  python add_energy_params.py --batch chapter-2/src/main/resources/instance/J20/ \\
      --output chapter-3/src/main/resources/instance/J20/

能耗配置 (统一配置):
  - 加工功率: 3-5 kW (随机)
  - 空载功率: 1-2 kW (随机)
  - 开关机能耗: 5-10 kWh (随机)
  - 所有机器都允许开关机
        """
    )
    
    parser.add_argument('input', nargs='?',
                       help='输入文件路径')
    parser.add_argument('output', nargs='?',
                       help='输出文件路径')
    parser.add_argument('--printer-profile', 
                       default='UNIFIED',
                       help='打印机能耗配置 (默认: UNIFIED，统一配置)')
    parser.add_argument('--batch-profile',
                       default='UNIFIED',
                       help='批处理机能耗配置 (默认: UNIFIED，统一配置)')
    parser.add_argument('--batch', metavar='DIR',
                       help='批量处理目录')
    parser.add_argument('--output-dir', metavar='DIR',
                       help='批量处理的输出目录 (默认: 输入目录_with_energy)')
    
    args = parser.parse_args()
    
    # 批量处理模式
    if args.batch:
        batch_process(args.batch, args.output_dir,
                     args.printer_profile, args.batch_profile)
        return
    
    # 单文件处理模式
    if not args.input or not args.output:
        parser.print_help()
        print("\n错误: 请提供输入文件和输出文件路径，或使用 --batch 进行批量处理")
        sys.exit(1)
    
    success = add_energy_params(args.input, args.output,
                               args.printer_profile, args.batch_profile,
                               verbose=True)
    
    if success:
        print("\n✓ 处理完成")
        sys.exit(0)
    else:
        print("\n✗ 处理失败")
        sys.exit(1)


if __name__ == '__main__':
    main()
