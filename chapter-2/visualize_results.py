#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
调度结果可视化工具
生成甘特图和打印机零件排布图
"""

import re
import matplotlib.pyplot as plt
import matplotlib.patches as patches
from matplotlib import font_manager
import numpy as np
import os
from typing import List, Dict, Tuple, Optional
import json

# 设置中文字体
plt.rcParams['font.sans-serif'] = ['SimHei', 'Arial Unicode MS', 'DejaVu Sans']
plt.rcParams['axes.unicode_minus'] = False

class ScheduleVisualizer:
    def __init__(self):
        self.machine_operations = {}
        self.job_operations = {}
        self.print_batches = {}
        self.makespan = 0.0

    def parse_output_file(self, filepath: str):
        """解析程序输出文件"""
        print(f"正在解析输出文件: {filepath}")

        with open(filepath, 'r', encoding='utf-8') as f:
            content = f.read()

        self._parse_machine_operations(content)
        self._parse_job_operations(content)
        self._parse_print_batches(content)
        self._parse_makespan(content)

        print("解析完成！")

    def _parse_machine_operations(self, content: str):
        """解析机器工序信息"""
        # 查找各机器的加工甘特图数据部分
        machine_pattern = r'(\w+机\d+)\s*\(机器\s*(\d+)\):.*?\n\s*总工序数:\s*(\d+).*?\n\s*完成时间:\s*([\d.]+).*?\n\s*工序详情:.*?\n\s*' + \
                         r'工件\s*工序\s*开始时间\s*结束时间\s*加工时间.*?\n\s*-+\n(.*?)(?=\n\s*\w+机|\n【|\n={10,}|$)'

        matches = re.findall(machine_pattern, content, re.DOTALL)
        for match in matches:
            machine_name, machine_id, total_ops, completion_time, ops_text = match
            machine_id = int(machine_id)

            operations = []
            # 解析每行工序
            for line in ops_text.strip().split('\n'):
                line = line.strip()
                if line and not line.startswith('工件'):
                    parts = re.split(r'\s+', line)
                    if len(parts) >= 5:
                        job = parts[0]
                        operation = parts[1]
                        start_time = float(parts[2])
                        end_time = float(parts[3])
                        process_time = float(parts[4])

                        # 提取工件编号
                        job_match = re.search(r'J(\d+)', job)
                        job_id = int(job_match.group(1)) if job_match else 0

                        operations.append({
                            'job': job_id,
                            'operation': operation,
                            'start': start_time,
                            'end': end_time,
                            'duration': process_time
                        })

            self.machine_operations[machine_id] = {
                'name': machine_name,
                'operations': operations,
                'completion_time': float(completion_time)
            }

    def _parse_job_operations(self, content: str):
        """解析工件加工路径"""
        # 查找各工件的加工路径部分
        job_pattern = r'工件\s*(\d+)\s*的加工路径:.*?\n\s*' + \
                     r'工序\s*机器\s*开始时间\s*结束时间\s*加工时间.*?\n\s*-+\n(.*?)\n\s*总完工时间:\s*([\d.]+)'

        matches = re.findall(job_pattern, content, re.DOTALL)
        for match in matches:
            job_id, ops_text, completion_time = match
            job_id = int(job_id)

            operations = []
            # 解析每行工序
            for line in ops_text.strip().split('\n'):
                line = line.strip()
                if line:
                    parts = re.split(r'\s+', line)
                    if len(parts) >= 5:
                        operation = parts[0]
                        machine = parts[1]
                        start_time = float(parts[2])
                        end_time = float(parts[3])
                        process_time = float(parts[4])

                        operations.append({
                            'operation': operation,
                            'machine': machine,
                            'start': start_time,
                            'end': end_time,
                            'duration': process_time
                        })

            self.job_operations[job_id] = {
                'operations': operations,
                'completion_time': float(completion_time)
            }

    def _parse_print_batches(self, content: str):
        """解析打印批次信息"""
        # 查找打印机批次信息
        batch_pattern = r'打印机\s*(\d+)\s*共有\s*(\d+)\s*个批次：(.*?(?=\n\s*打印机|\n【|\n={10,}|$))'

        matches = re.finditer(batch_pattern, content, re.DOTALL)
        for match in matches:
            printer_id, batch_count, batch_content = match.groups()
            printer_id = int(printer_id)

            batches = []
            # 解析每个批次
            batch_matches = re.findall(
                r'批次\s*(\d+).*?' +
                r'开始时间:\s*([\d.]+).*?' +
                r'结束时间:\s*([\d.]+).*?' +
                r'工件布局详情:.*?' +
                r'工件\s*X坐标\s*Y坐标\s*长\(L\)\s*宽\(W\)\s*高\(H\)\s*旋转.*?' +
                r'-+\n(.*?)(?=\n\s*└|\n\s*批次|\n\s*打印机|\n【|\n={10,}|$)',
                batch_content, re.DOTALL
            )

            for batch_match in batch_matches:
                batch_id, start_time, end_time, items_text = batch_match

                items = []
                for line in items_text.strip().split('\n'):
                    line = line.strip()
                    if line and 'J' in line:
                        parts = re.split(r'\s+', line)
                        if len(parts) >= 7:
                            job = parts[0]
                            x = float(parts[1])
                            y = float(parts[2])
                            l = float(parts[3])
                            w = float(parts[4])
                            h = float(parts[5])
                            rotate = parts[6] == '是'

                            # 提取工件编号
                            job_match = re.search(r'J(\d+)', job)
                            job_id = int(job_match.group(1)) if job_match else 0

                            items.append({
                                'job': job_id,
                                'x': x,
                                'y': y,
                                'l': l,
                                'w': w,
                                'h': h,
                                'rotate': rotate
                            })

                batches.append({
                    'batch_id': int(batch_id),
                    'start_time': float(start_time),
                    'end_time': float(end_time),
                    'items': items
                })

            self.print_batches[printer_id] = batches

    def _parse_makespan(self, content: str):
        """解析makespan"""
        makespan_match = re.search(r'最优makespan:\s*([\d.]+)', content)
        if makespan_match:
            self.makespan = float(makespan_match.group(1))

    def plot_gantt_chart(self, save_path: Optional[str] = None):
        """绘制甘特图"""
        print("正在生成甘特图...")

        if not self.machine_operations:
            print("未找到机器操作数据，无法生成甘特图")
            return

        # 计算需要的子图数量
        num_machines = len(self.machine_operations)
        cols = min(3, num_machines)
        rows = (num_machines + cols - 1) // cols

        fig, axes = plt.subplots(rows, cols, figsize=(15, 5 * rows))
        if rows == 1:
            axes = [axes] if cols == 1 else axes
        elif cols == 1:
            axes = axes.reshape(-1, 1)
        else:
            axes = axes.flatten()

        # 颜色映射
        colors = plt.cm.tab20(np.linspace(0, 1, 20))

        machine_ids = sorted(self.machine_operations.keys())

        for idx, machine_id in enumerate(machine_ids):
            if idx >= len(axes):
                break

            ax = axes[idx]
            machine_data = self.machine_operations[machine_id]

            # 绘制工序
            for op in machine_data['operations']:
                job_id = op['job']
                color = colors[job_id % len(colors)]

                ax.barh(f"J{job_id}",
                       op['end'] - op['start'],
                       left=op['start'],
                       height=0.6,
                       color=color,
                       alpha=0.8,
                       edgecolor='black',
                       linewidth=0.5)

                # 添加工序标签
                ax.text(op['start'] + (op['end'] - op['start']) / 2,
                       job_id,
                       f"{op['operation']}",
                       ha='center', va='center',
                       fontsize=8, fontweight='bold')

            ax.set_title(f"{machine_data['name']} 甘特图")
            ax.set_xlabel('时间')
            ax.set_ylabel('工件')
            ax.grid(True, alpha=0.3)

            # 设置x轴范围
            if self.makespan > 0:
                ax.set_xlim(0, self.makespan)

        # 隐藏多余的子图
        for idx in range(len(machine_ids), len(axes)):
            axes[idx].set_visible(False)

        plt.tight_layout()

        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"甘特图已保存到: {save_path}")
        else:
            plt.show()

    def plot_printer_layout(self, save_path: Optional[str] = None):
        """绘制打印机零件排布图"""
        print("正在生成打印机零件排布图...")

        if not self.print_batches:
            print("未找到打印批次数据，无法生成排布图")
            return

        # 计算需要的子图数量
        num_printers = len(self.print_batches)
        cols = min(2, num_printers)
        rows = (num_printers + cols - 1) // cols

        fig, axes = plt.subplots(rows, cols, figsize=(12, 6 * rows))
        if rows == 1 and cols == 1:
            axes = [axes]
        elif rows == 1:
            axes = axes if isinstance(axes, list) else [axes]
        else:
            axes = axes.flatten() if hasattr(axes, 'flatten') else [axes]

        # 颜色映射
        colors = plt.cm.Set3(np.linspace(0, 1, 12))

        printer_ids = sorted(self.print_batches.keys())

        for idx, printer_id in enumerate(printer_ids):
            if idx >= len(axes):
                break

            ax = axes[idx]
            batches = self.print_batches[printer_id]

            # 为每个批次绘制布局
            for batch in batches:
                batch_id = batch['batch_id']

                # 绘制每个零件
                for item in batch['items']:
                    job_id = item['job']
                    x, y = item['x'], item['y']
                    l, w = item['l'], item['w']

                    # 如果旋转，交换长宽
                    if item['rotate']:
                        l, w = w, l

                    color = colors[job_id % len(colors)]

                    # 绘制矩形
                    rect = patches.Rectangle((x, y), l, w,
                                           linewidth=1, edgecolor='black',
                                           facecolor=color, alpha=0.7)
                    ax.add_patch(rect)

                    # 添加工件标签
                    ax.text(x + l/2, y + w/2, f'J{job_id}',
                           ha='center', va='center',
                           fontsize=10, fontweight='bold')

                # 添加批次信息
                ax.text(0.02, 0.98,
                       f'批次 {batch_id}\n时间: {batch["start_time"]:.1f}-{batch["end_time"]:.1f}',
                       transform=ax.transAxes, fontsize=10,
                       verticalalignment='top',
                       bbox=dict(boxstyle='round', facecolor='wheat', alpha=0.8))

            ax.set_title(f'打印机 {printer_id} 零件排布图')
            ax.set_xlabel('X 坐标')
            ax.set_ylabel('Y 坐标')
            ax.set_aspect('equal')
            ax.grid(True, alpha=0.3)

            # 自动调整坐标轴范围
            if batches:
                all_items = [item for batch in batches for item in batch['items']]
                if all_items:
                    max_x = max(item['x'] + (item['w'] if item['rotate'] else item['l']) for item in all_items)
                    max_y = max(item['y'] + (item['l'] if item['rotate'] else item['w']) for item in all_items)
                    ax.set_xlim(0, max_x * 1.1)
                    ax.set_ylim(0, max_y * 1.1)

        # 隐藏多余的子图
        for idx in range(len(printer_ids), len(axes)):
            axes[idx].set_visible(False)

        plt.tight_layout()

        if save_path:
            plt.savefig(save_path, dpi=300, bbox_inches='tight')
            print(f"打印机排布图已保存到: {save_path}")
        else:
            plt.show()

    def generate_summary_report(self, save_path: str):
        """生成汇总报告"""
        print("正在生成汇总报告...")

        report = {
            'makespan': self.makespan,
            'machine_count': len(self.machine_operations),
            'job_count': len(self.job_operations),
            'printer_count': len(self.print_batches),
            'machine_utilization': {},
            'job_completion_times': {}
        }

        # 计算机器利用率
        for machine_id, data in self.machine_operations.items():
            total_time = data['completion_time']
            busy_time = sum(op['duration'] for op in data['operations'])
            utilization = busy_time / total_time if total_time > 0 else 0
            report['machine_utilization'][f"Machine_{machine_id}"] = {
                'name': data['name'],
                'utilization': utilization,
                'busy_time': busy_time,
                'total_time': total_time
            }

        # 工件完工时间
        for job_id, data in self.job_operations.items():
            report['job_completion_times'][f"Job_{job_id}"] = data['completion_time']

        # 保存JSON报告
        with open(save_path, 'w', encoding='utf-8') as f:
            json.dump(report, f, indent=2, ensure_ascii=False)

        print(f"汇总报告已保存到: {save_path}")

def main():
    """主函数"""
    print("=== 调度结果可视化工具 ===\n")

    # 默认输入文件路径
    default_output_file = "algorithm_output.txt"

    # 检查文件是否存在
    if not os.path.exists(default_output_file):
        print(f"错误：找不到输出文件 '{default_output_file}'")
        print("请确保算法输出已保存到该文件中")
        return

    # 创建可视化器
    visualizer = ScheduleVisualizer()

    try:
        # 解析输出文件
        visualizer.parse_output_file(default_output_file)

        # 创建输出目录
        output_dir = "visualization_results"
        os.makedirs(output_dir, exist_ok=True)

        # 生成甘特图
        gantt_path = os.path.join(output_dir, "gantt_chart.png")
        visualizer.plot_gantt_chart(gantt_path)

        # 生成打印机排布图
        layout_path = os.path.join(output_dir, "printer_layout.png")
        visualizer.plot_printer_layout(layout_path)

        # 生成汇总报告
        report_path = os.path.join(output_dir, "summary_report.json")
        visualizer.generate_summary_report(report_path)

        print("
✅ 可视化完成！"        print(f"输出目录: {output_dir}")
        print("文件列表:"
        print("  - gantt_chart.png: 甘特图")
        print("  - printer_layout.png: 打印机零件排布图")
        print("  - summary_report.json: 汇总报告")

    except Exception as e:
        print(f"可视化过程中出错: {e}")
        import traceback
        traceback.print_exc()

if __name__ == "__main__":
    main()
