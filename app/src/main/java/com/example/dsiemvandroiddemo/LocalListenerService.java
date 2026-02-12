package com.example.dsiemvandroiddemo;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import fi.iki.elonen.NanoHTTPD;
import kotlin.ExperimentalUnsignedTypes;

@ExperimentalUnsignedTypes
public class LocalListenerService extends Service
{
    private static final int LOCAL_LISTENER_PORT = 8080;

    private Thread serviceThread = null;

    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder
    {
        public LocalListenerService getService()
        {
            return LocalListenerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return binder;
    }

    @Override
    public void onCreate()
    {
        super.onCreate();
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if (serviceThread == null)
        {
            serviceThread = new Thread(this::localListenerServiceThread);
            serviceThread.setPriority(Thread.MAX_PRIORITY);
            serviceThread.start();

            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            {
                NotificationChannel notificationChannel = new NotificationChannel("Local Listener Service", "Local Listener Service", NotificationManager.IMPORTANCE_LOW);
                notificationChannel.setDescription("Local Listener Service");
                notificationManager.createNotificationChannel(notificationChannel);
            }
            Notification notification = new NotificationCompat.Builder(getApplicationContext(), "Local Listener Service")
                    .setSmallIcon(R.drawable.ic_launcher_background)
                    .setContentTitle("Local Listener Service")
                    .setContentText("Local Listener Service")
                    //.setAutoCancel(true)
                    .build();

            //startForeground(3, notification);
        }
        else
        {
            //LOGGER.finest("Local listener service is already running");
        }

        return START_STICKY;
    }

    public static void start(Context callerContext)
    {
        Intent startIntent = new Intent(callerContext, LocalListenerService.class);
        callerContext.startService(startIntent);
    }

    public void stop()
    {
        serviceThread.interrupt();
        try { serviceThread.join(); } catch (InterruptedException ignored) { }
        stopForeground(true);
        stopSelf();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
    }

    private void localListenerServiceThread()
    {
        //LOGGER.finest("Starting Local Listener service");
        LocalListener localListener = new LocalListener(LOCAL_LISTENER_PORT, this);

        //LOGGER.info("Local listener HTTP server is starting");
        try
        {
            localListener.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false);
            //LOGGER.info("Local listener HTTP server has started");
        }
        catch (Exception e)
        {
            //LOGGER.warning("Error starting Local Listener HTTP server: " + e.getMessage());
        }

        synchronized (this) { try { wait(); } catch (InterruptedException e) { /*LOGGER.info("Stopping Local Listener service thread");*/ } }

        //LOGGER.info("Local listener HTTP server is stopping");
        localListener.stop();
        //LOGGER.info("Local listener HTTP server has stopped");

        //LOGGER.info("Stopped Local Listener service thread");
    }

    private static class LocalListener extends NanoHTTPD
    {
        @NonNull
        private final Context context;

        public LocalListener(int port, @NonNull Context context)
        {
            super(port);
            this.context = context;
        }

        @Override
        public Response serve(IHTTPSession session) {
            Method method = session.getMethod();
            String uri = session.getUri();
            //com.example.dsiemvandroiddemo.LocalListener.LOG.info(method + " '" + uri + "' ");
            Map<String, String> files = new HashMap<String, String>();
            if (method == Method.POST) {
                String returnMSG = "";
                try {
                    byte[ ] Latin1RequestArray = new byte[ session.getInputStream( ).available( ) ];
                    session.getInputStream( ).read( Latin1RequestArray );
                    String UTF8RequestMsg = new String( Latin1RequestArray, StandardCharsets.ISO_8859_1 );

                    if ( !UTF8RequestMsg.contains( "TransactionCancel" ) )
                    {
                        returnMSG = dsiEMVAndroidinstance.getInstance(context).ProcessTransaction( UTF8RequestMsg );
                    }
                    else
                    {
                        dsiEMVAndroidinstance.getInstance(context).CancelRequest();
                    }

                } catch (Exception ex) {
                    Log.i("Local Listener", ex.getMessage());
                }


                return newFixedLengthResponse(returnMSG);
            }

            return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT,
                    "The requested resource does not exist");
        }
    }
}