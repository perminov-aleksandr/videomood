package ru.spbstu.videomoodadmin;

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

    private String seanceDateStart = null;
    public void setSeanceDateStart(Date time) {
        this.seanceDateStart = Seance.dateFormat.format(time);
    }
    public String getSeanceDateStart() {
        return seanceDateStart;
    }

    private String dateFinish = null;
    public void setDateFinish(Date time) {
        this.dateFinish = Seance.dateFormat.format(time);
    }
    public String getDateFinish() {
        return dateFinish;
    }

    public List<SeanceDataEntry> seanceData = new ArrayList<>();

    public UserViewModel(User user) {
        this.id = user.id;
        this.firstName = user.firstName;
        this.lastName = user.lastName;
        this.sex = user.sex;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(user.getBirthDate());
        int birthYear = calendar.get(Calendar.YEAR);
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        this.age = currentYear - birthYear;

        this.seanceDateStart = Seance.dateFormat.format(Calendar.getInstance().getTime());
    }
}
