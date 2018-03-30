package ru.spbstu.videomood.activities;

import android.app.Activity;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import ru.spbstu.videomood.R;

public abstract class BaseActivity extends AppCompatActivity {

    boolean doubleBackToExitPressedOnce = false;

    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            finish();
            //android.os.Process.killProcess(android.os.Process.myPid());
        } else {
            this.doubleBackToExitPressedOnce = true;

            Toast.makeText(this, R.string.press_back_again_to_exit, Toast.LENGTH_SHORT).show();

            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    doubleBackToExitPressedOnce = false;
                }
            }, 2000);
        }
    }
}
