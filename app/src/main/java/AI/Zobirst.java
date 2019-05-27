package AI;

public class Zobirst {
    private int []com;
    private int []hum;
    private int len=ConstanNum.GRID_NUMBER;
    private int code;
    private int rand(){
        return (int)(Math.random()*1000000000);
    }
    public Zobirst(){
        int len2=len*len;
        com=new int[len2];
        hum=new int[len2];
        for(int i=0;i<len2;i++){
            com[i]=rand();
            hum[i]=rand();
        }
        code=rand();
    }
    public int getCode(){
        return code;
    }
    public int go(int x,int y,int role){
        int index=len*x+y;
        code^=(role==ConstanNum.COM ? com[index]:hum[index]);
        return code;
    }
}
