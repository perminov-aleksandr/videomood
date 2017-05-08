package ru.spbstu.videomoodadmin;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import ru.spbstu.videomood.database.Video;

public class VideoAdapter extends ArrayAdapter<Video> {
    public VideoAdapter(@NonNull Context context, @LayoutRes int resource) {
        super(context, resource);
    }

    private ViewHolder viewHolder;

    private static class ViewHolder {
        private TextView nameTv;
        private TextView durationTv;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(this.getContext())
                    .inflate(R.layout.video_item, parent, false);

            viewHolder = new ViewHolder();
            viewHolder.nameTv = (TextView) convertView.findViewById(R.id.video_item_name);
            viewHolder.durationTv = (TextView) convertView.findViewById(R.id.video_item_duration);

            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        Video video = getItem(position);
        if (video != null) {
            viewHolder.nameTv.setText(video.getName());

            int duration = video.getDuration();
            int secs = duration % 60;
            int mins = duration / 60;
            viewHolder.durationTv.setText(String.format("%d:%02d", mins, secs));
        }

        return convertView;
    }
}
