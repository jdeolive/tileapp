package org.skunkworks.tileapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class Start extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        context.startService(new Intent(context, TileService.class));
    }

}
