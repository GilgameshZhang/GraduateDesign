package ProblemFrame;

public class SkyLine implements Comparable<SkyLine>{
    //天际线左端点X坐标
    public double x;
    //天际线左端点Y坐标
    public double y;
    //天际线长度
    public double len;

    public SkyLine() {
    }

    public SkyLine(double x, double y, double len) {
        this.x = x;
        this.y = y;
        this.len = len;
    }

    //天际线规则排序， y越小越优先， y一样时， x越小越优先
    @Override
    public int compareTo(SkyLine o) {
        int c1 = Double.compare(y, o.y);
        return c1 == 0 ? Double.compare(x, o.x) : c1;
    }

    //重写toString方法，方便打印查看天际线
    @Override
    public String toString() {
        return "SkyLine{" +
                "x =" + x +
                "y =" + y +
                ", len = " +len +
                '}';
    }

}
