package ru.spbstu.videomood.database;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class User {

    public int id;

    public String firstName;

    public String lastName;

    public String birthDateStr;

    private Date birthDate = null;

    public Date getBirthDate() {
        if (birthDate == null)
            try {
                birthDate = dateFormat.parse(birthDateStr);
            } catch (ParseException e) {
                e.printStackTrace();
                return null;
            }
        return birthDate;
    }

    public String getBirthDateFormatted() {
        if (birthDate == null)
            getBirthDate();
        return dateFormat.format(birthDate);
    }

    private void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public final DateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy");

    private void setBirthDate(String dateToParse) {
        try {
            this.birthDate = dateFormat.parse(dateToParse);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    private String sex;

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }
}
