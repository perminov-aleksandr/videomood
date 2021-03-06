package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import ru.spbstu.videomood.database.Sex;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomoodadmin.R;

public class UserAdapter extends ArrayAdapter<User> {

    private ViewHolder viewHolder;

    private static class ViewHolder {
        private TextView firstNameTextView;
        private TextView lastNameTextView;
        private TextView sexTextView;
        private TextView birthdateTextView;
    }

    public UserAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.user_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.firstNameTextView = (TextView) convertView.findViewById(R.id.user_firstname);
            viewHolder.lastNameTextView = (TextView) convertView.findViewById(R.id.user_lastname);
            viewHolder.sexTextView = (TextView) convertView.findViewById(R.id.user_sex);
            viewHolder.birthdateTextView = (TextView) convertView.findViewById(R.id.user_birthdate);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        User user = getItem(position);
        if (user != null) {
            viewHolder.firstNameTextView.setText(user.firstName);
            viewHolder.lastNameTextView.setText(user.lastName);
            viewHolder.sexTextView.setText(Sex.get(user.sex) == Sex.FEMALE ? R.string.female : R.string.male);
            viewHolder.birthdateTextView.setText(user.getBirthDateFormatted());
        }

        return convertView;
    }
}
