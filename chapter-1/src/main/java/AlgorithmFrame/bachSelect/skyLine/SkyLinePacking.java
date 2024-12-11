package AlgorithmFrame.bachSelect.skyLine;



import ProblemFrame.Item;
import ProblemFrame.PlaceItem;
import ProblemFrame.SkyLine;
import ProblemFrame.Solution;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

import static util.compareUtil.compareDouble;


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
        //用来记录已放置矩形的总竖直高度
        double totalG = 0.0;
        //获取初始天际线
        skyLinePriorityQueue.add(new SkyLine(0, 0, W));

        //记录已经放置的矩形
        boolean[] used = new boolean[items.length];

        //开始天际线启发式迭代
        while (!skyLinePriorityQueue.isEmpty() && placeItemList.size() < items.length) {
            //获取当前最下最左的天际线（取出队首元素）
            SkyLine skyLine = skyLinePriorityQueue.poll();
            //初始化hl和hr
            double hl = H - skyLine.y;
            double hr = H - skyLine.y;
            //提前跳出器，当hl和hr都获取到的时候就可以跳出，节省时间
            int c = 0;
            //找天际线的空间即skyLine的hl和hr
            for (SkyLine line : skyLinePriorityQueue) {
                //由于skyLine是队首元素，所以它的y肯定最小，所以line.getY() - skyLine.getY()肯定大于等于0
                //如果两个天际线尾首相连是hl
                if (compareDouble(line.x + line.len, skyLine.x) == 0) {
                    hl = line.y - skyLine.y;
                    c++;
                //如果两个天际线首尾相连则是hr
                } else if (compareDouble(skyLine.x + skyLine.len, line.x) == 0) {
                    hr = line.y - skyLine.y;
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
                    int score = score(items[i].l, items[i].w, skyLine, hl, hr);
                    //更新最大评分
                    if (score > maxScore) {
                        maxScore = score;
                        maxItemIndex = i;
                        isRotate = false;
                    }
                    //旋转的情况
                    if (isRotateEnable) {
                        //宽高互换
                        int rotateScore = score(items[i].w, items[i].l, skyLine, hl, hr);
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
                totalS += (items[maxItemIndex].l * items[maxItemIndex].w);

                totalG += items[maxItemIndex].h;
            } else {
                //如果评分都小于0， 则说明该天际线放不下任何一个矩形，此时上移天际线，与其他天际线合并
                combineSkyLine(skyLine);
            }
        }
        //计算放置零件数值高度方差
        double avgG = totalG / placeItemList.size();
        double sG = 0.0;
        for (PlaceItem placeItem : placeItemList) {
            sG += Math.pow((placeItem.h - avgG),2) / placeItemList.size();
        }
        //返回求解结果
        return new Solution(placeItemList, sG, totalS, totalS/(W * H));
    }

    /**
     * 打包多盘（分批）
     * @return
     */
    public List<Solution> packings() {
        //存放打包好的托盘
        List<Solution> solutions = new ArrayList<>();
        //记录已经放置的矩形
        boolean[] used = new boolean[items.length];
        //计数器
        int counter = 0;
        while (counter < items.length) {
            //放置已经放置的矩形
            List<PlaceItem> placeItemList = new ArrayList<>();
            //用来记录已经放置矩形的总面积
            double totalS = 0.0;
            //记录零件的总竖直高度
            double maxG = 0.0;
            //清空天际线队列
            skyLinePriorityQueue.clear();
            //获取初始天际线
            skyLinePriorityQueue.add(new SkyLine(0, 0, W));

            //开始天际线启发式迭代
            while (!skyLinePriorityQueue.isEmpty() && placeItemList.size() < items.length) {
                //获取当前最下最左的天际线（取出队首元素）
                SkyLine skyLine = skyLinePriorityQueue.poll();
                //初始化hl和hr
                double hl = H - skyLine.y;
                double hr = H - skyLine.y;
                //提前跳出器，当hl和hr都获取到的时候就可以跳出，节省时间
                int c = 0;
                //找天际线的空间即skyLine的hl和hr
                for (SkyLine line : skyLinePriorityQueue) {
                    //由于skyLine是队首元素，所以它的y肯定最小，所以line.getY() - skyLine.getY()肯定大于等于0
                    //如果两个天际线尾首相连是hl
                    if (compareDouble(line.x + line.len, skyLine.x) == 0) {
                        hl = line.y - skyLine.y;
                        c++;
                        //如果两个天际线首尾相连则是hr
                    } else if (compareDouble(skyLine.x + skyLine.len, line.x) == 0) {
                        hr = line.y - skyLine.y;
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
                        int score = score(items[i].l, items[i].w, skyLine, hl, hr);
                        //更新最大评分
                        if (score > maxScore) {
                            maxScore = score;
                            maxItemIndex = i;
                            isRotate = false;
                        }
                        //旋转的情况
                        if (isRotateEnable) {
                            //宽高互换
                            int rotateScore = score(items[i].w, items[i].l, skyLine, hl, hr);
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
                    counter++;
                    //将该矩形面积追加到totalS中
                    totalS += (items[maxItemIndex].l * items[maxItemIndex].w);
                    if (compareDouble(items[maxItemIndex].h, maxG) == 1) {
                            maxG = items[maxItemIndex].h;
                    }
                } else {
                    //如果评分都小于0， 则说明该天际线放不下任何一个矩形，此时上移天际线，与其他天际线合并
                    combineSkyLine(skyLine);
                }
            }
            solutions.add(new Solution(placeItemList, maxG, totalS, totalS/(W * H)));
        }
        //返回求解结果
        return solutions;
    }

    /**
     * 传入一个放置不下任意矩形的天际线，将其上移，与其他天际线进行合并
     * @param skyLine 一个放置不下任意矩形的天际线
     */
    private void combineSkyLine(SkyLine skyLine) {
        boolean b = false;
        for (SkyLine line : skyLinePriorityQueue) {
            if (compareDouble(skyLine.y, line.y) != 1) {
                //如果头尾相连
                if (compareDouble(skyLine.x, line.x + line.len) == 0) {
                    skyLinePriorityQueue.remove(line);
                    b = true;
                    skyLine.x = line.x;
                    skyLine.y = line.y;
                    skyLine.len = line.len + skyLine.len;
                    break;
                }
                //如果尾头相连
                if (compareDouble(skyLine.x+ skyLine.len, line.x) == 0) {
                    skyLinePriorityQueue.remove(line);
                    b = true;
                    skyLine.y = line.y;
                    skyLine.len = line.len + skyLine.len;
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
     * @param w  当前要放置的矩形的高
     * @param l 当前要放置的矩形的宽
     * @param skyLine 该天际线对象
     * @param hl 该天际线的左墙
     * @param hr 该天际线的右墙
     * @return
     */
    private int score(double l, double w, SkyLine skyLine, double hl, double hr) {
        //如果当前天际线放不下当前矩形（当前天际线长度小于当前矩形的宽）
        if (compareDouble(skyLine.len, l) == -1) {
            return -1;
        }
        //如果超出上界，也不能放
        if (compareDouble(skyLine.y + w, H) == 1) {
            return -1;
        }
        //赋初值
        int score = -1;
        if (hl >= hl) {
            if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hl) == 0) {
                score = 7;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hr) == 0) {
                score = 6;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85  && compareDouble(w , hl) == 1) {
                score = 5;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hl) == 0) {
                score = 4;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hl) == -1 && compareDouble(w, hr) == 1) {
                score = 3;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hr) == 0) {
                score = 2;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hr) == -1) {
                score = 1;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hl) != 0) {
                score = 0;
            }
        } else {//当右墙高于左墙
            if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hr) == 0) {
                score = 7;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hl) == 0) {
                score = 6;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hr) == 1) {
                score = 5;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hr) == 0) {
                // 靠右
                score = 4;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hr) == -1 && compareDouble(w, hl) == 1) {
                score = 3;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hl) == 0) {
                score = 2;
            } else if (l <= skyLine.len && l > skyLine.len * 0.85 && compareDouble(w, hl) == -1) {
                score = 1;
            } else if (l <= skyLine.len * 0.85 && compareDouble(w, hr) != 0) {
                // 靠右
                score = 0;
            }
        }
        //左墙高于等于右墙
