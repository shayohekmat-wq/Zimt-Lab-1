package de.naehe.gartenkueche;

/**
 * One editable plant across all evolution stages. No stage is deleted when the
 * user navigates backwards. Separate accepted topology snapshots make contour
 * edits reversible without destroying pigment.
 */
public final class AtelierDocument {
    public final int count;
    public final PlantArt[] stages;
    public final PlantArt[] accepted;
    public final boolean[] completed;
    public final int[] difficulty;
    public PlantArt death;
    public PlantArt acceptedDeath;
    public boolean deathMode, deathCompleted, colorMode;
    // active==count means the final death/finish step
    public int active, palettePage, selectedColor=PlantArt.PURPLE, tool=0;
    public int zoom=1, panX, panY;
    public long revision;

    public AtelierDocument(int count) {
        if(count<1 || count>10)throw new IllegalArgumentException("1..10 stages required");
        this.count=count;
        stages=new PlantArt[count];
        accepted=new PlantArt[count];
        completed=new boolean[count];
        difficulty=new int[count];
        for(int i=0;i<count;i++)stages[i]=new PlantArt();
        death=new PlantArt();
    }
    public boolean deathChoice(){return active==count&&!deathMode;}
    public PlantArt current(){return deathMode?death:stages[Math.min(active,count-1)];}
    public void replaceCurrent(PlantArt art) {
        if(deathMode)death=art;
        else stages[active]=art;
        revision++;
    }
    public PlantArt acceptedCurrent(){
        return deathMode?acceptedDeath:accepted[active];
    }
    public void acceptCurrent(PlantArt art) {
        replaceCurrent(art);
        if(deathMode)acceptedDeath=art.copy();
        else accepted[active]=art.copy();
    }
    public void restoreAcceptedOutline(){
        PlantArt baseline=acceptedCurrent();
        if(baseline==null)return;
        PlantArt art=current();
        System.arraycopy(baseline.outline,0,art.outline,0,PlantArt.TOTAL);
        System.arraycopy(baseline.outlineColors,0,art.outlineColors,0,PlantArt.TOTAL);
        art.inside=baseline.inside==null?null:baseline.inside.clone();
        art.detectedRegions=baseline.detectedRegions;
        art.points=baseline.points;
        art.startX=baseline.startX;art.startY=baseline.startY;
        art.endX=baseline.endX;art.endY=baseline.endY;
        revision++;
    }
    public void beginContourEdit(){
        if(acceptedCurrent()==null && current().hasInterior()){
            if(deathMode)acceptedDeath=current().copy();
            else accepted[active]=current().copy();
        }
        colorMode=false;
        revision++;
    }
    public int threatenedColors(PlantArt candidate){
        PlantArt old=acceptedCurrent();
        if(old==null || old.inside==null)return 0;
        boolean[] next=candidate.inside;
        if(next==null)return 0;
        PlantArt now=current();
        int lost=0;
        for(int i=0;i<PlantArt.TOTAL;i++){
            if(now.pigments[i]!=0 && old.inside[i] &&
                (!next[i] || candidate.outline[i]))lost++;
        }
        return lost;
    }
    public AtelierDocument copy(){
        AtelierDocument d=new AtelierDocument(count);
        for(int i=0;i<count;i++){
            d.stages[i]=stages[i].copy();
            d.accepted[i]=accepted[i]==null?null:accepted[i].copy();
            d.completed[i]=completed[i];d.difficulty[i]=difficulty[i];
        }
        d.death=death.copy();
        d.acceptedDeath=acceptedDeath==null?null:acceptedDeath.copy();
        d.deathMode=deathMode;d.deathCompleted=deathCompleted;d.colorMode=colorMode;
        d.active=active;d.palettePage=palettePage;d.selectedColor=selectedColor;
        d.tool=tool;d.zoom=zoom;d.panX=panX;d.panY=panY;d.revision=revision;
        return d;
    }
}
