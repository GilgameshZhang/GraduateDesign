package ProgramEntity;

/**
 * 已经放置过的对象
 */
public class PlaceItem {
    //名字
    public String name;
    //x坐标
    public double x;
    //y坐标
    public double y;
    //宽
    public double w;
    //长
    public double l;
    //高
    public double h;
    //是否旋转
    public boolean isRotate;

    public PlaceItem() {
    }

    public PlaceItem(String name, double x, double y, double l, double w, double h, boolean isRotate) {
        this.name = name;
        this.x = x;
        this.y = y;
        this.w = w;
        this.l = l;
        this.h = h;
        this.isRotate = isRotate;
    }
    
    /**
     * 拷贝构造函数
     * 
     * @param other 要拷贝的PlaceItem对象
     */
    public PlaceItem(PlaceItem other) {
        if (other == null) {
            return;
        }
        
        this.name = other.name;
        this.x = other.x;
        this.y = other.y;
        this.w = other.w;
        this.l = other.l;
        this.h = other.h;
        this.isRotate = other.isRotate;
    }
}

