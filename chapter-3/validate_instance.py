#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
快速验证生成的算例格式

功能:
1. 验证算例格式是否正确
2. 显示算例基本信息
3. 验证能耗参数是否合理

使用方法:
    python validate_instance.py instance_file.txt
    python validate_instance.py --batch instance_dir/

作者: AI Assistant
日期: 2026-01-26
"""

import sys
import os
import argparse
from typing import Dict, List, Tuple


class InstanceValidator:
    """算例验证器"""
    
    def __init__(self, filepath: str):
        self.filepath = filepath
        self.lines = []
        self.errors = []
        self.warnings = []
        
        # 解析结果
        self.machine_count = 0
        self.job_count = 0
        self.print_count = 0
        self.batch_count = 0
        self.discrete_count = 0
        self.has_energy_params = False
        
    def load(self) -> bool:
        """加载文件"""
        try:
            with open(self.filepath, 'r', encoding='utf-8') as f:
                self.lines = [line.strip() for line in f.readlines() if line.strip()]
            return True
        except Exception as e:
            self.errors.append(f"无法读取文件: {e}")
            return False
    
    def validate(self) -> bool:
        """验证算例格式"""
        if not self.load():
            return False
        
        # 检查最小行数
        if len(self.lines) < 4:
            self.errors.append(f"文件行数不足 (至少需要4行，实际{len(self.lines)}行)")
            return False
        
        # 验证第1行: 问题规模
        if not self._validate_line1():
            return False
        
        # 验证第2行: 打印机配置
        if not self._validate_line2():
            return False
        
        # 验证第3行: 批处理机配置
        if not self._validate_line3():
            return False
        
        # 检测是否包含能耗参数
        self._detect_energy_params()
        
        if self.has_energy_params:
            # 验证第4-6行: 能耗参数
            if not self._validate_energy_params():
                return False
            
            # 验证工件信息 (从第7行开始)
            job_start_line = 6
        else:
            # 验证工件信息 (从第4行开始)
            job_start_line = 3
            self.warnings.append("未检测到能耗参数 (兼容第二章格式)")
        
        # 验证工件信息
        if not self._validate_jobs(job_start_line):
            return False
        
        # 验证机器数量一致性
        if self.machine_count != self.print_count + self.batch_count + self.discrete_count:
            self.errors.append(
                f"机器总数不一致: 声明{self.machine_count}台, "
                f"实际{self.print_count}打印+{self.batch_count}批处理+{self.discrete_count}离散"
            )
            return False
        
        return len(self.errors) == 0
    
    def _validate_line1(self) -> bool:
        """验证第1行: machineCount jobCount"""
        try:
            tokens = self.lines[0].split()
            if len(tokens) != 2:
                self.errors.append(f"第1行格式错误: 应为'machineCount jobCount'")
                return False
            
            self.machine_count = int(tokens[0])
            self.job_count = int(tokens[1])
            
            if self.machine_count <= 0 or self.job_count <= 0:
                self.errors.append(f"第1行数值错误: 机器数和工件数必须>0")
                return False
            
            return True
        except Exception as e:
            self.errors.append(f"第1行解析失败: {e}")
            return False
    
    def _validate_line2(self) -> bool:
        """验证第2行: 打印机配置"""
        try:
            tokens = self.lines[1].split()
            self.print_count = int(tokens[0])
            
            # 每台打印机7个参数 (id L W H printSpeed recordTime prepareTime)
            expected_tokens = 1 + self.print_count * 7
            if len(tokens) != expected_tokens:
                self.errors.append(
                    f"第2行格式错误: 期望{expected_tokens}个参数, 实际{len(tokens)}个"
                )
                return False
            
            # 验证每台打印机的参数
            for i in range(self.print_count):
                base = 1 + i * 7
                machine_id = int(tokens[base])
                L = float(tokens[base + 1])
                W = float(tokens[base + 2])
                H = float(tokens[base + 3])
                
                if L <= 0 or W <= 0 or H <= 0:
                    self.warnings.append(f"打印机{machine_id}尺寸异常: L={L}, W={W}, H={H}")
            
            return True
        except Exception as e:
            self.errors.append(f"第2行解析失败: {e}")
            return False
    
    def _validate_line3(self) -> bool:
        """验证第3行: 批处理机配置"""
        try:
            tokens = self.lines[2].split()
            self.batch_count = int(tokens[0])
            
            # 每台批处理机2个参数 (id capacity)
            expected_tokens = 1 + self.batch_count * 2
            if len(tokens) != expected_tokens:
                self.errors.append(
                    f"第3行格式错误: 期望{expected_tokens}个参数, 实际{len(tokens)}个"
                )
                return False
            
            return True
        except Exception as e:
            self.errors.append(f"第3行解析失败: {e}")
            return False
    
    def _detect_energy_params(self):
        """检测是否包含能耗参数"""
        if len(self.lines) < 6:
            self.has_energy_params = False
            return
        
        try:
            # 检查第4行是否为能耗参数格式
            tokens = self.lines[3].split()
            count = int(tokens[0])
            
            # 能耗参数格式: count [id P_run P_idle E_switch T_warmup allowSwitch] ...
            # 每台机器6个参数
            expected = 1 + count * 6
            
            if len(tokens) == expected:
                self.has_energy_params = True
            else:
                self.has_energy_params = False
        except:
            self.has_energy_params = False
    
    def _validate_energy_params(self) -> bool:
        """验证能耗参数"""
        try:
            # 第4行: 打印机能耗
            tokens4 = self.lines[3].split()
            count4 = int(tokens4[0])
            if count4 != self.print_count:
                self.errors.append(
                    f"第4行能耗参数错误: 打印机数量不匹配 (声明{self.print_count}, 能耗{count4})"
                )
                return False
            
            expected4 = 1 + count4 * 6
            if len(tokens4) != expected4:
                self.errors.append(
                    f"第4行能耗参数格式错误: 期望{expected4}个参数, 实际{len(tokens4)}个"
                )
                return False
            
            # 验证能耗参数合理性
            for i in range(count4):
                base = 1 + i * 6
                P_run = float(tokens4[base + 1])
                P_idle = float(tokens4[base + 2])
                E_switch = float(tokens4[base + 3])
                T_warmup = float(tokens4[base + 4])
                
                if P_run <= 0 or P_idle < 0:
                    self.warnings.append(f"打印机{i+1}功率异常: P_run={P_run}, P_idle={P_idle}")
                
                if P_idle >= P_run:
                    self.warnings.append(f"打印机{i+1}待机功率>=运行功率")
                
                if E_switch < 0 or T_warmup < 0:
                    self.warnings.append(f"打印机{i+1}能耗参数异常: E_switch={E_switch}, T_warmup={T_warmup}")
            
            # 第5行: 批处理机能耗
            tokens5 = self.lines[4].split()
            count5 = int(tokens5[0])
            if count5 != self.batch_count:
                self.errors.append(
                    f"第5行能耗参数错误: 批处理机数量不匹配 (声明{self.batch_count}, 能耗{count5})"
                )
                return False
            
            # 第6行: 离散机能耗
            tokens6 = self.lines[5].split()
            self.discrete_count = int(tokens6[0])
            
            return True
        except Exception as e:
            self.errors.append(f"能耗参数解析失败: {e}")
            return False
    
    def _validate_jobs(self, start_line: int) -> bool:
        """验证工件信息"""
        try:
            job_lines = self.lines[start_line:]
            
            if len(job_lines) != self.job_count:
                self.errors.append(
                    f"工件数量不匹配: 声明{self.job_count}个, 实际{len(job_lines)}行"
                )
                return False
            
            for job_idx, line in enumerate(job_lines):
                tokens = line.split()
                if len(tokens) < 5:
                    self.errors.append(f"工件{job_idx+1}格式错误: 参数不足")
                    return False
                
                ops_count = int(tokens[0])
                L = float(tokens[1])
                W = float(tokens[2])
                H = float(tokens[3])
                
                if L <= 0 or W <= 0 or H <= 0:
                    self.warnings.append(f"工件{job_idx+1}尺寸异常: L={L}, W={W}, H={H}")
                
                # 简单验证工序信息格式
                # 完整验证比较复杂，这里只做基本检查
                if ops_count <= 0:
                    self.errors.append(f"工件{job_idx+1}工序数错误: {ops_count}")
                    return False
            
            return True
        except Exception as e:
            self.errors.append(f"工件信息解析失败: {e}")
            return False
    
    def print_report(self):
        """打印验证报告"""
        print(f"\n{'='*60}")
        print(f"文件: {os.path.basename(self.filepath)}")
        print(f"{'='*60}")
        
        if self.errors:
            print("\n❌ 验证失败\n")
            for err in self.errors:
                print(f"  [错误] {err}")
        else:
            print("\n✅ 验证通过\n")
        
        if self.warnings:
            print()
            for warn in self.warnings:
                print(f"  [警告] {warn}")
        
        print(f"\n基本信息:")
        print(f"  机器总数: {self.machine_count}")
        print(f"  - 打印机: {self.print_count}")
        print(f"  - 批处理机: {self.batch_count}")
        print(f"  - 离散机: {self.discrete_count}")
        print(f"  工件数: {self.job_count}")
        print(f"  包含能耗参数: {'是' if self.has_energy_params else '否'}")
        print(f"  格式版本: {'v2.0 (第三章)' if self.has_energy_params else 'v1.0 (第二章)'}")
        
        print(f"{'='*60}\n")


def batch_validate(directory: str):
    """批量验证目录下的所有算例"""
    print(f"\n{'='*60}")
    print(f"批量验证: {directory}")
    print(f"{'='*60}\n")
    
    txt_files = []
    for root, dirs, files in os.walk(directory):
        for file in files:
            if file.endswith('.txt'):
                txt_files.append(os.path.join(root, file))
    
    if not txt_files:
        print("未找到任何.txt文件")
        return
    
    print(f"找到 {len(txt_files)} 个算例文件\n")
    
    pass_count = 0
    fail_count = 0
    
    for filepath in txt_files:
        validator = InstanceValidator(filepath)
        is_valid = validator.validate()
        
        if is_valid:
            pass_count += 1
            print(f"✅ {os.path.basename(filepath)}")
        else:
            fail_count += 1
            print(f"❌ {os.path.basename(filepath)}")
            for err in validator.errors[:2]:  # 只显示前2个错误
                print(f"    {err}")
    
    print(f"\n{'='*60}")
    print(f"验证完成: 通过 {pass_count}/{len(txt_files)}, 失败 {fail_count}/{len(txt_files)}")
    print(f"{'='*60}\n")


def main():
    parser = argparse.ArgumentParser(description='验证算例格式')
    parser.add_argument('input', nargs='?', help='算例文件路径')
    parser.add_argument('--batch', metavar='DIR', help='批量验证目录')
    
    args = parser.parse_args()
    
    if args.batch:
        batch_validate(args.batch)
    elif args.input:
        validator = InstanceValidator(args.input)
        validator.validate()
        validator.print_report()
    else:
        parser.print_help()
        print("\n错误: 请提供算例文件路径或使用 --batch 批量验证")
        sys.exit(1)


if __name__ == '__main__':
    main()
