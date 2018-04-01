package ru.spbstu.videomood;

import android.arch.lifecycle.LiveData;
import android.arch.lifecycle.ViewModel;

public class MuseDataViewModel extends ViewModel {
    private MuseDataRepository repository;

    public MuseDataViewModel(MuseDataRepository museDataRepository){
        repository = museDataRepository;
    }

    public LiveData<MuseData> getMuseData() {
        return repository.getMuseData();
    }
}
