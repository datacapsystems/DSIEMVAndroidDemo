package com.example.dsiemvandroiddemo;

import android.content.Context;

import com.datacap.android.dsiEMVAndroid;

public class dsiEMVAndroidinstance
{
    private static volatile dsiEMVAndroid instance;

    public static dsiEMVAndroid getInstance(Context context)
    {
        // Double-checked locking with volatile instance field
        dsiEMVAndroid localInstance = instance;
        if (localInstance == null)
        {
            synchronized (dsiEMVAndroidinstance.class)
            {
                localInstance = instance;
                if(localInstance == null)
                {
                    // Use application context to avoid memory leaks
                    instance = localInstance = new dsiEMVAndroid(context.getApplicationContext());
                }
            }
        }
        return localInstance;
    }

}
