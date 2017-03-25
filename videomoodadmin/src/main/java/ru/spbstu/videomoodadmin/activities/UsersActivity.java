package ru.spbstu.videomoodadmin.activities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
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

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbContext;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.R;

public class UsersActivity extends AppCompatActivity {

    private VideoMoodDbContext dbContext;
    private View createUserForm;
    private View userCard;
    private RadioGroup sexRadioGroup;
    private UserAdapter userAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_users);

        dbContext = new VideoMoodDbContext(this);

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

        Button startSessionBtn = (Button) findViewById(R.id.startSessionBtn);
        startSessionBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startSession();
            }
        });

        createUserForm = findViewById(R.id.userCreateLayout);
        createUserForm.setVisibility(View.GONE);

        userCard = findViewById(R.id.usercard);
        userCard.setVisibility(View.GONE);

        sexRadioGroup = (RadioGroup) findViewById(R.id.sex_radiogroup);
    }

    private int selectedUserId;

    private void startSession() {
        Intent intent = new Intent(this, ConnectActivity.class);
        intent.putExtra(AdminConst.EXTRA_USER_ID, selectedUserId);
        startActivity(intent);
    }

    private void confirmCreateUser() {
        //Get values from form
        EditText firstNameView = (EditText) findViewById(R.id.firstname_textbox);
        String firstName = firstNameView.getText().toString();
        EditText lastNameView = (EditText) findViewById(R.id.lastname_textbox);
        String lastName = lastNameView.getText().toString();
        EditText birthDateView = (EditText) findViewById(R.id.birthdate_editbox);
        String birthdate = birthDateView.getText().toString();

        //construct a user instance
        User userToCreate = new User();
        userToCreate.firstName = firstName;
        userToCreate.lastName = lastName;
        userToCreate.birthDateStr = birthdate;
        RadioButton selectedSex = (RadioButton)sexRadioGroup.getChildAt(0);
        userToCreate.sex = selectedSex.getText().toString();

        //try to create it with dbContext
        dbContext.createUser(userToCreate);

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

    private ListView usersListView;

    @Override
    protected void onStart() {
        super.onStart();

        usersListView = (ListView) findViewById(R.id.usersListView);
        userAdapter = new UserAdapter(this, R.layout.user_item);
        usersListView.setAdapter(userAdapter);
        userAdapter.addAll(dbContext.getUsers());

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                createUserForm.setVisibility(View.GONE);
                User user = (User) usersListView.getItemAtPosition(position);
                selectedUserId = user.id;
                initUserCard(user);
                userCard.setVisibility(View.VISIBLE);
                usersListView.setEmptyView(findViewById(R.id.users_nodata));
            }
        });

        usersListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder adb = new AlertDialog.Builder(UsersActivity.this);
                final int positionToRemove = position;
                final User userToRemove = userAdapter.getItem(positionToRemove);
                adb.setTitle(R.string.deleteUserDialogTittle);
                adb.setMessage(getString(R.string.deleteUserDialogMessage, userToRemove.firstName));
                adb.setNegativeButton("Cancel", null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dbContext.removeUser(userToRemove.id);
                        userAdapter.remove(userToRemove);
                        userAdapter.notifyDataSetChanged();
                    }});
                adb.show();
                return true;
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
        final SeanceAdapter seancesAdapter = new SeanceAdapter(this, R.layout.seance_item);
        userSeancesListView.setAdapter(seancesAdapter);
        userSeancesListView.setEmptyView(findViewById(R.id.usercard_seances_nodata));
        userSeancesListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Seance seance = seancesAdapter.getItem(position);
                openSeanceCard(seance);
            }
        });

        seancesAdapter.addAll(dbContext.getSeances(user.id));
    }

    private void openSeanceCard(Seance seance) {
        Intent intent = new Intent(this, SeanceActivity.class);
        intent.putExtra(AdminConst.EXTRA_SEANCE_ID, seance.getId());
        intent.putExtra(AdminConst.EXTRA_SEANCE_DATEFROM, seance.getDateFrom());
        intent.putExtra(AdminConst.EXTRA_SEANCE_DATETO, seance.getDateTo());
        intent.putExtra(AdminConst.EXTRA_SEANCE_DATA, seance.getDataStr());
        startActivityForResult(intent, RESULT_OK);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }
}
