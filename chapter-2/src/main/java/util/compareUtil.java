package util;

public class compareUtil {
    /**
     * 判断两个浮点数之间的关系
     * @param d1
     * @param d2
     * @return 0代表两数相等， -1代表前者小于后者， 1代表前者大于后者
     */
    public static int compareDouble(double d1, double d2) {
        //当误差小于0.000001时认为二者相等
        double error = 1e-08;
        if (Math.abs(d1 - d2) < error) {
            return 0;
        } else if (d1 < d2) {
            return -1;
        } else  {
            return 1;
        }

    }
}
