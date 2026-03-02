package com.example.dsiemvandroiddemo;

import android.app.Application;
import android.content.ComponentName;
import android.content.Context;
import android.content.ServiceConnection;
import android.os.IBinder;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class DsiEmvAndroidDemo extends Application {

//    @Nullable
//    private LocalListenerService localListenerService;
//
//    @NonNull
//    public final ServiceConnection localListenerServiceConnection = new ServiceConnection()
//    {
//        @Override
//        public void onServiceConnected(ComponentName componentName, IBinder binder)
//        {
//            localListenerService = ((LocalListenerService.LocalBinder) binder).getService();
//        }
//
//        @Override
//        public void onServiceDisconnected(ComponentName componentName)
//        {
//            localListenerService = null;
//        }
//    };

    @Override
    public void onCreate()
    {
        super.onCreate();

        LocalListenerService.start(this);
    }
}
