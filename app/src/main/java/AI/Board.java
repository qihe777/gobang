package AI;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

public class Board {
    //当前棋盘
    private int [][]board;
    //分数缓存
    private int [][][][]scoreCache;
    private int [][]comScore;
    public int [][]humScore;
    private Zobirst zobirst;
    private int count;
    private int len;
    //当前一步的思考
    private ArrayList<Point> currentSteps,allSteps;
    private int maxScore,minScore;
    private HashMap<Integer,CacheObj> caChe;
    //开始时间
    private Date startDate;
    private boolean comFirst;
    public Board() {
        //需要在这里赋值
        len=ConstanNum.GRID_NUMBER;
        zobirst=new Zobirst();
        count=0;
        board=new int[len][len];
        comScore=new int[len][len];
        humScore=new int[len][len];
        scoreCache=new int[3][4][len][len];
        currentSteps=new ArrayList<>();
        allSteps=new ArrayList<>();
        comFirst=false;
        maxScore=ConstanNum.FIVE*10;
        minScore=-maxScore;
        caChe=new HashMap<>();
    }
    //重新初始化
    public void init(){
        zobirst=new Zobirst();
        count=0;
        board=new int[len][len];
        comScore=new int[len][len];
        humScore=new int[len][len];
        scoreCache=new int[3][4][len][len];
        currentSteps.clear();
        allSteps.clear();
        caChe.clear();
        //如果上局不是电脑先走，这局就是电脑先走
        if(!comFirst){
            put(len/2,len/2,ConstanNum.COM);
        }
        comFirst=!comFirst;
    }
    public boolean backward(){
        if(allSteps.size()<2){
            return false;
        }
        for(int i=0;i<2;i++){
            Point point=allSteps.get(allSteps.size()-1);
            regret(point.x,point.y);
        }
        return true;
    }
    //下子
    public void put(int x,int y,int role){
        board[x][y]=role;
        zobirst.go(x,y,role);
        updateScore(x,y);
        count++;
        currentSteps.add(new Point(x,y,role));
        allSteps.add(new Point(x,y,role));
    }
    //移除棋子
    public void remove(int x,int y){
        int role=board[x][y];
        zobirst.go(x,y,role);
        board[x][y]=ConstanNum.EMPTY;
        updateScore(x,y);
        allSteps.remove(allSteps.size()-1);
        currentSteps.remove(currentSteps.size()-1);
        count--;
    }

    public void regret(int x,int y){
        int role=board[x][y];
        zobirst.go(x,y,role);
        board[x][y]=ConstanNum.EMPTY;
        updateScore(x,y);
        allSteps.remove(allSteps.size()-1);
        if(currentSteps.size()>0)
        currentSteps.remove(currentSteps.size()-1);
        count--;
    }

    //只更新一个点附近的分数
    private void updateScore(int px,int py){
        //4个元素的半径
        int radius=4;
        for(int i=-radius;i<=radius;i++){
            int y=py+i;
            if(y<0)
                continue;
            if(y>=len){
                break;
            }
            update(px,y,0);
        }
        for(int i=-radius;i<=radius;i++){
            int x=px+i;
            if(x<0)
                continue;
            if(x>=len){
                break;
            }
            update(x, py,1);
        }
        for(int i=-radius;i<=radius;i++){
            int x=px+i,y=py+i;
            if(y<0||x<0)
                continue;
            if(y>=len||x>=len){
                break;
            }
            update(x,y,2);
        }
        for(int i=-radius;i<=radius;i++){
            int x=px+i,y=py-i;
            if(y>=len||x<0)
                continue;
            if(y<0||x>=len){
                break;
            }
            update(x,y,3);
        }
    }
    private void update(int x,int y,int dir){
        int role=board[x][y];
        //如果电脑,或者空白
        if(role==ConstanNum.COM||role==ConstanNum.EMPTY){
            //评分函数时重新计算整个方向的，所以不需要累加。
            comScore[x][y]=evaluatePoint(x,y,ConstanNum.COM,dir);
        }
        else{
            comScore[x][y]=0;
        }
        if(role==ConstanNum.HUMEN||role==ConstanNum.EMPTY){
            humScore[x][y]=evaluatePoint(x,y,ConstanNum.HUMEN,dir);
        }
        else{
            humScore[x][y]=0;
        }
    }
    //score为当前局面的评分，step为steps的长度
    class Leaf{
        int score;
        int step;
        ArrayList<Point> steps;
        int abcut;
        Leaf(int score,int step,ArrayList<Point> steps){
            this.score=score;
            this.step=step;
            this.steps=steps;
        }
    }
    class CacheObj{

