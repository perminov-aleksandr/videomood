package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class HorseshoeView extends SurfaceView {

    public HorseshoeView(Context context) {
        super(context);
        init();
    }

    public HorseshoeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public HorseshoeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private SurfaceHolder surfaceHolder;

    private void init() {
        background = BitmapFactory.decodeResource(getResources(), R.drawable.horseshoe);
        circles = new Boolean[]{
            true,true,true,true,true
        };
        surfaceHolder = getHolder();
        if (!this.isInEditMode()) {
            setZOrderOnTop(true);
            surfaceHolder.setFormat(PixelFormat.TRANSPARENT);
            surfaceHolder.addCallback(new SurfaceHolder.Callback(){

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    /*Canvas canvas = holder.lockCanvas();
                    if (canvas != null) {
                        drawHorseshoe(canvas);
                        holder.unlockCanvasAndPost(canvas);
                    }
                    setWillNotDraw(false);*/
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder,
                                           int format, int width, int height) {
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                }});
        }
    }

//    @Override
//    public void draw(Canvas canvas) {
//        super.draw(canvas);
//        if (!isInEditMode()) {
//            Canvas c = surfaceHolder.lockCanvas();
//            drawHorseshoe(c);
//            surfaceHolder.unlockCanvasAndPost(c);
//        }
//    }

    private Boolean[] circles;

    private Bitmap background;

    public void drawHorseshoe(Canvas canvas) {
        canvas.drawBitmap(background, 0, 0, null);

        Paint p = new Paint();
        p.setAntiAlias(true);
        p.setColor(getResources().getColor(R.color.colorAccent));

        int radius = 17;
        int[] coordinates = {31, 104, 28, 54, 75, 22, 121, 54, 119, 104};
        for (int i = 0; i < 5; i++) {
            if (circles[i]) {
                canvas.drawCircle(coordinates[i*2], coordinates[i*2 + 1], radius, p);
            }
        }
    }

    public void setCircles(Boolean[] circles) {
        if (circles.length == 4)
            this.circles = new Boolean[]{
                circles[0], circles[1], true, circles[2], circles[3]
            };
        else
            this.circles = circles;
        invalidate();
    }
}
