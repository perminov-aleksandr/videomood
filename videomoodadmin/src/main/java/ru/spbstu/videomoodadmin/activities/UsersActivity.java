package ru.spbstu.videomoodadmin.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbWorker;
import ru.spbstu.videomoodadmin.R;

public class UsersActivity extends AppCompatActivity {

    private VideoMoodDbWorker dbWorker;
    private View createUserForm;
    private RadioGroup sexRadioGroup;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_users);

        dbWorker = new VideoMoodDbWorker(this);

        Button createButton = (Button) findViewById(R.id.createUserBtn);
        createButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                createUser();
            }
        });

        Button cancelCreateButton = (Button) findViewById(R.id.cancelCreateUserBtn);
        cancelCreateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                cancelCreateUser();
            }
        });

        Button confirmCreateUserBtn = (Button) findViewById(R.id.confirmCreateUserBtn);
        confirmCreateUserBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirmCreateUser();
            }
        });

        this.createUserForm = findViewById(R.id.userCreateLayout);
        createUserForm.setVisibility(View.GONE);

        sexRadioGroup = (RadioGroup) findViewById(R.id.sex_radiogroup);
    }

    private void confirmCreateUser() {
        EditText firstNameView = (EditText) findViewById(R.id.firstname_textbox);
        String firstName = firstNameView.getText().toString();
        EditText lastNameView = (EditText) findViewById(R.id.lastname_textbox);
        String lastName = lastNameView.getText().toString();
        EditText birthDateView = (EditText) findViewById(R.id.birthdate_editbox);
        String birthdate = birthDateView.getText().toString();

        User userToCreate = new User();
        userToCreate.firstName = firstName;
        userToCreate.lastName = lastName;
        userToCreate.birthDateStr = birthdate;
        RadioButton selectedSex = (RadioButton)sexRadioGroup.getChildAt(0);
        userToCreate.sex = selectedSex.getText().toString();

        dbWorker.createUser(userToCreate);

        userAdapter.add(userToCreate);
    }

    private void createUser() {
        createUserForm.setVisibility(View.VISIBLE);
    }

    private void cancelCreateUser() {
        createUserForm.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        ListView usersListView =  (ListView) findViewById(R.id.usersListView);
        userAdapter = new UserAdapter(this, R.layout.user_item);
        usersListView.setAdapter(userAdapter);

        userAdapter.addAll(dbWorker.getUsers());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
