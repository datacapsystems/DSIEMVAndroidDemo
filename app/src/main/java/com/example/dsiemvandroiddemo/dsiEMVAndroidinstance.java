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
                    // Use the application context. Current Activity is supplied via setActivity() in
                    // MainActivity.onResume().
                    instance = new dsiEMVAndroid(context.getApplicationContext());
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
