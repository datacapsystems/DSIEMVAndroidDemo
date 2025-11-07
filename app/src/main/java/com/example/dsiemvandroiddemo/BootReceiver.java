package com.example.dsiemvandroiddemo;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.util.logging.Logger;

public class BootReceiver extends BroadcastReceiver
{
    private static final Logger LOGGER = Logger.getLogger("dsiEMVAndroidDemo");
    @Override
    public void onReceive(Context context, Intent intent)
    {
        LOGGER.info("BootReceiver.onReceive called");
        String action = intent.getAction();
        LOGGER.info("Boot event received: " + action);

        // When BOOT_COMPLETED event is received
        if (action.equals("android.intent.action.QUICKBOOT_POWERON")
                || action.equals(Intent.ACTION_BOOT_COMPLETED)
                || action.equals("android.intent.action.BOOT_COMPLETED")
                || action.equals("paydroid.intent.action.BOOT_COMPLETED"))
        {
            Intent activityIntent = new Intent(context, MainActivity.class);
            activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION);
            context.startActivity(activityIntent);
            LOGGER.info("Did app come to the foreground?");
        }
    }
    public static class Secondary extends BroadcastReceiver
    {
        @Override
        public void onReceive(Context context, Intent intent)
        {
            LOGGER.info("BootReceiver.Secondary.onReceive called");
            String action = intent.getAction();
            LOGGER.info("Boot event received: " + action);
            if (action.equals("com.datacap.action.BROADCAST_RECEIVER_READY"))
            {
                Intent launchIntent = new Intent(context, MainActivity.class);
                PendingIntent pendingIntent = PendingIntent.getActivity(
                        context,
                        32347328,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                Intent activityIntent = new Intent("com.datacap.action.START_ACTIVITY");
                activityIntent.putExtra("callingPackage", "com.example.dsiemvandroiddemo");
                activityIntent.putExtra("pendingIntent", pendingIntent);
                context.sendBroadcast(activityIntent);
                context.unregisterReceiver(Secondary.this);
            }
        }
    }
}