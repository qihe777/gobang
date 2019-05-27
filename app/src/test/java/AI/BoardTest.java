package AI;

import org.junit.Test;

import static org.junit.Assert.*;

public class BoardTest {

    @Test
    public void findPoint() {
        Board board=new Board(0);
        board.put(7,7,ConstanNum.HUMEN);
        Point x=board.findPoint(ConstanNum.COM,ConstanNum.searchDeep);
        System.out.println(x.x+":"+x.y);
        board.put(7,8,ConstanNum.HUMEN);
        x=board.findPoint(ConstanNum.COM,ConstanNum.searchDeep);
        System.out.println(x.x+":"+x.y);
    }
}