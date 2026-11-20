package de.naehe.gartenkueche;

/** Pure Java virtual-time calculations. Neither system time nor care logic is modified. */
public final class VirtualTimeMath {
    public static final long DAY_MS=86_400_000L;
    private VirtualTimeMath(){}
    public static long project(long simulatedAnchorUtc,long elapsedRealMillis,int factor){
        if(factor<1||factor>20)throw new IllegalArgumentException("Speed must be 1-20");
        return simulatedAnchorUtc+Math.max(0,elapsedRealMillis)*factor;
    }
    public static int plantDay(long simulatedNow,long createdSimUtc){
        long elapsed=Math.max(0,simulatedNow-createdSimUtc);
        return 1+(int)Math.min(Integer.MAX_VALUE-1,elapsed/DAY_MS);
    }
    public static long realMillisForOneDay(int speed){
        if(speed<1||speed>20)throw new IllegalArgumentException("Speed");
        return DAY_MS/speed;
    }
}
