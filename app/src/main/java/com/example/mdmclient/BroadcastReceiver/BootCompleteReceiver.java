package com.example.mdmclient.BroadcastReceiver;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import com.example.mdmclient.MainActivity;

/*******************************************************************************
 * Start MDM Client automatically on Phone reboot.
 * Receiver to detect Android reboot event.
 *******************************************************************************/


public class BootCompleteReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Intent i = new Intent(context, MainActivity.class);
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(i);
    }
}
