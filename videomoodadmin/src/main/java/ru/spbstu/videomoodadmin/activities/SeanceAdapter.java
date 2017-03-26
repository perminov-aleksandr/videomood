package ru.spbstu.videomoodadmin.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomoodadmin.R;

public class SeanceAdapter extends ArrayAdapter<Seance> {

    private ViewHolder viewHolder;

    private static class ViewHolder {
        private TextView dateTextView;
        private TextView fromTextView;
        private TextView toTextView;
    }

    public SeanceAdapter(Context context, int resource) {
        super(context, resource);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.seance_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.dateTextView = (TextView) convertView.findViewById(R.id.seanceitem_date);
            viewHolder.fromTextView = (TextView) convertView.findViewById(R.id.seanceitem_from);
            viewHolder.toTextView = (TextView) convertView.findViewById(R.id.seanceitem_to);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Seance seance = getItem(position);
        if (seance != null) {
            try {
                Date dateFrom = Seance.dateFormat.parse(seance.getDateFrom());
                viewHolder.dateTextView.setText(new SimpleDateFormat("dd.MM.yyyy").format(dateFrom));

                DateFormat timeFormat = new SimpleDateFormat("hh:mm");
                viewHolder.fromTextView.setText(String.format("%s - ", timeFormat.format(dateFrom)));

                Date dateTo = Seance.dateFormat.parse(seance.getDateTo());
                viewHolder.toTextView.setText(timeFormat.format(dateTo));
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        return convertView;
    }
}
