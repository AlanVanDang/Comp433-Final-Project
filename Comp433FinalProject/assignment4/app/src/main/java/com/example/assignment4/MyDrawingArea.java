package com.example.assignment4;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import androidx.annotation.Nullable;

import java.io.File;
import java.io.FileOutputStream;

public class MyDrawingArea extends View {
    Path path = new Path();
    Paint paint = new Paint();
    private int currentColor = Color.BLACK;
    private float currentStrokeSize = 5f;
    public MyDrawingArea(Context context) {
        super(context);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public MyDrawingArea(Context context, @Nullable AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);

    }

    private void init() {
        paint.setColor(currentColor);
        paint.setStyle(Paint.Style.STROKE);
        paint.setAntiAlias(true);
        paint.setStrokeWidth(currentStrokeSize);
    }
    public void setPaintColor(int color) {
        currentColor = color;
        paint.setColor(currentColor);
    }
    public void setPaintStrokeSize(float strokeSize) {
        currentStrokeSize = strokeSize;
        paint.setStrokeWidth(currentStrokeSize);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        init();
//        Paint p = new Paint();
//        p.setColor(Color.BLACK);
//        p.setStyle(Paint.Style.STROKE);
//        p.setStrokeWidth(5f);
        /*
        Note: Declare path somewhere outside onDraw
        Path path = new Path();
        */
        canvas.drawPath(path, paint);
        invalidate();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        float x = event.getX(), y = event.getY();
        int action = event.getAction();
        if(action == MotionEvent.ACTION_DOWN){
            path.moveTo(x, y); //path is global. Same thing that onDraw uses.
        }
        else if(action == MotionEvent.ACTION_MOVE){
            path.lineTo(x, y);
        }
        return true;
    }

    /*This bmp is declared outside globally in the custom view class*/
    Bitmap bmp;
    public Bitmap getBitmap()
    {
        bmp = Bitmap.createBitmap(getWidth(), getHeight(), Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);
        c.drawPath(path, paint); //path is global. The very same thing that onDraw uses
        return bmp;
    }

    public void clearPath(){
        path.reset();
    }


    public void reset() {
        path.reset();
    }
}
