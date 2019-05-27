package AI;

import java.util.ArrayList;

public class Point implements Comparable {
    public int x,y;
    public int role;
    public int scoreCom,scoreHum;
    public int score;
    public int step;
    public ArrayList<Point> steps;
    Point(int x,int y,int role){
        this.x=x;
        this.y=y;
        this.role=role;
    }

    @Override
    public int compareTo(Object o) {
        Point xx=(Point) o;
        if (score==xx.score) {
            // 大于零是优势，尽快获胜，因此取步数短的
            // 小于0是劣势，尽量拖延，因此取步数长的
            if (score >= 0) {
                if (step != xx.step)
                    return step - xx.step;
                else
                    return xx.score - score; // 否则 选取当前分最高的（直接评分)
            }
            else {
                if (step != xx.step)
                    return xx.step - step;
                else
                    return xx.score - score; // 否则 选取当前分最高的（直接评分)
            }
        }
        else
            return (xx.score - score);
    }
}
