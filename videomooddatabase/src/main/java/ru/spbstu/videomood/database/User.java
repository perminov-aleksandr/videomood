package ru.spbstu.videomood.database;

import com.j256.ormlite.dao.ForeignCollection;
import com.j256.ormlite.field.DatabaseField;
import com.j256.ormlite.field.ForeignCollectionField;
import com.j256.ormlite.table.DatabaseTable;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@DatabaseTable(tableName = "users")
public class User {

    @DatabaseField(generatedId = true)
    public int id;

    @DatabaseField(canBeNull = false)
    public String firstName;

    @DatabaseField(canBeNull = false)
    public String lastName;

    @DatabaseField(canBeNull = false)
    public String sex;

    @DatabaseField(canBeNull = false)
    public String birthDateStr;

    @ForeignCollectionField()
    public ForeignCollection<Seance> seances;

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
}
