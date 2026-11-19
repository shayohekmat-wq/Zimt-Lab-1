package de.naehe.gartenkueche;

import java.util.Random;

/**
 * Pure-Java rules (no Android dependencies); shareable with future ZIMT module.
 * The following interpretations remain provisional pending gameplay review.
 */
public final class CareRules {
    public static final long DAY_MS = 86_400_000L;
    public static final int EASY=0, MEDIUM=1, PRO=2, DIVA=3;
    private CareRules() {}

    public static final class Draw {
        public final int minWater, maxWater, windowDays, evolutionDays;
        Draw(int min, int max, int window, int evolution) {
            minWater=min; maxWater=max; windowDays=window; evolutionDays=evolution;
        }
    }
    public static Draw sample(String plantId, int stageNumber, int difficulty, long windowNumber) {
        if(difficulty<0 || difficulty>3 || stageNumber<1 || windowNumber<0)
            throw new IllegalArgumentException("Invalid care stage");
        long seed=0x9e3779b97f4a7c15L;
        for (int i=0; i<plantId.length();i++) seed=(seed^plantId.charAt(i))*0x100000001b3L;
        seed ^= ((long)stageNumber * 0x9e3779b97f4a7c15L);
        seed ^= (windowNumber * 0xbf58476d1ce4e5b9L);
        Random rnd=new Random(seed);
        switch (difficulty) {
            case EASY:
                return new Draw(1, uniform(rnd,9,11),uniform(rnd,12,16),uniform(rnd,3,5));
            case MEDIUM:
                return new Draw(1, uniform(rnd,5,9),uniform(rnd,7,11),uniform(rnd,4,8));
            case PRO:
                return new Draw(uniform(rnd,1,3),5,uniform(rnd,5,7),uniform(rnd,5,11));
            case DIVA:
                // 4±5 includes -1 and 0. Rejection sampling gives uniform 1..9.
                int days;
                do { days=uniform(rnd,-1,9); } while(days<1);
                return new Draw(1,5,days,uniform(rnd,4,18));
            default: throw new IllegalArgumentException("Unknown difficulty");
        }
    }
    private static int uniform(Random r,int min,int max) {
        return min+r.nextInt(max-min+1);
    }
    public static boolean dailyLimit(int difficulty) { return difficulty==EASY || difficulty==MEDIUM; }
}
