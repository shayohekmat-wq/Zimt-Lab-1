package de.naehe.gartenkueche;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/** Self-contained HSV hue/saturation wheel; value is adjusted with the dialog slider. */
final class HueWheelView extends View {
    interface Listener {void picked(int color);}
    private Listener listener;
    private final Paint paint=new Paint(3);
    private final Rect bitmapRect=new Rect();
    private final RectF dest=new RectF();
    private Bitmap wheel;
    private float hue=45,saturation=1,value=1;
    HueWheelView(Context c){super(c);setContentDescription("Farbkreis: Farbton und Sättigung durch Berühren wählen");}
    void setListener(Listener l){listener=l;}
    void setBrightness(float v){value=Math.max(.05f,Math.min(1,v));invalidate();if(listener!=null)listener.picked(color());}
    int color(){return Color.HSVToColor(new float[]{hue,saturation,value});}
    void setColor(int argb){
        float[] hsv=new float[3];Color.colorToHSV(argb,hsv);
        hue=hsv[0];saturation=hsv[1];value=hsv[2];
        invalidate();
    }
    private void makeWheel(){
        if(wheel!=null)return;
        final int size=256;
        int[] pixels=new int[size*size];
        float cx=(size-1)/2f,cy=cx;
        float[] hsv={0,0,1};
        for(int y=0;y<size;y++)for(int x=0;x<size;x++){
            float dx=x-cx,dy=y-cy,r=(float)Math.hypot(dx,dy)/cx;
            if(r>1){pixels[y*size+x]=0;continue;}
            hsv[0]=((float)Math.toDegrees(Math.atan2(dy,dx))+360)%360;
            hsv[1]=Math.min(1,r);
            pixels[y*size+x]=Color.HSVToColor(hsv);
        }
        wheel=Bitmap.createBitmap(size,size,Bitmap.Config.ARGB_8888);
        wheel.setPixels(pixels,0,size,0,0,size,size);
    }
    @Override protected void onMeasure(int w,int h){
        int width=View.MeasureSpec.getSize(w);
        int side=Math.min(width,Ui.dp(getContext(),256));
        setMeasuredDimension(side,side);
    }
    @Override protected void onDraw(Canvas c){
        super.onDraw(c);makeWheel();
        float side=Math.min(getWidth(),getHeight());
        bitmapRect.set(0,0,wheel.getWidth(),wheel.getHeight());
        dest.set(0,0,side,side);
        paint.setFilterBitmap(true);paint.setAlpha(255);
        c.drawBitmap(wheel,bitmapRect,dest,paint);
        float rad=(float)Math.toRadians(hue);
        float r=(side/2-1)*saturation;
        float mx=side/2+(float)Math.cos(rad)*r;
        float my=side/2+(float)Math.sin(rad)*r;
        paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(Ui.dp(getContext(),2));
        paint.setColor(Color.BLACK);c.drawCircle(mx,my,Ui.dp(getContext(),7),paint);
        paint.setColor(Color.WHITE);c.drawCircle(mx,my,Ui.dp(getContext(),5),paint);
        paint.setStyle(Paint.Style.FILL);
    }
    @Override public boolean onTouchEvent(MotionEvent ev){
        int action=ev.getActionMasked();
        if(action==MotionEvent.ACTION_DOWN||action==MotionEvent.ACTION_MOVE){
            float cx=getWidth()/2f,cy=getHeight()/2f;
            float dx=ev.getX()-cx,dy=ev.getY()-cy;
            hue=((float)Math.toDegrees(Math.atan2(dy,dx))+360)%360;
            saturation=Math.min(1f,(float)Math.hypot(dx,dy)/(Math.min(cx,cy)-1));
            invalidate();if(listener!=null)listener.picked(color());return true;
        }
        return true;
    }
    @Override protected void onDetachedFromWindow(){
        if(wheel!=null){wheel.recycle();wheel=null;}
        super.onDetachedFromWindow();
    }
}
