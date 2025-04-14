package com.example.flightbookingapp;

import android.app.job.JobParameters;
import android.app.job.JobService;

public class NotificationJobService extends JobService {

    @Override
    public boolean onStartJob(JobParameters params) {
        NotificationHelper mNotificationHelper = new NotificationHelper(getApplicationContext());
        mNotificationHelper.send("It's time to book a flight! \\ (•◡•) /");
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {
        return true;
    }
}