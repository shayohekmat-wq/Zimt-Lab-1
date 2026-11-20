package de.naehe.gartenkueche;

public final class VirtualTimeMathTest {
    static void check(boolean ok,String message){if(!ok)throw new AssertionError(message);}
    public static void main(String[] args){
        long anchor=1_850_000_000_000L;
        check(VirtualTimeMath.project(anchor,1000L,1)==anchor+1000L,"1x realtime");
        check(VirtualTimeMath.project(anchor,1000L,20)==anchor+20_000L,"20x scaling");
        check(VirtualTimeMath.realMillisForOneDay(20)==4_320_000L,"one 20x day=72 minutes");
        check(VirtualTimeMath.realMillisForOneDay(10)==8_640_000L,"one 10x day=144 minutes");
        check(VirtualTimeMath.plantDay(anchor,anchor)==1,"new plant day 1");
        check(VirtualTimeMath.plantDay(anchor+VirtualTimeMath.DAY_MS-1,anchor)==1,
            "day 1 until 24 simulated hours passed");
        check(VirtualTimeMath.plantDay(anchor+VirtualTimeMath.DAY_MS,anchor)==2,
            "day 2 after exactly 24 simulated hours");
        check(VirtualTimeMath.plantDay(anchor+20*VirtualTimeMath.DAY_MS,anchor)==21,
            "age depends on virtual days");
        long change=VirtualTimeMath.project(anchor,5000L,20);
        check(VirtualTimeMath.project(change,0L,1)==change,"speed change has no time jump");
        check(VirtualTimeMath.project(change,3000L,1)==anchor+103_000L,
            "time continues at new speed, not retroactively scaled");
        check(VirtualTimeMath.project(anchor,-10L,20)==anchor,"negative elapsed rejected");
        System.out.println("VirtualTimeMathTest: 1x..20x, day boundaries and speed transitions passed");
    }
}
