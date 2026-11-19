package de.naehe.gartenkueche;

import java.util.ArrayList;

/**
 * Offline plant lifecycle; all state and sampled care limits are serializable.
 * Proposed gameplay interpretation: each ACTIVE stage has its own care requirements.
 * No background service is required: evaluate missed windows on app resume or watering.
 */
public final class CareEngine {
    private CareEngine() {}
    public static final class Result {
        public boolean changed;
        public final ArrayList<String> events = new ArrayList<>();
        public String message() { return events.isEmpty() ? "" : events.get(events.size()-1); }
    }
    private static int difficulty(PlantStore.Plant p) {
        return p.difficulties.get(p.stage-1);
    }
    private static long plusDays(long timestamp, int days) {
        return timestamp + days * CareRules.DAY_MS;
    }
    private static void newWindow(PlantStore.Plant p, long start, boolean enteredStage) {
        p.windowOrdinal++;
        CareRules.Draw draw = CareRules.sample(p.id, p.stage, difficulty(p), p.windowOrdinal);
        p.minWater = draw.minWater;
        p.maxWater = draw.maxWater;
        p.windowDays = draw.windowDays;
        if(enteredStage) p.evolutionDays = draw.evolutionDays;
        p.windowStartUtc = start;
        p.windowEndUtc = plusDays(start, p.windowDays);
        p.windowWaterings = 0;
    }
    private static void enterStage(PlantStore.Plant p, long at) {
        p.stageEnteredAtUtc = at;
        newWindow(p, at, true);
    }
    private static long normalizedNow(PlantStore.Plant p, long requested) {
        // Offline clocks are not tamperproof. We freeze on backwards jumps.
        return Math.max(p.lastEvaluatedAtUtc, requested);
    }
    public static Result evaluate(PlantStore.Plant p, long requestedTimeUtc) {
        Result result = new Result();
        if(p.dead) return result;
        long now = normalizedNow(p, requestedTimeUtc);
        if(p.windowEndUtc <= 0) {
            enterStage(p, now); // On migration, do not retroactively punish older plants.
            result.changed = true;
            result.events.add("Pflegephase gestartet.");
        }
        // Each old care window is processed exactly once, even after weeks offline.
        // Cap avoids malformed timestamps freezing the UI; remaining backlog is caught up on resume.
        int checked = 0;
        while (!p.dead && p.windowEndUtc <= now && checked++ < 2048) {
            long deadline = p.windowEndUtc;
            if(p.windowWaterings < p.minWater) {
                violation(p, deadline, result, "Nicht ausreichend gegossen.");
            } else {
                newWindow(p, deadline, false);
                result.changed = true;
            }
        }
        if(!p.dead) maybeEvolve(p, now, result);
        if(p.lastEvaluatedAtUtc != now) {
            p.lastEvaluatedAtUtc = now;
            result.changed = true;
        }
        return result;
    }
    private static void violation(PlantStore.Plant p, long when, Result r, String reason) {
        r.changed = true;
        if (p.stage > 1) {
            p.stage--;
            p.strikesAtStageOne = 0;
            enterStage(p, when);
            r.events.add(reason + " Rückentwicklung auf Stufe " + p.stage + ".");
        } else {
            p.strikesAtStageOne++;
            if (p.strikesAtStageOne >= 2) {
                p.dead = true;
                r.events.add(reason + " Die Pflanze ist endgültig gestorben.");
            } else {
                enterStage(p, when);
                r.events.add(reason + " Erste Verwarnung auf Stufe 1.");
            }
        }
    }
    private static void maybeEvolve(PlantStore.Plant p, long when, Result r) {
        if(p.stage >= p.stages.size()) return;
        long due = plusDays(p.stageEnteredAtUtc, p.evolutionDays);
        if (when >= due && p.windowWaterings >= p.minWater) {
            p.stage++;
            p.strikesAtStageOne=0;
            enterStage(p, when);
            r.changed = true;
            r.events.add("Die Pflanze ist auf Evolutionsstufe " + p.stage + " gewachsen!");
        }
    }
    private static int recentWaterings(PlantStore.Plant p, long at) {
        long cutoff = Math.max(p.stageEnteredAtUtc, at - CareRules.DAY_MS);
        int recent=0;
        for(int i=p.waterEventsUtc.size()-1;i>=0;i--) {
            long t=p.waterEventsUtc.get(i);
            if(t < cutoff) break;
            if(t <= at) recent++;
        }
        return recent;
    }
    /** Call only from one completed five-second watering gesture (once). */
    public static Result water(PlantStore.Plant p, long requestedTimeUtc) {
        Result result = evaluate(p, requestedTimeUtc);
        if(p.dead) {
            result.events.add("Tote Pflanzen können nicht mehr gegossen werden.");
            return result;
        }
        long now = normalizedNow(p, requestedTimeUtc);
        p.wateringCount++;
        p.lastWaterAtUtc = now;
        p.waterEventsUtc.add(now);
        p.windowWaterings++;
        result.changed = true;
        boolean over = CareRules.dailyLimit(difficulty(p))
                ? recentWaterings(p, now) > p.maxWater
                : p.windowWaterings > p.maxWater;
        if (over) {
            violation(p, now, result, "Zu häufig gegossen.");
        } else {
            result.events.add("Ein Gießvorgang registriert.");
            maybeEvolve(p, now, result);
        }
        p.lastEvaluatedAtUtc = now;
        return result;
    }
    public static String rulesFor(PlantStore.Plant p, long now) {
        if(p.dead) return "Tot · Todesform · keine weitere Pflege möglich";
        if(p.windowEndUtc<=0) return "Stufe " + p.stage + " · Pflegezeitfenster wird gestartet";
        String cat=PlantStore.DIFFICULTIES[difficulty(p)];
        long remaining=Math.max(0, p.windowEndUtc-Math.max(now,p.lastEvaluatedAtUtc));
        long hours=(remaining + 3_599_999L)/3_600_000L;
        String upper=CareRules.dailyLimit(difficulty(p)) ? "/24 Std." : "/Zeitfenster";
        return "Stufe " + p.stage + "/" + p.stages.size() + " · " + cat
            + "\nIn diesem " + p.windowDays + "-Tage-Fenster: " + p.windowWaterings
            + " / mindestens " + p.minWater + " Mal"
            + "\nMaximal " + p.maxWater + " Gießvorgänge " + upper
            + " · Frist in ca. " + hours + " Std."
            + (p.stage < p.stages.size()
                ? "\nEvolution frühestens nach " + p.evolutionDays + " Tagen ab Stufeneintritt"
                : "\nHöchste Stufe erreicht; weiter pflegen.");
    }
}
