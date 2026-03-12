package com.example.dsiemvandroiddemo;

import android.content.Context;
import android.util.Log;

import com.datacap.android.dsiEMVAndroid;

public class dsiEMVAndroidinstance
{
    private static final String TAG = "dsiEMVAndroid";
    private static volatile dsiEMVAndroid instance;

    public static dsiEMVAndroid getInstance(Context context)
    {
        if (instance == null)
        {
            synchronized (dsiEMVAndroid.class)
            {
                if(instance == null)
                {
                    instance = new dsiEMVAndroid(context);
                }

            }

        }
        return instance;
    }

    public static String processTransaction(Context context, String xmlRequest)
    {
        Log.i(TAG, "ProcessTransaction request: " + xmlRequest);
        String response = getInstance(context).ProcessTransaction(xmlRequest);
        Log.i(TAG, "ProcessTransaction response: " + response);
        return response;
    }

}
