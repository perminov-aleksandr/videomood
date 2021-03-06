package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.support.annotation.NonNull;
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
import ru.spbstu.videomood.database.SeanceVideo;

public class SeanceVideoAdapter extends ArrayAdapter<SeanceVideo> {

    private ViewHolder viewHolder;

    private static class ViewHolder {
        private TextView nameTextView;
        private TextView durationTextView;
    }

    public SeanceVideoAdapter(Context context, int resource) {
        super(context, resource);
    }

    @NonNull
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext()).inflate(R.layout.seance_video_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.nameTextView = (TextView) convertView.findViewById(R.id.seancevideoitem_name);
            viewHolder.durationTextView = (TextView) convertView.findViewById(R.id.seancevideoitem_duration);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        SeanceVideo seanceVideo = getItem(position);
        if (seanceVideo != null) {
            viewHolder.nameTextView.setText(seanceVideo.video.getName());
            long timestamp = seanceVideo.getTimestamp();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm:ss");
            viewHolder.durationTextView.setText(simpleDateFormat.format(new Date(timestamp)));
        }

        return convertView;
    }
}
