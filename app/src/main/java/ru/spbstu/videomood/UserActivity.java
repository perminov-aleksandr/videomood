package ru.spbstu.videomood;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

public class UserActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        setAgeRanges();
        setMoods();
    }

    private void setMoods() {
        String[] moods = new String[]{
                "=)", "=|", "=)"
        };

        LinearLayout moodButtonsLayout = (LinearLayout) findViewById(R.id.moodButtonsLayout);
        for (int i = 0; i < moods.length; i++) {
            Button moodButton = new Button(this);
            moodButton.setText(moods[i]);
            moodButtonsLayout.addView(moodButton);
        }
    }

    private void setAgeRanges() {
        AgeRange[] ageRanges = new AgeRange[]{
                new AgeRange(0,6),
                new AgeRange(6,12),
                new AgeRange(12,16),
                new AgeRange(16,25),
                new AgeRange(25,99),
        };

        LinearLayout ageButtonsLayout = (LinearLayout) findViewById(R.id.ageButtonsLayout);
        for (int i = 0; i < ageRanges.length; i++) {
            Button ageButton = new Button(this);
            ageButton.setText(ageRanges[i].toString());
            ageButtonsLayout.addView(ageButton);
        }
    }

    public void goToVideo(View view) {
        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }
}
