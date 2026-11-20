package de.naehe.gartenkueche;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;

/**
 * Local-only virtual time for Beet testing. Not transferable as real partner time.
 * 1x follows real wall time while closed. 2..20x pauses when Beet is not active.
 * Gesture/hold durations MUST continue to use SystemClock.elapsedRealtime().
 */
public final class TestClock {
    private static final String PREF="beet_test_clock_v1";
    private final SharedPreferences prefs;
    private long anchorSimUtc, anchorRealElapsed, anchorWallUtc;
    private int speed;
    private boolean running;
    public TestClock(Context context){
        prefs=context.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);
        long real=System.currentTimeMillis();
        anchorSimUtc=prefs.getLong("simUtc",real);
        anchorWallUtc=prefs.getLong("wallUtc",real);
        speed=limit(prefs.getInt("speed",1));
        anchorRealElapsed=SystemClock.elapsedRealtime();
    }
    private static int limit(int value){return Math.max(1,Math.min(20,value));}
    public int speed(){return speed;}
    public long now(){
        if(running){
            long elapsed=Math.max(0,SystemClock.elapsedRealtime()-anchorRealElapsed);
            return VirtualTimeMath.project(anchorSimUtc,elapsed,speed);
        }
        // In ordinary 1x mode, missed real-world time continues to count.
        return speed==1
            ?anchorSimUtc+Math.max(0,System.currentTimeMillis()-anchorWallUtc)
            :anchorSimUtc;
    }
    public void resume(){
        if(running)return;
        anchorSimUtc=now(); // account for real time spent away at 1x only
        anchorWallUtc=System.currentTimeMillis();
        anchorRealElapsed=SystemClock.elapsedRealtime();
        running=true;
        persist();
    }
    public void pause(){
        if(!running)return;
        anchorSimUtc=now();
        anchorRealElapsed=SystemClock.elapsedRealtime();
        anchorWallUtc=System.currentTimeMillis();
        running=false;
        persist();
    }
    /** Change speed without changing the simulated timestamp at the switch. */
    public void setSpeed(int requested){
        anchorSimUtc=now();
        anchorRealElapsed=SystemClock.elapsedRealtime();
        anchorWallUtc=System.currentTimeMillis();
        speed=limit(requested);
        persist();
    }
    public int plantDay(long creationSimUtc){
        return VirtualTimeMath.plantDay(now(),creationSimUtc);
    }
    private void persist(){
        prefs.edit()
            .putLong("simUtc",anchorSimUtc)
            .putLong("wallUtc",anchorWallUtc)
            .putInt("speed",speed)
            .apply();
    }
}
