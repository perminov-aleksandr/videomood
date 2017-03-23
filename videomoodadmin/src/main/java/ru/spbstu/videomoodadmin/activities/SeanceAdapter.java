package ru.spbstu.videomoodadmin.activities;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

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
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.user_item, parent, false);

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
            //todo: extract times and dates
            viewHolder.dateTextView.setText(seance.dateFrom);
            viewHolder.fromTextView.setText(seance.dateFrom);
            viewHolder.toTextView.setText(seance.dateTo);
        }

        return convertView;
    }
}
