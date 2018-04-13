package ru.spbstu.videomoodadmin;

import android.util.Log;

import com.j256.ormlite.dao.Dao;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;

import ru.spbstu.videomood.btservice.VideoItem;
import ru.spbstu.videomood.database.Seance;
import ru.spbstu.videomood.database.SeanceVideo;
import ru.spbstu.videomood.database.User;
import ru.spbstu.videomood.database.Video;
import ru.spbstu.videomood.database.VideoMoodDbHelper;

import static android.content.ContentValues.TAG;

public class UsersRepository {

    private Dao<User, Integer> userDao;
    private Dao<Seance, Integer> seanceDao;
    private Dao<Video, ?> videoDao;
    private Dao<SeanceVideo, ?> seanceVideoDao;

    public void init(VideoMoodDbHelper videoMoodDbHelper) {
        try {
            userDao = videoMoodDbHelper.getUserDao();
            seanceDao = videoMoodDbHelper.getDao(Seance.class);
            videoDao = videoMoodDbHelper.getDao(Video.class);
            seanceVideoDao = videoMoodDbHelper.getDao(SeanceVideo.class);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public User get(int userId) throws SQLException {
        return userDao.queryForId(userId);
    }

    public void updateSeance(Seance seance) throws SQLException {
        seanceDao.update(seance);
    }

    public int saveSeance(Seance seance) throws SQLException {
        return seanceDao.create(seance);
    }

    public void saveSeanceDataChunk(UserViewModel userViewModel, Seance seance) throws SQLException {
        List<Video> videoEntities = videoDao.queryForEq("path", userViewModel.getCurrentVideoName());
        if (videoEntities.isEmpty())
            return;

        Video currentVideo = videoEntities.get(0);

        SeanceVideo seanceVideo = new SeanceVideo();
        seanceVideo.video = currentVideo;
        seanceVideo.seance = seance;

        Calendar seanceStart = Calendar.getInstance();
        seanceStart.setTime(userViewModel.getSeanceDateStart());

        seanceVideo.setData(userViewModel.seanceData);
        userViewModel.seanceData.clear();

        seanceVideoDao.create(seanceVideo);
    }

    public void syncVideosWithDb(ArrayList<VideoItem> remoteVideoList) throws SQLException {
        List<Video> dbVideoList;
        dbVideoList = videoDao.queryForAll();

        //fill the set of remote video names
        HashSet<String> remoteVideosNames = new HashSet<>();
        for (VideoItem remoteVideo : remoteVideoList)
            remoteVideosNames.add(remoteVideo.getName());

        HashSet<String> dbVideosNames = new HashSet<>();
        for (Video dbVideo : dbVideoList) {
            String dbVideoName = dbVideo.getName();
            if (remoteVideosNames.contains(dbVideoName))
                //fill the set of db video names
                dbVideosNames.add(dbVideoName);
            else
                //remove absent videos from db
                videoDao.delete(dbVideo);
        }

        for (VideoItem remoteVideo : remoteVideoList) {
            String remoteVideoName = remoteVideo.getName();
            //skip existing db videos
            if (dbVideosNames.contains(remoteVideoName))
                continue;

            //add absent remote videos to db
            Video video = new Video();
            video.setName(remoteVideo.getName());
            video.setPath(remoteVideo.getName());
            video.setDuration(remoteVideo.getDuration());
            videoDao.create(video);
        }
    }
}