        int deep;
        Leaf leaf;
        CacheObj(int deep,Leaf leaf){
            this.deep=deep;
            this.leaf=leaf;
        }
    }

    private void cache(int deep, Leaf leaf){
        if(leaf.abcut==1){
            return ;
        }
        caChe.put(zobirst.getCode(),new CacheObj(deep,leaf));
    }
    //极大极小值搜索,递归搜索，返回下一层中局面最合适的点
    private Leaf r(int deep,int alpha,int beta,int role,int step,ArrayList<Point> steps,int spread){
        CacheObj cacheObj =caChe.get(zobirst.getCode());
        if(cacheObj!=null){
            // 如果缓存中的结果搜索深度不比当前小，则结果完全可用
            if(cacheObj.deep>=deep){
                // 记得clone，因为这个分数会在搜索过程中被修改，会使缓存中的值不正确
                return new Leaf(cacheObj.leaf.score,cacheObj.leaf.step+step,cacheObj.leaf.steps);
            }
            // 如果缓存的结果中搜索深度比当前小，那么任何一方出现双三及以上结果的情况下可用
            // TODO: 只有这一个缓存策略是会导致开启缓存后会和以前的结果有一点点区别的，其他几种都是透明的缓存策略
            else{
                if (cacheObj.leaf.score>=ConstanNum.FOUR ||cacheObj.leaf.score <= -ConstanNum.FOUR) {
                    return cacheObj.leaf;
                }
            }
        }
        int myScore=evaluateGame(role);
        Leaf myLeaf=new Leaf(myScore,step,steps);
        count++;
        if(deep<=0||myScore>=ConstanNum.FIVE||myScore<=-ConstanNum.FIVE){
            return myLeaf;
        }
        Leaf best=new Leaf(minScore,step,steps);
        //双方个下两个子之后，开启star spread 模式
        ArrayList<Point> points=gen(role,count>10 ? step>1:step>3,step>1);
        if(points.isEmpty()){
            return myLeaf;
        }
        for(Point p:points){
            put(p.x,p.y,role);
            int tmpdeep=deep-1;
            if(spread<ConstanNum.spreadLimit){
                if((role==ConstanNum.COM&&p.scoreCom>= ConstanNum.FIVE)||(role==ConstanNum.HUMEN&&p.scoreCom>=ConstanNum.FIVE)){
                    tmpdeep+=2;
                    spread++;
                }
            }
            ArrayList<Point> tmpSteps=(ArrayList<Point>) steps.clone();
            tmpSteps.add(p);
            int nextrole;
            if(role==ConstanNum.COM){
                nextrole=ConstanNum.HUMEN;
            }
            else{
                nextrole=ConstanNum.COM;
            }
            Leaf v=r(tmpdeep,-beta,-alpha,nextrole,step+1,tmpSteps,spread);
            v.score*=-1;
            remove(p.x,p.y);

            //开始减枝

            // 注意，这里决定了剪枝时使用的值必须比MAX小
            if(v.score > best.score) {
                best = v;
            }
            alpha = Math.max(best.score, alpha);
            //AB 剪枝
            // 这里不要直接返回原来的值，因为这样上一层会以为就是这个分，实际上这个节点直接剪掉就好了，根本不用考虑，也就是直接给一个很大的值让他被减掉
            // 这样会导致一些差不多的节点都被剪掉，但是没关系，不影响棋力
            // 一定要注意，这里必须是 greatThan 即 明显大于，而不是 greatOrEqualThan 不然会出现很多差不多的有用分支被剪掉，会出现致命错误
            //这里有问题
            if(v.score>beta) {
                v.score = maxScore-1; // 被剪枝的，直接用一个极大值来记录，但是注意必须比MAX小
                v.abcut = 1; // 剪枝标记
                return v;
            }
        }
        cache(deep,best);
        return best;
    }