//        if (hl >= hl) {
//            if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hl) == 0) {
//                score = 7;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hr) == 0) {
//                score = 6;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w , hl) == 1) {
//                score = 5;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hl) == 0) {
//                score = 4;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hl) == -1 && compareDouble(w, hr) == 1) {
//                score = 3;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hr) == 0) {
//                score = 2;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hr) == -1) {
//                score = 1;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hl) != 0) {
//                score = 0;
//            }
//        } else {//当右墙高于左墙
//            if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hr) == 0) {
//                score = 7;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hl) == 0) {
//                score = 6;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hr) == 1) {
//                score = 5;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hr) == 0) {
//                // 靠右
//                score = 4;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hr) == -1 && compareDouble(w, hl) == 1) {
//                score = 3;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hl) == 0) {
//                score = 2;
//            } else if (compareDouble(l, skyLine.len) == 0 && compareDouble(w, hl) == -1) {
//                score = 1;
//            } else if (compareDouble(l, skyLine.len) == -1 && compareDouble(w, hr) != 0) {
//                // 靠右
//                score = 0;
//            }
//        }
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
            placeItem = new PlaceItem(item.name, skyLine.x + skyLine.len - item.l, skyLine.y, item.l, item.w, item.h, isRotate);
        } else {
            placeItem = new PlaceItem(item.name, skyLine.x + skyLine.len - item.w, skyLine.y, item.w, item.l, item.h, isRotate);
        }
        //更新天际线并加入队列
        addSkyLineInQueue(skyLine.x , skyLine.y, skyLine.len - placeItem.l);
        addSkyLineInQueue(placeItem.x, skyLine.y + placeItem.w, placeItem.l);
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
            placeItem = new PlaceItem(item.name, skyLine.x, skyLine.y, item.l, item.w, item.h, isRotate);
        } else {
          placeItem = new PlaceItem(item.name, skyLine.x, skyLine.y, item.w, item.l, item.h, isRotate);
        }
        //更新天际线并加入队列
        addSkyLineInQueue(skyLine.x, skyLine.y + placeItem.w, placeItem.l);
        addSkyLineInQueue(skyLine.x + placeItem.l, skyLine.y, skyLine.len - placeItem.l);
        return placeItem;
    }

//    /**
//     * 判断两个浮点数之间的关系
//     * @param d1
//     * @param d2
//     * @return 0代表两数相等， -1代表前者小于后者， 1代表前者大于后者
//     */
//    private int compareDouble(double d1, double d2) {
//        //当误差小于0.000001时认为二者相等
//        double error = 1e-08;
//        if (Math.abs(d1 - d2) < error) {
//            return 03;
//        } else if (d1 < d2) {
//            return -1;
//        } else  {
//            return 1;
//        }
//
//    }

}
