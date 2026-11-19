package de.naehe.gartenkueche;

public final class CareEngineTest {
    private static void ok(boolean condition,String message) {
        if(!condition)throw new AssertionError(message);
    }
    private static PlantStore.Plant plant(int... difficulties) {
        PlantStore.Plant p=new PlantStore.Plant();
        for(int d:difficulties) {p.stages.add(new Object());p.difficulties.add(d);}
        return p;
    }
    public static void main(String[] argv) {
        final long DAY=CareRules.DAY_MS;
        int[] seenDiva = new int[10];
        for (int d=0;d<4;d++) for (int k=0;k<1500;k++) {
            String id="plant"+k;
            CareRules.Draw a=CareRules.sample(id,1,d,k);
            CareRules.Draw b=CareRules.sample(id,1,d,k);
            ok(a.windowDays==b.windowDays && a.minWater==b.minWater &&
               a.maxWater==b.maxWater && a.evolutionDays==b.evolutionDays,
               "roll must stay identical for same plant+stage+window");
            if(d==0) ok(a.windowDays>=12 && a.windowDays<=16 &&
                  a.maxWater>=9 && a.maxWater<=11 && a.minWater==1 &&
                  a.evolutionDays>=3 && a.evolutionDays<=5,"easy range");
            if(d==1) ok(a.windowDays>=7 && a.windowDays<=11 &&
                  a.maxWater>=5 && a.maxWater<=9 && a.minWater==1 &&
                  a.evolutionDays>=4 && a.evolutionDays<=8,"medium range");
            if(d==2) ok(a.windowDays>=5 && a.windowDays<=7 &&
                  a.maxWater==5 && a.minWater>=1 && a.minWater<=3 &&
                  a.evolutionDays>=5 && a.evolutionDays<=11,"pro range");
            if(d==3) {
                ok(a.windowDays>=1 && a.windowDays<=9 &&
                  a.maxWater==5 && a.minWater==1 &&
                  a.evolutionDays>=4 && a.evolutionDays<=18,"diva range");
                seenDiva[a.windowDays]++;
            }
        }
        for (int i=1;i<=9;i++) ok(seenDiva[i]>0,"diva draw never produced "+i);

        PlantStore.Plant p=plant(0,1);
        long time=1_800_000_000_000L;
        CareEngine.evaluate(p,time);
        int evolutionDays=p.evolutionDays;
        ok(p.stage==1 && p.windowEndUtc>time,"new stage initialized");
        CareEngine.water(p,time+DAY);
        ok(p.wateringCount==1 && p.stage==1,"watering counts once before due");
        CareEngine.evaluate(p,time + evolutionDays*DAY);
        ok(p.stage==2,"stage advances after required watering and time");
        ok(p.windowWaterings==0,"new stage has fresh care window");
        long deadline=p.windowEndUtc;
        CareEngine.evaluate(p,deadline);
        ok(p.stage==1,"missed care deadline regresses a stage");
        long first=p.windowEndUtc;
        CareEngine.evaluate(p,first);
        ok(p.stage==1 && p.strikesAtStageOne==1,"first missed rule on stage one");
        CareEngine.evaluate(p,first);
        ok(p.strikesAtStageOne==1,"idempotent deadline evaluation");
        CareEngine.evaluate(p,p.windowEndUtc);
        ok(p.dead && p.stage==1,"second violation on stage one kills");
        int oldCount=p.wateringCount;
        CareEngine.water(p,p.windowEndUtc + DAY);
        ok(p.wateringCount==oldCount,"dead plant cannot be watered");

        PlantStore.Plant over=plant(0);
        CareEngine.evaluate(over,time);
        int allowed=over.maxWater;
        for(int i=0;i<allowed;i++) CareEngine.water(over,time+1000L+i);
        ok(!over.dead && over.strikesAtStageOne==0,"allowed daily water must not hurt");
        CareEngine.water(over,time+5000L+allowed);
        ok(over.strikesAtStageOne==1,"first overwater penalty");
        for(int i=0;i<=allowed;i++) CareEngine.water(over,time+6000L+allowed+i);
        ok(over.dead,"second overwatering penalty on stage one after exceeding new phase limit");

        PlantStore.Plant solo=plant(0);
        CareEngine.evaluate(solo,time);
        CareEngine.water(solo,time+DAY);
        CareEngine.evaluate(solo,time + solo.evolutionDays*DAY);
        ok(solo.stage==1,"single-stage plant never evolves");
        System.out.println("CareEngineTest: all lifecycle and RNG assertions passed.");
    }
}