    /*思路：
     * 每次开始迭代前，先生成一组候选列表，然后在迭代加深的过程中不断更新这个列表中的分数
     * 这样迭代的深度越大，则分数越精确，并且，任何时候达到时间限制而中断迭代的时候，
     * 能保证这个列表中的分数都是可靠的
     */
    //在这里面给condidates数组中的Point属性添加了其他值
    private int minMaxSearch(ArrayList<Point> candidates,int role,int deep,int alpha,int beta){
        count=0;
        currentSteps.clear();
        //对每一个节点进行迭代查询
        for(Point p:candidates){
            put(p.x,p.y,role);
            ArrayList<Point> steps=new ArrayList<>();
            steps.add(p);
            int nextrole;
            if(role==ConstanNum.COM){
                nextrole=ConstanNum.HUMEN;
            }
            else{
                nextrole=ConstanNum.COM;
            }
            //搜索对手的分数，
            Leaf v=r(deep-1,-beta,-alpha,nextrole,1,(ArrayList<Point>) steps.clone(),0);
            v.score*=-1;
            alpha=Math.max(alpha,v.score);
            remove(p.x,p.y);
            p.score=v.score;
            p.step=v.step;
            p.steps=v.steps;
            //超时判定
            if(new Date().getTime()-startDate.getTime()>ConstanNum.timeLimit*1000){
                break;
            }
        }
        return alpha;
    }
    /* 所谓迭代加深，就是从2层开始，逐步增加搜索深度，直到找到胜利走法或者达到深度限制为止。
    比如我们搜索6层深度，那么我们先尝试2层，如果没有找到能赢的走法，再尝试4层，最后尝试6层。
    我们只尝试偶数层。因为奇数层其实是电脑比玩家多走了一步，忽视了玩家的防守，并不会额外找到更好的解法。*/
    public Point findPoint(int role,int deep){
        startDate=new Date();
        //通过gen找到候选点
        ArrayList<Point> candidate=gen(role,false,false);
        // 每次开始迭代的时候清空缓存。这里缓存的主要目的是在每一次的时候加快搜索，而不是长期存储。
        // 事实证明这样的清空方式对搜索速度的影响非常小（小于10%)
        caChe.clear();
        int bestScore;
        //在最大最小值搜索中，会改变candidate中的值的大小。
        for(int i=2;i<=deep;i+=2){
            bestScore=minMaxSearch(candidate,role,i,minScore,maxScore);
            //如果搜索到了最大值，则停止
            if(bestScore>=ConstanNum.FIVE){
                break;
            }
        }
        Collections.sort(candidate);
        return candidate.get(0);
    }


    /*启发函数
     * 变量starBread的用途是用来进行米子计算
     * 所谓米子计算，只是，如果第一步尝试了一个位置A，那么接下来尝试的位置有两种情况：
     * 1: 大于等于活三的位置
     * 2: 在A的米子位置上
     * 注意只有对小于活三的棋才进行starSpread优化
     */
     /*gen 函数的排序是非常重要的，因为好的排序能极大提升AB剪枝的效率。
     * 而对结果的排序，是要根据role来的
     * 按照成五，活四，双三，活三，其他 的顺序来排序
     */

     //在每一步生成所有可以落子的点。
    //并不是所有的空位我们又要搜索，很多位置明显不合适的我们可以直接排除。


