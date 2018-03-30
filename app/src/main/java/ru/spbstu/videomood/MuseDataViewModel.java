package ru.spbstu.videomood;

import android.arch.lifecycle.LiveData;

public class MuseDataViewModel {
    private LiveData<MuseData> museData;
    private MuseDataRepository repository;

    public MuseDataViewModel(MuseDataRepository museDataRepository){
        repository = museDataRepository;
    }

    public LiveData<MuseData> getMuseData() {
        return museData;
    }
}
