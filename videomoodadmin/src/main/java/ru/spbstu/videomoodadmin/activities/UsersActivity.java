package ru.spbstu.videomoodadmin.activities;

import android.animation.AnimatorInflater;
import android.animation.AnimatorSet;
import android.animation.LayoutTransition;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.transition.Slide;
import android.transition.Transition;
import android.transition.TransitionManager;
import android.transition.TransitionValues;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.j256.ormlite.android.apptools.OrmLiteBaseActivity;
import com.j256.ormlite.dao.Dao;

import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.Collection;

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.Sex;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.VideoMoodDbHelper;
import ru.spbstu.videomoodadmin.AdminConst;
import ru.spbstu.videomoodadmin.R;
import ru.spbstu.videomoodadmin.SeanceAdapter;
import ru.spbstu.videomoodadmin.UserAdapter;

import android.util.Log;

public class UsersActivity extends OrmLiteBaseActivity<VideoMoodDbHelper> {

    private View createUserForm;
    private View userCard;
    private RadioGroup sexRadioGroup;
    private UserAdapter userAdapter;
    private Dao<User, Integer> userDao;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            userDao = getHelper().getUserDao();
        } catch (SQLException e) {
            e.printStackTrace();
        }

        setContentView(R.layout.activity_users);

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
        hideCreateUserForm();

        userCard = findViewById(R.id.usercard);
        hideUserCard();

        sexRadioGroup = (RadioGroup) findViewById(R.id.sex_radiogroup);

        setupAnimations();
    }

    private void setupAnimations() {
        Point outSize = new Point();
        getWindowManager().getDefaultDisplay().getSize(outSize);
        float width = outSize.x/2;

        ObjectAnimator animIn = ObjectAnimator.
                ofFloat(null, "translationX", width, 0f);
        ObjectAnimator animOut = ObjectAnimator.
                ofFloat(null, "translationX", 0f, width);

        LayoutTransition transition = new LayoutTransition();
        transition.setAnimator(LayoutTransition.APPEARING, animIn);
        transition.setAnimator(LayoutTransition.DISAPPEARING, animOut);
        transition.setInterpolator(LayoutTransition.APPEARING, new DecelerateInterpolator());
        transition.setInterpolator(LayoutTransition.DISAPPEARING, new AccelerateInterpolator());

        ((ViewGroup)findViewById(R.id.userCardContainer)).setLayoutTransition(transition);
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
        DatePicker birthDateView = (DatePicker) findViewById(R.id.birthdate_editbox);
        int year = birthDateView.getYear();
        int month = birthDateView.getMonth()+1;
        int day = birthDateView.getDayOfMonth();
        String birthdate = String.format("%1$02d.%2$02d.%3$04d", day, month, year);
        Log.i("DATE", birthdate);

        //construct a user instance
        User userToCreate = new User();
        userToCreate.firstName = firstName;
        userToCreate.lastName = lastName;
        userToCreate.birthDateStr = birthdate;

        String selectedSexStr;
        if (sexRadioGroup.getCheckedRadioButtonId() == R.id.users_create_sex_male)
            selectedSexStr = Sex.toString(Sex.MALE);
        else
            selectedSexStr = Sex.toString(Sex.FEMALE);
        userToCreate.sex = selectedSexStr;

        try {
            if (userDao.create(userToCreate) == 0)
                throw new Exception("No user created");
            if (userToCreate.id != -1)
                userAdapter.add(userToCreate);
            else
                throw new Exception("No user created");
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(UsersActivity.this, R.string.user_create_error, Toast.LENGTH_LONG);
        }
    }

    private void createUser() {
        showCreateUserForm();
    }

    private void hideCreateUserForm() {
        createUserForm.setVisibility(View.GONE);
    }

    private void showCreateUserForm() {
        if (userCard.getVisibility() == View.VISIBLE) {
            hideUserCard();
        }
        createUserForm.setVisibility(View.VISIBLE);
    }

    private void hideUserCard() {
        userCard.setVisibility(View.GONE);
    }

    private void showUserCard() {
        if (createUserForm.getVisibility() == View.VISIBLE)
            hideCreateUserForm();
        userCard.setVisibility(View.VISIBLE);
    }

    private void cancelCreateUser() {
        hideCreateUserForm();
    }

    private ListView usersListView;

    @Override
    protected void onStart() {
        super.onStart();

        usersListView = (ListView) findViewById(R.id.usersListView);
        userAdapter = new UserAdapter(this, R.layout.user_item);
        usersListView.setAdapter(userAdapter);
        try {
            userAdapter.addAll(userDao.queryForAll());
        } catch (SQLException e) {
            e.printStackTrace();
        }

        usersListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                User user = (User) usersListView.getItemAtPosition(position);
                selectedUserId = user.id;
                try {
                    User userFromDb = userDao.queryForId(selectedUserId);
                    initUserCard(userFromDb);
                } catch (SQLException e) {
                    e.printStackTrace();
                    return;
                }
                hideCreateUserForm();
                showUserCard();
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
                        try {
                            userDao.delete(userToRemove);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        hideUserCard();
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
        sex.setText(Sex.get(user.sex) == Sex.FEMALE ? R.string.female : R.string.male );

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
        userSeancesListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                AlertDialog.Builder adb = new AlertDialog.Builder(UsersActivity.this);
                final Seance seanceToRemove = seancesAdapter.getItem(position);
                adb.setTitle(R.string.deleteSeanceDialogTittle);
                adb.setMessage(getString(R.string.deleteSeanceDialogMessage, seanceToRemove.getDateFrom()));
                adb.setNegativeButton(R.string.cancel, null);
                adb.setPositiveButton("Ok", new AlertDialog.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            getHelper().getDao(Seance.class).delete(seanceToRemove);
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }
                        seancesAdapter.remove(seanceToRemove);
                        seancesAdapter.notifyDataSetChanged();
                    }});
                adb.show();
                return true;
            }
        });

        seancesAdapter.addAll(user.seances);
    }

    private void openSeanceCard(Seance seance) {
        Intent intent = new Intent(this, SeanceActivity.class);
        intent.putExtra(AdminConst.EXTRA_SEANCE_ID, seance.getId());
        startActivityForResult(intent, RESULT_OK);
    }
}