    private ArrayList<Point> gen(int role,boolean onlyThrees,boolean starSpread){
        //进攻点
        ArrayList<Point> attackPoints=new ArrayList<>();
        //防守点
        ArrayList<Point> defendPoints=new ArrayList<>();
        ArrayList<Point> fives=new ArrayList<>();
        ArrayList<Point> comfours=new ArrayList<>();
        ArrayList<Point> humfours=new ArrayList<>();
        ArrayList<Point> comblockedfours=new ArrayList<>();
        ArrayList<Point> humblockedfours=new ArrayList<>();
        ArrayList<Point> comtwothrees=new ArrayList<>();
        ArrayList<Point> humtwothrees=new ArrayList<>();
        ArrayList<Point> comthrees=new ArrayList<>();
        ArrayList<Point> humthrees=new ArrayList<>();
        ArrayList<Point> comtwos=new ArrayList<>();
        ArrayList<Point> humtwos=new ArrayList<>();
        ArrayList<Point> neighbors=new ArrayList<>();


        //if(count<=0)
          //  return new Point(7,7,ConstanNum.COM);
        // 默认情况下 我们遍历整个棋盘。但是在开启star模式下，我们遍历的范围就会小很多
        // 只需要遍历以两个点为中心正方形。
        // 注意除非专门处理重叠区域，否则不要把两个正方形分开算，因为一般情况下这两个正方形
        // 会有相当大的重叠面积，别重复计算了

        //开启star模式
        if(starSpread){
            int i=currentSteps.size()-1;
            while (i>=0){
                Point tmp=currentSteps.get(i);
                //如果是人，则找电脑分数大于3的地方，如果是机器，则防守人的分数>3的地方
                if((role==ConstanNum.HUMEN&&tmp.scoreCom>=ConstanNum.THREE)
                        ||(role==ConstanNum.COM&&tmp.scoreHum>=ConstanNum.THREE)){
                    defendPoints.add(tmp);
                    break;
                }
                i-=2;
            }
            i=currentSteps.size()-2;
            while (i>=0){
                Point tmp=currentSteps.get(i);
                //如果是人，则找人分数大于3的地方进攻，如果是机器，则进攻机器的分数>3的地方
                if((role==ConstanNum.COM&&tmp.scoreCom>=ConstanNum.THREE)
                        ||(role==ConstanNum.HUMEN&&tmp.scoreHum>=ConstanNum.THREE)){
                    attackPoints.add(tmp);
                    break;
                }
                i-=2;
            }
            //如果为空，则找到第一个下棋的地方
            if (!attackPoints.isEmpty())
                attackPoints.add(currentSteps.get(0).role
                        == role ? currentSteps.get(0) : currentSteps.get(1));
            if (!defendPoints.isEmpty())
                defendPoints.add(currentSteps.get(0).role
                        != role ? currentSteps.get(0) : currentSteps.get(1));
        }

        //遍历棋盘
        for(int i=0;i<board.length;i++){
            for(int j=0;j<board.length;j++){
                if(board[i][j]==ConstanNum.EMPTY){
                    if(allSteps.size()<6){
                        if(!hasNeighbor(i,j,1,1))
                        continue;
                    }
                    else if(!hasNeighbor(i,j,2,1)){
                        continue;
                    }
                    int scoreHum=humScore[i][j];
                    int scoreCom=comScore[i][j];
                    int maxScore=Math.max(scoreCom,scoreHum);

                    if(onlyThrees&&maxScore<ConstanNum.THREE){
                        continue;
                    }
                    Point tmpP=new Point(i,j,role);
                    tmpP.scoreHum=scoreHum;
                    tmpP.scoreCom=scoreCom;
                    tmpP.score=maxScore;
                    if(scoreCom>=ConstanNum.FIVE){
                        fives.add(tmpP);
                    }
                    else if(scoreHum>=ConstanNum.FIVE){
                        fives.add(tmpP);
                    }
                    else if(scoreCom>=ConstanNum.FOUR){
                        comfours.add(tmpP);
                    }
                    else if(scoreHum>=ConstanNum.FOUR){
                        humfours.add(tmpP);
                    }
                    else if(scoreCom>=ConstanNum.BLOCKED_FOUR){
                        comblockedfours.add(tmpP);
                    }
                    else if(scoreHum>=ConstanNum.BLOCKED_FOUR){
                        humblockedfours.add(tmpP);
                    }
                    else if(scoreCom>=2*ConstanNum.THREE){
                        comtwothrees.add(tmpP);
                    }
                    else if(scoreHum>=2*ConstanNum.THREE){
                        humtwothrees.add(tmpP);
                    }
                    else if(scoreCom>=ConstanNum.THREE){
                        comthrees.add(tmpP);
                    }
                    else if(scoreHum>=ConstanNum.THREE){
                        humthrees.add(tmpP);
                    }
                    else if(scoreCom>=ConstanNum.TWO){
                        comtwos.add(0,tmpP);
                    }
                    else if(scoreHum>=ConstanNum.TWO){
                        humtwos.add(0,tmpP);
                    }
                    else
                        neighbors.add(tmpP);
                }
            }
        }
        //如果成五，是必杀棋，直接返回
        if(!fives.isEmpty()){
            return fives;
        }
        // 自己能活四，则直接活四，不考虑冲四
        if (role == ConstanNum.COM && !comfours.isEmpty()){
            return comfours;
        }
        if (role == ConstanNum.HUMEN && !humfours.isEmpty()){
            return humfours;
        }

        // 对面有活四冲四，自己冲四都没，则只考虑对面活四 （此时对面冲四就不用考虑了)

        if (role ==ConstanNum.COM && !humfours.isEmpty() && comblockedfours.isEmpty()){
            return humfours;
        }
        if (role ==ConstanNum.HUMEN && !comfours.isEmpty() && humblockedfours.isEmpty()){
            return comfours;
        }

        // 对面有活四自己有冲四，则都考虑下
        if(role ==ConstanNum.COM ){
            comfours.addAll(humfours);
            comblockedfours.addAll(humblockedfours);
            if(!comfours.isEmpty()){
                comfours.addAll(comblockedfours);
                return comfours;
            }
        }
        else{
            humfours.addAll(comfours);
            humblockedfours.addAll(comblockedfours);
            if(!humfours.isEmpty()){
                humfours.addAll(humblockedfours);
                return humfours;
            }
        }
        ArrayList<Point> result=new ArrayList<>();
        if (role ==ConstanNum.COM) {
            result.addAll(comtwothrees);
            result.addAll(humtwothrees);
            result.addAll(comblockedfours);
            result.addAll(humblockedfours);
            result.addAll(comthrees);
            result.addAll(humthrees);
        }
        if (role ==ConstanNum.HUMEN) {
            result.addAll(humtwothrees);
            result.addAll(comtwothrees);
            result.addAll(humblockedfours);
            result.addAll(comblockedfours);
            result.addAll(humthrees);
            result.addAll(comthrees);
        }
        //双三很特殊，因为能形成双三的不一定比一个活三强
        if(!comtwothrees.isEmpty() || !humtwothrees.isEmpty()) {
            return result;
        }
        // 只返回大于等于活三的棋
        if (onlyThrees) {
            return result;
        }
        ArrayList<Point> twos =new ArrayList<>();
        if (role ==ConstanNum.COM) {
            twos.addAll(comtwos);
            twos.addAll(humtwos);
        }
        else{
            twos.addAll(humtwos);
            twos.addAll(comtwos);
        }
        //twos.sort(function(a, b) { return b.score - a.score })
        result.addAll(!twos.isEmpty()? twos : neighbors);

        //这种分数低的，就不用全部计算了
        if(result.size()>ConstanNum.countLimit) {
            for(int i=ConstanNum.countLimit;i<result.size();i++){
                result.remove(i);
            }
            return result;
        }
        return result;
    }
    //判断在distance范围内的邻居的数量是否大于count
    private boolean hasNeighbor(int x,int y,int distance,int count){
        int startX=x-distance,startY=y-distance;
        int endX=x+distance,endY=y+distance;
        for(int i=startX;i<=endX;i++){
            if(i<0||i>=len){
                continue;
            }
            for(int j=startY;j<endY;j++){
                if(j<0||j>=len){
                    continue;
                }
                //如果是本身，则不计入
                if(i==x && j==y){
                    continue;
                }
                if(board[i][j]!=ConstanNum.EMPTY){
                    count--;
                    if(count<=0)
                        return true;
                }
            }
        }
        return false;
    }

