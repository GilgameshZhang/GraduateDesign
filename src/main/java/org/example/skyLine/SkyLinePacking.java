package org.example.skyLine;

import org.example.entity.Item;
import org.example.entity.PlaceItem;
import org.example.entity.SkyLine;
import org.example.entity.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public class SkyLinePacking {

    //边界的宽
    private double W;
    //边界的高
    private double H;
    //矩形数组
    private Item[] items;
    //是否可以旋转
    private boolean isRotateEnable;
    //天际线优先队列
    private PriorityQueue<SkyLine> skyLinePriorityQueue = new PriorityQueue<>();

    public SkyLinePacking() {
    }

    public SkyLinePacking(double w, double h, Item[] items, boolean isRotateEnable) {
        W = w;
        H = h;
        this.items = items;
        this.isRotateEnable = isRotateEnable;
    }

    public Solution packing() {
        //放置已经放置的矩形
        List<PlaceItem> placeItemList = new ArrayList<>();
        //用来记录已经放置矩形的总面积
        double totalS = 0.0;

        //获取初始天际线
        skyLinePriorityQueue.add(new SkyLine(0, 0, W));

        //记录已经放置的矩形
        boolean[] used = new boolean[items.length];

        //开始天际线启发式迭代
        while (!skyLinePriorityQueue.isEmpty() && placeItemList.size() < items.length) {
            //获取当前最下最左的天际线（取出队首元素）
            SkyLine skyLine = skyLinePriorityQueue.poll();
            //初始化hl和hr
            double hl = H - skyLine.getY();
            double hr = H - skyLine.getY();
            //提前跳出器，当hl和hr都获取到的时候就可以跳出，节省时间
            int c = 0;
            //找天际线的空间即skyLine的hl和hr
            for (SkyLine line : skyLinePriorityQueue) {
                //由于skyLine是队首元素，所以它的y肯定最小，所以line.getY() - skyLine.getY()肯定大于等于0
                //如果两个天际线尾首相连是hl
                if (compareDouble(line.getX() + line.getLen(), skyLine.getX()) == 0) {
                    hl = line.getY() - skyLine.getY();
                    c++;
                //如果两个天际线首尾相连则是hr
                } else if (compareDouble(skyLine.getX() + skyLine.getLen(), line.getX()) == 0) {
                    hr = line.getY() - skyLine.getY();
                    c++;
                }
                //如果c=2，则说明hl和hl均已找到
                if (c == 2) {
                    break;
                }
            }
            //记录最大评分矩阵的索引
            int maxItemIndex = -1;
            //记录最大评分的矩阵是否旋转
            boolean isRotate = false;
            //记录最大评分
            int maxScore = -1;
            //遍历矩阵，选取最大评分的矩形进行放置
            for (int i = 0; i < items.length; i++) {
                //判断矩形是否放置过
                if (!used[i]) {
                    //不旋转的情况
                    int score = score(items[i].getW(), items[i].getH(), skyLine, hl, hr);
                    //更新最大评分
                    if (score > maxScore) {
                        maxScore = score;
                        maxItemIndex = i;
                        isRotate = false;
                    }
                    //旋转的情况
                    if (isRotateEnable) {
                        //宽高互换
                        int rotateScore = score(items[i].getH(), items[i].getW(), skyLine, hl, hr);
                        //更新最大评分
                        if (rotateScore > maxScore) {
                            maxScore = rotateScore;
                            maxItemIndex = i;
                            isRotate = true;
                        }
                    }
                }
            }
            //如果当前最大得分大于等于0， 则说明可以放置，按照规则进行放置
            if (maxScore >= 0) {
                //如果左墙高于右墙
                if (hl >= hr) {
                    //评分为2时， 矩形靠天际线右边放置，否则靠左边放置
                    if (maxScore == 2) {
                        placeItemList.add(placeRight(items[maxItemIndex], skyLine, isRotate));
                    } else {
                        placeItemList.add(placeLeft(items[maxItemIndex], skyLine, isRotate));
                    }
                } else { //左墙低于右墙
                    //评分为4或0的时候， 矩形靠天际线右边放，否则靠左边放
                    if (maxScore == 4 || maxScore == 0) {
                        placeItemList.add(placeRight(items[maxItemIndex], skyLine, isRotate));
                    } else {
                        placeItemList.add(placeLeft(items[maxItemIndex], skyLine, isRotate));
                    }
                }
                //根据索引将该矩形设置为已用过状态
                used[maxItemIndex] = true;
                //将该矩形面积追加到totalS中
                totalS += (items[maxItemIndex].getW() * items[maxItemIndex].getH());
            } else {
                //如果评分都小于0， 则说明该天际线放不下任何一个矩形，此时上移天际线，与其他天际线合并
                combineSkyLine(skyLine);
            }
        }
        //返回求解结果
        return new Solution(placeItemList, totalS, totalS/(W * H));
    }

    /**
     * 传入一个放置不下任意矩形的天际线，将其上移，与其他天际线进行合并
     * @param skyLine 一个放置不下任意矩形的天际线
     */
    private void combineSkyLine(SkyLine skyLine) {
        boolean b = false;
        for (SkyLine line : skyLinePriorityQueue) {
            if (compareDouble(skyLine.getY(), line.getY()) != 1) {
                //如果头尾相连
                if (compareDouble(skyLine.getX(), line.getX() + line.getLen()) == 0) {
                    skyLinePriorityQueue.remove(line);
                    b = true;
                    skyLine.setX(line.getX());
                    skyLine.setY(line.getY());
                    skyLine.setLen(line.getLen() + skyLine.getLen());
                    break;
                }
                //如果尾头相连
                if (compareDouble(skyLine.getX() + skyLine.getLen(), line.getX()) == 0) {
                    skyLinePriorityQueue.remove(line);
                    b = true;
                    skyLine.setY(line.getY());
                    skyLine.setLen(line.getLen() + skyLine.getLen());
                    break;
                }
            }
        }
        //如果有合并，加入队列中
        if (b) {
            //将合并好的天际线加入队列
            skyLinePriorityQueue.add(skyLine);
        }
    }

    /**
     * 矩形块的评分，如果评分为 -1 ，则说明该矩形不能放置在该天际线上
     * @param h  当前要放置的矩形的高
     * @param w 当前要放置的矩形的宽
     * @param skyLine 该天际线对象
     * @param hl 该天际线的左墙
     * @param hr 该天际线的右墙
     * @return
     */
    private int score(double w, double h, SkyLine skyLine, double hl, double hr) {
        //如果当前天际线放不下当前矩形（当前天际线长度小于当前矩形的宽）
        if (compareDouble(skyLine.getLen(), w) == -1) {
            return -1;
        }
        //如果超出上界，也不能放
        if (compareDouble(skyLine.getY() + h, H) == 1) {
            return -1;
        }
        //赋初值
        int score = -1;
        //左墙高于等于右墙
        if (hl >= hl) {
            if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == 0) {
                score = 7;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 0) {
                score = 6;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h , hl) == 1) {
                score = 5;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) == 0) {
                score = 4;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == -1 && compareDouble(h, hr) == 1) {
                score = 3;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) == 0) {
                score = 2;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == -1) {
                score = 1;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) != 0) {
                score = 0;
            }
        } else {//当右墙高于左墙
            if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 0) {
                score = 7;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == 0) {
                score = 6;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == 1) {
                score = 5;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) == 0) {
                // 靠右
                score = 4;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hr) == -1 && compareDouble(h, hl) == 1) {
                score = 3;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hl) == 0) {
                score = 2;
            } else if (compareDouble(w, skyLine.getLen()) == 0 && compareDouble(h, hl) == -1) {
                score = 1;
            } else if (compareDouble(w, skyLine.getLen()) == -1 && compareDouble(h, hr) != 0) {
                // 靠右
                score = 0;
            }
        }
        return score;
    }

    /**
     * 将矩形靠右放置
     * @param item 要放置的对象
     * @param skyLine 矩形位置所在的天际线
     * @param isRotate 举行是否旋转
     * @return 返回放置好的矩形对象
     */
    private PlaceItem placeRight(Item item, SkyLine skyLine, boolean isRotate) {
        //生成PlaceItem对象
        PlaceItem placeItem = null;
        if (!isRotate) {
            placeItem = new PlaceItem(item.getName(), skyLine.getX() + skyLine.getLen() - item.getW(), skyLine.getY(), item.getW(), item.getH(), isRotate);
        } else {
            placeItem = new PlaceItem(item.getName(), skyLine.getX() + skyLine.getLen() - item.getH(), skyLine.getY(), item.getH(), item.getW(), isRotate);
        }
        //更新天际线并加入队列
        addSkyLineInQueue(skyLine.getX() , skyLine.getY(), skyLine.getLen() - placeItem.getW());
        addSkyLineInQueue(placeItem.getX(), skyLine.getY() + placeItem.getH(), placeItem.getW());
        return placeItem;
    }

    /**
     * 将指定属性的天际线加入到天际线队列中（length > 0才能加入）
     * @param x 新天际线的x坐标
     * @param y 新天际线的y坐标
     * @param length 天际线长度
     */
    private void addSkyLineInQueue(double x, double y, double length) {
        //新天际线长度大于0才加入
        if (compareDouble(length, 0.0) == 1) {
            skyLinePriorityQueue.add(new SkyLine(x, y, length));
        }
    }

    /**
     * 将矩形靠左放置
     * @param item 要放置的对象
     * @param skyLine 矩形位置所在的天际线
     * @param isRotate 举行是否旋转
     * @return 返回放置好的矩形对象
     */
    private PlaceItem placeLeft(Item item, SkyLine skyLine, boolean isRotate) {
        //生成PlaceItem对象
        PlaceItem placeItem = null;
        if (!isRotate) {
            placeItem = new PlaceItem(item.getName(), skyLine.getX(), skyLine.getY(), item.getW(), item.getH(), isRotate);
        } else {
          placeItem = new PlaceItem(item.getName(), skyLine.getX(), skyLine.getY(), item.getH(), item.getW(), isRotate);
        }
        //更新天际线并加入队列
        addSkyLineInQueue(skyLine.getX(), skyLine.getY() + placeItem.getH(), placeItem.getW());
        addSkyLineInQueue(skyLine.getX() + placeItem.getW(), skyLine.getY(), skyLine.getLen() - placeItem.getW());
        return placeItem;
    }

    /**
     * 判断两个浮点数之间的关系
     * @param d1
     * @param d2
     * @return 0代表两数相等， -1代表前者小于后者， 1代表前者大于后者
     */
    private int compareDouble(double d1, double d2) {
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
