package ru.spbstu.videomoodadmin;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceDataEntry;
import ru.spbstu.videomood.database.User;

public class UserViewModel {
    public int id;
    public String firstName;
    public String lastName;
    public String sex;
    public int age;

    private String dateStart = null;
    public void setDateStart(Date time) {
        this.dateStart = Seance.dateFormat.format(time);
    }
    public String getDateStart() {
        return dateStart;
    }

    private String dateFinish = null;
    public void setDateFinish(Date time) {
        this.dateFinish = Seance.dateFormat.format(time);
    }
    public String getDateFinish() {
        return dateFinish;
    }

    public List<SeanceDataEntry> data = new ArrayList<>();

    public UserViewModel(User user) {
        this.id = user.id;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.sex = user.getSex();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(user.getBirthDate());
        int birthYear = calendar.get(Calendar.YEAR);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        this.age = currentYear - birthYear;

        this.dateStart = Seance.dateFormat.format(Calendar.getInstance().getTime());
    }
}