    //局面评分函数
    private int evaluateGame(int role){
        int comMaxScore = 0;
        int humMaxScore = 0;
        //遍历出最高分
        for(int i=0;i<board.length;i++){
            for(int j=0;j<board[i].length;j++){
                if(board[i][j]==ConstanNum.COM){
                    comMaxScore +=fixScore(comScore[i][j]);
                }
                else if(board[i][j]==ConstanNum.HUMEN){
                    humMaxScore +=fixScore(humScore[i][j]);
                }
            }
        }
        return (role == ConstanNum.COM ? 1:-1)*(comMaxScore - humMaxScore);
    }

    //冲四的分其实肯定比活三高，但是如果这样的话容易形成盲目冲四的问题，所以如果发现电脑有无意义的冲四，则将分数降低到和活三一样
    //而对于冲四活三这种杀棋，则将分数提高。
    private int fixScore(int score){
        //如果是被封锁的四个棋子状态
        if(score<ConstanNum.FOUR&&score>=ConstanNum.BLOCKED_FOUR){
            //单独冲四,意义不大
            if(score < ConstanNum.BLOCKED_FOUR + ConstanNum.THREE){
                return ConstanNum.THREE;
            }
            //冲四或三，比双三分数高，相当于自己形成活四
            else if(score < ConstanNum.BLOCKED_FOUR * 2){
                return ConstanNum.FOUR;
            }
            //双冲四，比活四分数高
            else{
                return ConstanNum.FOUR*2;
            }
        }
        return score;
    }

