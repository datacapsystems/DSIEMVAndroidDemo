package com.example.dsiemvandroiddemo;

import android.content.Context;

import com.datacap.android.dsiEMVAndroid;

public class dsiEMVAndroidinstance {
private static volatile dsiEMVAndroid instance;
public static dsiEMVAndroid getInstance(Context context){
    if (instance == null) {
        synchronized (dsiEMVAndroid .class){
            if(instance == null){
                instance = new dsiEMVAndroid(context);
            }

        }

    }
    return instance;
}

}
