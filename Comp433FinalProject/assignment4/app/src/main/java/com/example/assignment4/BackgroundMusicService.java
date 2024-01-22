package com.example.assignment4;

import android.app.Service;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

public class BackgroundMusicService extends Service {
    MediaPlayer mediaPlayer;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startID){
        Log.d("mylog", "Start playing");
        mediaPlayer = MediaPlayer.create(this, R.raw.animal_crossing_bubblegum_kk_remix);
        mediaPlayer.start();
        return super.onStartCommand(intent, flags, startID);
    }

    @Override
    public boolean stopService(Intent name){
        return super.stopService(name);
    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        mediaPlayer.stop();
        mediaPlayer.release();
        mediaPlayer = null;
    }

}
