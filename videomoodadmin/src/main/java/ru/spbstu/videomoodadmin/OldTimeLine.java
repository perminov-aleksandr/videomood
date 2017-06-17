package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * TODO: document your custom view class.
 */
public class OldTimeLine extends View {
    public OldTimeLine(Context context) {
        super(context);
        init(context, null, 0);
    }

    public OldTimeLine(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context, attrs, 0);
    }

    public OldTimeLine(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context, attrs, defStyle);
    }

    private GestureDetector gestureDetector;

    private List<TimeLineEvent> timelineEvents;

    private void init(Context context, AttributeSet attrs, int defStyle) {
        gestureDetector = new GestureDetector(context, new MyGestureListener());

        timelineEvents = new ArrayList<>();
        timelineEvents.add(new TimeLineEvent("First Event", 300));
        timelineEvents.add(new TimeLineEvent("Second Event", 660));
        timelineEvents.add(new TimeLineEvent("Third Event", 710));
        timelineEvents.add(new TimeLineEvent("Fourth Event", 240));
        timelineEvents.add(new TimeLineEvent("Fifth Event", 560));
        //timelineEvents.add(new TimeLineEvent("Sixth Event", 380));

        int N = timelineEvents.size();
        int half = N/2;
        int prevWeight = 1;
        int sum = 0;
        for (int i = 0; i < N; i++) {
            TimeLineEvent ev = timelineEvents.get(i);

            if (i < half) {
                ev.weight *= prevWeight * (i+1);
                prevWeight += 3;
            } else {
                ev.weight *= prevWeight * (i+1);
                prevWeight -= 3;
            }

            sum += ev.weight;
        }

        //get weights from 0..1
        for (int i = 0; i < N; i++) {
            TimeLineEvent ev = timelineEvents.get(i);
            ev.weight = ev.weight / sum;
        }

        textPaint = new Paint();
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setColor(Color.RED);
        textPaint.setTextSize(50);

        rectPaint = new Paint();
    }

    private Paint textPaint;
    private Paint rectPaint;

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int contentWidth = getWidth();
        int contentHeight = getHeight();

        if (timelineEvents == null || timelineEvents.isEmpty())
            return;

        int prevBorder = 0;
        int N = timelineEvents.size();
        int colorIncrement = 256/N;
        for (int i = 0; i < N; i++) {
            TimeLineEvent timelineEvent = timelineEvents.get(i);

            int colorValue = i * colorIncrement;
            rectPaint.setColor(Color.rgb(colorValue, colorValue, colorValue));

            int eventWidth = (int) (timelineEvent.weight * contentWidth);
            canvas.drawRect(prevBorder, 0, prevBorder + eventWidth, contentHeight, rectPaint);
            canvas.drawText(timelineEvent.displayName, prevBorder + eventWidth/2, contentHeight/2, textPaint);
            prevBorder += eventWidth;
        }
    }

    private class MyGestureListener extends GestureDetector.SimpleOnGestureListener
    {
        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY)
        {
            //scrollBy((int)distanceX, 0);
            int size = timelineEvents.size();
            int half = size / 2;
            for (int i = 0; i < size; i++) {
                TimeLineEvent event = timelineEvents.get(i);
                if (distanceX > 0) {
                    if (i < half)
                        event.weight *= distanceX;
                    else
                        event.weight /= distanceX;
                }
                else {
                    if (i < half)
                        event.weight *= -distanceX;
                    else
                        event.weight /= -distanceX;
                }
            }
            return true;
        }
    }

    public void setTimelineEvents(List<TimeLineEvent> timelineEvents) {
        this.timelineEvents = timelineEvents;
    }

    public class TimeLineEvent {

        private float weight;

        private String displayName;

        private int duration;

        public TimeLineEvent(String displayName, int duration){
            this.displayName = displayName;
            this.duration = duration;
            this.weight = duration;
        }
    }
}
