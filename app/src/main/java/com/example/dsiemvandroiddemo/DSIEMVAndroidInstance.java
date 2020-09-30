package com.example.dsiemvandroiddemo;

import android.content.Context;

import com.datacap.android.DSIEMVAndroid;

public class DSIEMVAndroidInstance {
private static volatile DSIEMVAndroid instance;
public static DSIEMVAndroid getInstance(Context context){
    if (instance == null) {
        synchronized (DSIEMVAndroid .class){
            if(instance == null){
                instance = new DSIEMVAndroid(context);
            }

        }

    }
    return instance;
}

}
