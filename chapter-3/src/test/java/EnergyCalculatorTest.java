import ProblemFrame.EnergyCalculator;
import ProblemFrame.PowerParameters;
import ProgramEntity.Operation;
import ProgramEntity.Problem;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * 能耗计算器测试类
 * 
 * 验证开/关机策略的能耗计算逻辑
 */
public class EnergyCalculatorTest {
    
    @Test
    public void testPowerParametersBreakEvenTime() {
        // 测试盈亏平衡时间计算
        PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
        
        // T_breakEven = E_switch / P_idle = 2.0 / 0.5 = 4.0
        double T_be = params.getBreakEvenTime();
        assertEquals(4.0, T_be, 1e-9);
        
        System.out.println("✓ 盈亏平衡时间计算正确: T_be = " + T_be);
    }
    
    @Test
    public void testShouldShutdownLogic() {
        PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
        
        // T_warmup = 5.0, T_breakEven = 4.0
        // 关机条件：gap >= T_warmup + T_breakEven = 9.0
        
        assertFalse("gap=3.0 < 5.0, 应该待机", params.shouldShutdown(3.0));
        assertFalse("gap=7.0 < 9.0, 应该待机", params.shouldShutdown(7.0));
        assertTrue("gap=10.0 >= 9.0, 应该关机", params.shouldShutdown(10.0));
        assertTrue("gap=100.0 >= 9.0, 应该关机", params.shouldShutdown(100.0));
        
        System.out.println("✓ 开关机判定逻辑正确");
    }
    
    @Test
    public void testDisallowSwitching() {
        // 不允许关机的设备
        PowerParameters params = new PowerParameters(3.0, 0.8, 5.0, 20.0, false);
        
        // 无论gap多大，都不应该关机
        assertFalse("allowSwitch=false, gap=100, 不应该关机", params.shouldShutdown(100.0));
        
        // 能耗应该按待机计算
        double energy = params.calculateGapEnergy(50.0);
        double expectedEnergy = 0.8 * 50.0; // P_idle * gap
        assertEquals(expectedEnergy, energy, 1e-6);
        
        System.out.println("✓ 不允许关机的设备正确按待机计算");
    }
    
    @Test
    public void testGapEnergyCalculation() {
        PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
        
        // Case 1: 短间隔（gap=3.0 < T_warmup=5.0），待机
        double gap1 = 3.0;
        double energy1 = params.calculateGapEnergy(gap1);
        double expectedIdle1 = 0.5 * 3.0; // P_idle * gap
        assertEquals("短间隔应按待机计算", expectedIdle1, energy1, 1e-6);
        
        // Case 2: 中间隔（gap=7.0, 7.0 < 9.0），待机
        double gap2 = 7.0;
        double energy2 = params.calculateGapEnergy(gap2);
        double expectedIdle2 = 0.5 * 7.0;
        assertEquals("中等间隔仍应待机", expectedIdle2, energy2, 1e-6);
        
        // Case 3: 长间隔（gap=20.0 >= 9.0），关机
        double gap3 = 20.0;
        double energy3 = params.calculateGapEnergy(gap3);
        double expectedShutdown = 2.0; // E_switch
        assertEquals("长间隔应关机", expectedShutdown, energy3, 1e-6);
        
        System.out.println("✓ 空闲间隔能耗计算正确:");
        System.out.println("  gap=3.0 -> " + energy1 + " kWh (待机)");
        System.out.println("  gap=7.0 -> " + energy2 + " kWh (待机)");
        System.out.println("  gap=20.0 -> " + energy3 + " kWh (关机)");
    }
    
    @Test
    public void testEnergyComparison() {
        // 验证长间隔时关机比待机节能
        PowerParameters params = new PowerParameters(5.0, 0.5, 2.0, 5.0, true);
        
        double longGap = 50.0;
        
        // 关机能耗
        double shutdownEnergy = params.E_switch; // 2.0
        
        // 待机能耗（如果不关机）
        double idleEnergy = params.P_idle * longGap; // 0.5 * 50.0 = 25.0
        
        // 关机应该更节能
        assertTrue("长间隔时关机应比待机节能", shutdownEnergy < idleEnergy);
        
        double savings = idleEnergy - shutdownEnergy;
        System.out.println("✓ 能耗节省验证: gap=50.0时");
        System.out.println("  待机能耗: " + idleEnergy + " kWh");
        System.out.println("  关机能耗: " + shutdownEnergy + " kWh");
        System.out.println("  节省: " + savings + " kWh (" + 
            String.format("%.1f%%", 100.0 * savings / idleEnergy) + ")");
    }
    
    @Test
    public void testMockMachineScheduleEnergy() {
        // 模拟一台机器的调度，测试完整能耗计算
        
        // 创建模拟工序
        Operation[] ops = new Operation[4];
        ops[0] = new Operation(0, 0, 0.0, 10.0, 0);    // 0-10
        ops[1] = new Operation(0, 1, 12.0, 15.0, 0);   // 12-15, gap=2.0 (待机)
        ops[2] = new Operation(0, 2, 20.0, 30.0, 0);   // 20-30, gap=5.0 (待机)
        ops[3] = new Operation(0, 3, 50.0, 60.0, 0);   // 50-60, gap=20.0 (关机)
        
        PowerParameters params = PowerParameters.createPrintMachineDefault();
        
        // 计算运行能耗
        double runEnergy = 0.0;
        for (Operation op : ops) {
            runEnergy += params.P_run * (op.endTime - op.startTime);
        }
        // (10-0)*5.0 + (15-12)*5.0 + (30-20)*5.0 + (60-50)*5.0 = 50+15+50+50 = 165.0
        
        // 计算空闲能耗
        double gap1 = 2.0;   // 待机: 0.5*2 = 1.0
        double gap2 = 5.0;   // 待机: 0.5*5 = 2.5 (因为 5 < 5(warmup) + 4(T_be) = 9)
        double gap3 = 20.0;  // 关机: 2.0
        
        double idleEnergy = params.calculateGapEnergy(gap1) + 
                           params.calculateGapEnergy(gap2) + 
                           params.calculateGapEnergy(gap3);
        // 1.0 + 2.5 + 2.0 = 5.5
        
        double totalEnergy = runEnergy + idleEnergy;
        
        System.out.println("✓ 模拟机器调度能耗:");
        System.out.println("  运行能耗: " + runEnergy + " kWh");
        System.out.println("  空闲能耗: " + idleEnergy + " kWh");
        System.out.println("  总能耗: " + totalEnergy + " kWh");
        
        // 验证总能耗非负
        assertTrue("总能耗应大于0", totalEnergy > 0);
        assertTrue("运行能耗应大于空闲能耗", runEnergy > idleEnergy);
    }
    
    @Test
    public void testZeroIdlePower() {
        // 测试P_idle=0的边界情况
        PowerParameters params = new PowerParameters(5.0, 0.0, 2.0, 5.0, true);
        
        // 当P_idle=0时，T_breakEven = +INF，永远不应该关机
        double T_be = params.getBreakEvenTime();
        assertEquals(Double.POSITIVE_INFINITY, T_be, 0.0);
        
        assertFalse("P_idle=0时不应该关机", params.shouldShutdown(1000.0));
        
        System.out.println("✓ P_idle=0的边界情况正确处理");
    }
}

