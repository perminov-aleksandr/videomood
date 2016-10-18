package ru.spbstu.videomood.ru.spbstu.videomood.activities;

import android.content.Intent;
import android.os.Bundle;
import android.app.Activity;
import android.view.View;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import ru.spbstu.videomood.Const;
import ru.spbstu.videomood.R;
import ru.spbstu.videomood.User;

public class UserActivity extends Activity {

    private static final String TAG = "VideoMood:UserActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user);

        ageRadioGroup = initRadioGroup(R.id.ageButtonsLayout, Const.ageRanges);
        moodRadioGroup = initRadioGroup(R.id.moodButtonsLayout, Const.moods);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //todo: write values if anything selected
        //todo: write muse index
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        //todo: read selected values if they do exist
    }

    private RadioGroup moodRadioGroup;
    private RadioGroup ageRadioGroup;

    private RadioGroup initRadioGroup(int viewId, Object[] items) {
        RadioGroup radioGroup = (RadioGroup) findViewById(viewId);

        for (int i = 0; i < items.length; i++) {
            RadioButton radioButton = new RadioButton(this);
            radioButton.setText(items[i].toString());
            radioGroup.addView(radioButton);
        }

        return radioGroup;
    }

    private int getSelectedIndex(RadioGroup radioGroup) {
        int radioButtonID = radioGroup.getCheckedRadioButtonId();
        if (radioButtonID == -1)
            return -1;

        View radioButton = radioGroup.findViewById(radioButtonID);
        return radioGroup.indexOfChild(radioButton);
    }

    /**
     * Try to go to VideoActivity with validation
     * @param view
     */

    public void goToVideo(View view) {
        int ageIndex = getSelectedIndex(ageRadioGroup);
        int moodIndex = getSelectedIndex(moodRadioGroup);

        if (!isUserDataValid(ageIndex, moodIndex))
            return;

        User.setCurrentMood(Const.moods[moodIndex]);
        User.setRange(Const.ageRanges[ageIndex]);

        Intent intent = new Intent(this, VideoActivity.class);
        startActivity(intent);
    }

    private boolean isUserDataValid(int ageIndex, int moodIndex) {
        if (ageIndex == -1) {
            displayValidationMessage(getResources().getString(R.string.noAgeRangeSelected));
            return false;
        }
        else if (moodIndex == -1)
        {
            displayValidationMessage(getResources().getString(R.string.noMoodSelected));
            return false;
        }
        return true;
    }

    private void displayValidationMessage(String s) {
        TextView validationMessage = (TextView) findViewById(R.id.validationMessage);
        validationMessage.setText(s);
        validationMessage.setVisibility(View.VISIBLE);
    }
}
