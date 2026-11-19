package de.naehe.gartenkueche;

import java.util.ArrayList;
/** Minimal in-memory fixture for compiling real CareEngine without Android dependencies. */
public final class PlantStore {
    public static final String[] DIFFICULTIES={"Pflegeleicht","Medium","Profi","Diva"};
    public static final class Plant {
        public String id="test-plant-001";
        public int stage=1;
        public final ArrayList<Object> stages=new ArrayList<>();
        public final ArrayList<Integer> difficulties=new ArrayList<>();
        public boolean dead;
        public int wateringCount, strikesAtStageOne, windowWaterings, minWater, maxWater, windowDays, evolutionDays;
        public long createdAtUtc, lastWaterAtUtc, stageEnteredAtUtc,
             windowStartUtc, windowEndUtc, lastEvaluatedAtUtc, windowOrdinal;
        public final ArrayList<Long> waterEventsUtc=new ArrayList<>();
    }
}
