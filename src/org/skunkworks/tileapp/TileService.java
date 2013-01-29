package org.skunkworks.tileapp;

import static org.skunkworks.tileapp.NanoHTTPD.*;

import java.io.IOException;
import java.util.Properties;

import org.skunkworks.tileapp.NanoHTTPD.Response;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;

public class TileService extends Service {

    TileServer server;

    @Override
    public void onCreate() {
        try {
            server = new TileServer(8000);
        }
        catch(IOException e) {
            Log.wtf("NanoHTTPD did not start", e);
        }
        Log.i("service", "Tile service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        server.stop();
        Log.i("service", "Tile service destroyed");
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

}
