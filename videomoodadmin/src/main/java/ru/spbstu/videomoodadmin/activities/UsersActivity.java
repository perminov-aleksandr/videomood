package ru.spbstu.videomoodadmin.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbWorker;
import ru.spbstu.videomoodadmin.R;

public class UsersActivity extends AppCompatActivity {

    private VideoMoodDbWorker dbWorker;
    private View createUserForm;
    private View userCard;
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

        createUserForm = findViewById(R.id.userCreateLayout);
        createUserForm.setVisibility(View.GONE);

        userCard = findViewById(R.id.usercard);
        userCard.setVisibility(View.GONE);

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

        if (userToCreate.id != -1)
            userAdapter.add(userToCreate);
        else {
            Toast.makeText(this, R.string.user_create_error, Toast.LENGTH_LONG);
        }
    }

    private void createUser() {
        createUserForm.setVisibility(View.VISIBLE);
        if (userCard.getVisibility() == View.VISIBLE)
            userCard.setVisibility(View.GONE);
    }

    private void cancelCreateUser() {
        createUserForm.setVisibility(View.GONE);
    }

    @Override
    protected void onStart() {
        super.onStart();

        final ListView usersListView =  (ListView) findViewById(R.id.usersListView);
        userAdapter = new UserAdapter(this, R.layout.user_item);
        usersListView.setAdapter(userAdapter);
        userAdapter.addAll(dbWorker.getUsers());

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                createUserForm.setVisibility(View.GONE);
                User user = (User) usersListView.getItemAtPosition(position);
                initUserCard(user);
                userCard.setVisibility(View.VISIBLE);
                usersListView.setEmptyView(findViewById(R.id.users_nodata));
            }
        });
    }

    private void initUserCard(User user) {
        TextView firstName = (TextView) findViewById(R.id.usercard_firstname);
        firstName.setText(user.firstName);
        TextView lastName = (TextView) findViewById(R.id.usercard_lastname);
        lastName .setText(user.lastName);
        TextView birthdate = (TextView) findViewById(R.id.usercard_birthdate);
        birthdate.setText(user.birthDateStr);
        TextView sex = (TextView) findViewById(R.id.usercard_sex);
        sex.setText(user.sex);

        ListView userSeancesListView = (ListView) findViewById(R.id.usercard_seances);
        SeanceAdapter adapter = new SeanceAdapter(this, R.layout.seance_item);
        userSeancesListView.setAdapter(adapter);
        userSeancesListView.setEmptyView(findViewById(R.id.usercard_seances_nodata));

        adapter.addAll(dbWorker.getSeances(user.id));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