    //单个位置评分函数
    //根据这个位置是否能成五，活四，活三等来进行打分。
    //为了快速计算，之后更新的时候只用计算dir方向的值，所以增加了dir变量
    private int evaluatePoint(int px, int py, int role, int dir){
        //一个方向上的连续棋子数；另一个方向的；
        // block为遮挡大小，两边没遮挡则为0，全遮挡为2
        //empty为中间空白的位置，-1则没有空白
        int count,block,secondCount,empty;
        //棋盘条数
        int len=ConstanNum.GRID_NUMBER;
        //最终分数
        int result=0;
        //dir为-1时，全方向遍历，其他时候只遍历一个方向
        //先下后上
        if(dir==-1||dir==0){
            count=1;block=0;secondCount=0;empty=-1;
            //向下查找
            for(int i=py+1;true;i++){
                //如果碰到边界则停止
                if(i>=len){
                    block++;
                    break;
                }
                int t= board[px][i];
                //如果当前点为空
                if(t==ConstanNum.EMPTY){
                    //如果是第一次遇到，且之后有当前角色棋子
                    if(empty==-1&&i<len-1&&board[px][i+1]==role){
                        //使用empty记录空白节点的位置
                        empty=count;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                //如果当前位置为当前角色棋子
                if (t==role){
                    count++;
                }
                else{
                    block++;
                    break;
                }
            }
            //向上探索
            for(int i=py-1;true;i--){
                //如果碰到边界
                if(i<0){
                    block++;
                    break;
                }
                int t=board[px][i];
                if(t==ConstanNum.EMPTY){
                    //empty为0，因为之后每增加一个secondcount，empty都+1，用来记录空位位置
                    //且empty必须为-1，因为当前评分规则只记录一个空位，如果不为-1，则说明另一个方向有空位了
                    if(empty==-1&&i>0&&board[px][i-1]==role){
                        empty=0;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                if(t==role){
                    secondCount++;
                    if(empty!=-1){
                        empty++;
                    }
                }
                else{
                    block++;
                    break;
                }
            }
            count+=secondCount;
            //缓存分数，其中0位方向
            scoreCache[role][0][px][py]=countToScore(count,block,empty);
        }
        result+=scoreCache[role][0][px][py];
        //先右后左
        if(dir==-1||dir==1){
            //重置分数
            count=1;block=0;secondCount=0;empty=-1;
            //向右查找
            for(int i=px+1;true;i++){
                //如果碰到边界则停止
                if(i>=len){
                    block++;
                    break;
                }
                int t= board[i][py];
                //如果当前点为空
                if(t==ConstanNum.EMPTY){
                    //如果是第一次遇到，且之后有当前角色棋子
                    if(empty==-1&&i<len-1&&board[i+1][py]==role){
                        //使用empty记录空白节点的位置
                        empty=count;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                //如果当前位置为当前角色棋子
                if (t==role){
                    count++;
                }
                else{
                    block++;
                    break;
                }
            }
            //向zuo探索
            for(int i=px-1;true;i--){
                //如果碰到边界
                if(i<0){
                    block++;
                    break;
                }
                int t=board[i][py];
                if(t==ConstanNum.EMPTY){
                    //empty为0，因为之后每增加一个secondcount，empty都+1，用来记录空位位置
                    //且empty必须为-1，因为当前评分规则只记录一个空位，如果不为-1，则说明另一个方向有空位了
                    if(empty==-1&&i>0&&board[i-1][py]==role){
                        empty=0;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                if(t==role){
                    secondCount++;
                    if(empty!=-1){
                        empty++;
                    }
                }
                else{
                    block++;
                    break;
                }
            }
            count+=secondCount;
            //缓存分数，其中0位方向
            scoreCache[role][1][px][py]=countToScore(count,block,empty);
        }
        result += scoreCache[role][1][px][py];
        //先右xia再左shang
        if(dir==-1||dir==2){
            count=1;block=0;secondCount=0;empty=-1;
            //向右xia角查找
            for(int i=1;true;i++){
                int x=px+i,y=py+i;
                //如果碰到边界则停止
                if(x>=len||y>=len){
                    block++;
                    break;
                }
                int t= board[x][y];
                //如果当前点为空
                if(t==ConstanNum.EMPTY){
                    //如果是第一次遇到，且之后有当前角色棋子
                    if(empty==-1&&x<len-1&&y<len-1&&board[x+1][y+1]==role){
                        //使用empty记录空白节点的位置
                        empty=count;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                //如果当前位置为当前角色棋子
                if (t==role){
                    count++;
                }
                else{
                    block++;
                    break;
                }
            }
            //向左shang探索
            for(int i=1;true;i++){
                int x=px-i,y=py-i;
                //如果碰到边界
                if(x<0||y<0){
                    block++;
                    break;
                }
                int t=board[x][y];
                if(t==ConstanNum.EMPTY){
                    //empty为0，因为之后每增加一个secondcount，empty都+1，用来记录空位位置
                    //且empty必须为-1，因为当前评分规则只记录一个空位，如果不为-1，则说明另一个方向有空位了
                    if(empty==-1&&x>0&&y>0&&board[x-1][y-1]==role){
                        empty=0;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                if(t==role){
                    secondCount++;
                    if(empty!=-1){
                        empty++;
                    }
                }
                else{
                    block++;
                    break;
                }
            }
            count+=secondCount;
            //缓存分数，其中0位方向
            scoreCache[role][2][px][py]=countToScore(count,block,empty);
        }
        result += scoreCache[role][2][px][py];
        //先右shang再左xia
        if(dir==-1||dir==3){
            count=1;block=0;secondCount=0;empty=-1;
            //向you下查找
            for(int i=1;true;i++){
                int x=px+i,y=py-i;
                //如果碰到边界则停止
                if(x>=len||y<0){
                    block++;
                    break;
                }
                int t= board[x][y];
                //如果当前点为空
                if(t==ConstanNum.EMPTY){
                    //如果是第一次遇到，且之后有当前角色棋子
                    if(empty==-1&&x<len-1&&y>0&&board[x+1][y-1]==role){
                        //使用empty记录空白节点的位置
                        empty=count;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                //如果当前位置为当前角色棋子
                if (t==role){
                    count++;
                }
                else{
                    block++;
                    break;
                }
            }
            //向zuoxia探索
            for(int i=1;true;i++){
                int x=px-i,y=py+i;
                //如果碰到边界
                if(x<0||y>=len){
                    block++;
                    break;
                }
                int t=board[x][y];
                if(t==ConstanNum.EMPTY){
                    //empty为0，因为之后每增加一个secondcount，empty都+1，用来记录空位位置
                    //且empty必须为-1，因为当前评分规则只记录一个空位，如果不为-1，则说明另一个方向有空位了
                    if(empty==-1&&y<len-1&&x>0&&board[x-1][y+1]==role){
                        empty=0;
                        continue;
                    }
                    else{
                        break;
                    }
                }
                if(t==role){
                    secondCount++;
                    if(empty!=-1){
                        empty++;
                    }
                }
                else{
                    block++;
                    break;
                }
            }
            count+=secondCount;
            //缓存分数，其中0位方向
            scoreCache[role][3][px][py]=countToScore(count,block,empty);
        }
        result += scoreCache[role][3][px][py];
        return result;
    }
    //根据count，empty，block评分
    private int countToScore(int count,int block,int empty){
        //没有空位
        if(empty<=0){
            //成五
            if (count>=5)
                return ConstanNum.FIVE;
            //两边没有堵塞的情况下
            if(block==0){
                switch (count){
                    case 1:return ConstanNum.ONE;
                    case 2:return ConstanNum.TWO;
                    case 3:return ConstanNum.THREE;
                    case 4:return ConstanNum.FOUR;
                }
            }
            //有一边堵塞的情况下，棋子的分值下降
            if(block==1){
                switch (count){
                    case 1:return ConstanNum.BLOCKED_ONE;
                    case 2: return ConstanNum.BLOCKED_TWO;
                    case 3:return ConstanNum.BLOCKED_THREE;
                    case 4: return ConstanNum.BLOCKED_FOUR;
                }
            }
        }
        //第一个是空位,如上 分别判断情况
        else if (empty==1|| empty == count-1){
            //成5
            if(count>=6){
                return ConstanNum.FIVE;
            }
            //1个棋子和count-1个连续棋子中间隔着一个空位，需要对其降分
            if(block==0){
                switch (count){
                    case 2:return ConstanNum.TWO/2;
                    case 3:return ConstanNum.THREE;
                    case 4:return ConstanNum.BLOCKED_FOUR;
                    case 5:return ConstanNum.FOUR;
                }
            }
            //有一边堵塞且1个棋子和count-1个连续棋子中间隔着一个空位，需要对其降分
            if(block==1){
                switch(count) {
                    case 2: return ConstanNum.BLOCKED_TWO;
                    case 3: return ConstanNum.BLOCKED_THREE;
                    case 4: return ConstanNum.BLOCKED_FOUR;
                    case 5: return ConstanNum.BLOCKED_FOUR;
                }
            }
        }
        //第二个是空位
        else if(empty==2||empty==count-2){
            //成5
            if(count>=7){
                return ConstanNum.FIVE;
            }
            //2个棋子和count-2个连续棋子中间隔着一个空位，需要对其降分
            if(block==0){
                switch (count){
                    case 3: return ConstanNum.THREE;
                    case 4:
                    case 5: return ConstanNum.BLOCKED_FOUR;
                    case 6: return ConstanNum.FOUR;
                }
            }
            //有一边堵塞且1个棋子和count-1个连续棋子中间隔着一个空位，需要对其降分
            if(block==1){
                switch(count) {
                    case 3: return ConstanNum.BLOCKED_THREE;
                    case 4: return ConstanNum.BLOCKED_FOUR;
                    case 5: return ConstanNum.BLOCKED_FOUR;
                    case 6: return ConstanNum.FOUR;
                }
            }
            if(block==2){
                switch(count) {
                    case 4:
                    case 5:
                    case 6: return ConstanNum.BLOCKED_FOUR;
                }
            }
        }
        //第三个是空位
        else if(empty == 3 || empty == count-3) {
            if(count >= 8) {
                return ConstanNum.FIVE;
            }
            if(block == 0) {
                switch(count) {
                    case 4:
                    case 5: return ConstanNum.THREE;
                    case 6: return ConstanNum.BLOCKED_FOUR;
                    case 7: return ConstanNum.FOUR;
                }
            }

            if(block == 1) {
                switch(count) {
                    case 4:
                    case 5:
                    case 6: return ConstanNum.BLOCKED_FOUR;
                    case 7: return ConstanNum.FOUR;
                }
            }

            if(block == 2) {
                switch(count) {
                    case 4:
                    case 5:
                    case 6:
                    case 7: return ConstanNum.BLOCKED_FOUR;
                }
            }
        }
        //第四个是空位
        else if(empty == 4 || empty == count-4) {
            if(count >= 9) {
                return ConstanNum.FIVE;
            }
            if(block == 0) {
                switch(count) {
                    case 5:
                    case 6:
                    case 7:
                    case 8: return ConstanNum.FOUR;
                }
            }

            if(block == 1) {
                switch(count) {
                    case 4:
                    case 5:
                    case 6:
                    case 7: return ConstanNum.BLOCKED_FOUR;
                    case 8: return ConstanNum.FOUR;
                }
            }

            if(block == 2) {
                switch(count) {
                    case 5:
                    case 6:
                    case 7:
                    case 8: return ConstanNum.BLOCKED_FOUR;
                }
            }
        }
        //第五个是空位
        else if(empty == 5 || empty == count-5) {
            return ConstanNum.FIVE;
        }

        //无法成5的棋型则返回0
        return 0;
    }




}
