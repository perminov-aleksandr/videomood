package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class Timeline extends View
{
    private ScaleGestureDetector scaleGestureDetector;
    private GestureDetector gestureDetector;

    private int leftBorder = 0;
    private int rightBorder = 0;
    private final int rectPadding = 4;

    private Paint rectPaint = new Paint();
    private Paint textPaint = new Paint();
    private Paint durationPaint = new Paint();

    private int height;
    private int width;

    private final int textSize = 25;
    private final int durationSize = 15;

    private float scaleFactor = 1;

    public Timeline(Context context)
    {
        super(context);
        init(context, null);
    }

    public Timeline(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs);
    }

    private void init(Context context, AttributeSet attrs) {
        gestureDetector = new GestureDetector(context, new MyGestureListener());
        scaleGestureDetector = new ScaleGestureDetector(context, new MyScaleGestureListener());

        timelineEvents = new ArrayList<>();
        timelineEvents.add(new TimeLineEvent("First Event", 300));
        timelineEvents.add(new TimeLineEvent("Second Event", 660));
        timelineEvents.add(new TimeLineEvent("Third Event", 710));
        timelineEvents.add(new TimeLineEvent("Fourth Event", 240));
        timelineEvents.add(new TimeLineEvent("Fifth Event", 560));
        timelineEvents.add(new TimeLineEvent("Sixth Event", 380));

        rectPaint.setColor(getResources().getColor(R.color.colorAccent));
        rectPaint.setStyle(Paint.Style.FILL);

        textPaint.setColor(Color.WHITE);
        textPaint.setTextAlign(Paint.Align.LEFT);
        textPaint.setTextSize(textSize);

        durationPaint.setColor(Color.WHITE);
        durationPaint.setTextSize(durationSize);
        durationPaint.setTextAlign(Paint.Align.RIGHT);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        width = MeasureSpec.getSize(widthMeasureSpec);
        height = MeasureSpec.getSize(heightMeasureSpec);
    }

    @Override
    public void onDraw(Canvas canvas) {
        int prevLeft = 0;
        for (TimeLineEvent event : timelineEvents) {
            int eventWidth = (int) (event.duration * scaleFactor);
            canvas.drawRect(prevLeft, 0, prevLeft + eventWidth - rectPadding, height, rectPaint);
            canvas.drawText(event.displayName, prevLeft + rectPadding, (height + textSize)/2, textPaint);
            canvas.drawText(event.durationStr(), prevLeft + eventWidth - rectPadding*2, (height + durationSize)/2, durationPaint);
            prevLeft += eventWidth;
        }
        rightBorder = prevLeft > width ? prevLeft - width + rectPadding : leftBorder;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event)
    {
        if (gestureDetector.onTouchEvent(event)) return true;
        if (scaleGestureDetector.onTouchEvent(event)) return true;
        return true;
    }

    public void setScaleFactor(float scaleFactor) {
        this.scaleFactor *= scaleFactor;
        invalidate();
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            scrollBy((int)distanceX, 0);

            int scrollX = getScrollX();
            if (scrollX < leftBorder)
                scrollTo(leftBorder, 0);
            if (scrollX > rightBorder)
                scrollTo(rightBorder, 0);

            return true;
        }
    }

    private List<TimeLineEvent> timelineEvents;

    public void setTimelineEvents(List<TimeLineEvent> timelineEvents) {
        this.timelineEvents = timelineEvents;
    }

    public static class TimeLineEvent {

        private float weight;

        private String displayName;

        private int duration;

        String durationStr() {
            return String.format("%d:%02d", duration / 60, duration % 60);
        }

        public TimeLineEvent(String displayName, int duration){
            this.displayName = displayName;
            this.duration = duration;
            this.weight = duration;
        }
    }

    private class MyScaleGestureListener implements ScaleGestureDetector.OnScaleGestureListener {

        public boolean onScale(ScaleGestureDetector detector)
        {
            scaleFactor *= detector.getScaleFactor();

            int newScrollX = (int)((getScrollX() + detector.getFocusX()) * detector.getScaleFactor() - detector.getFocusX());
            scrollTo(newScrollX, 0);

            invalidate();

            return true;
        }

        public boolean onScaleBegin(ScaleGestureDetector detector)
        {
            return true;
        }

        public void onScaleEnd(ScaleGestureDetector detector)
        {
        }
    }
}
