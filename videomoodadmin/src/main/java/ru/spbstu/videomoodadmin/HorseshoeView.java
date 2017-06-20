package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

public class HorseshoeView extends View {

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

    private void init() {
        background = BitmapFactory.decodeResource(getResources(), R.drawable.horseshoe);
        circles = new Boolean[]{
            false,true,false,true,false
        };
        accentColor = getResources().getColor(R.color.colorAccent);
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawHorseshoe(canvas);
    }

    private Boolean[] circles;

    private Bitmap background;

    private Paint circlePaint = new Paint();
    private int accentColor;

    public void drawHorseshoe(Canvas canvas) {
        canvas.drawBitmap(background, 0, 0, null);

        circlePaint.setAntiAlias(true);

        int radius = 17;
        int[] coordinates = {31, 104, 28, 54, 75, 22, 121, 54, 119, 104};
        for (int i = 0; i < 5; i++) {
            circlePaint.setColor(circles[i] ? accentColor : Color.WHITE);
            canvas.drawCircle(coordinates[i*2], coordinates[i*2 + 1], radius, circlePaint);
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
