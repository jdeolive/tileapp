package org.skunkworks.tileapp;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class TileServer extends NanoHTTPD {

    static final Pattern TILES_URI_RE = Pattern.compile("/(\\w+)/+tiles/+(\\d+)/+(\\d+)/+(\\d+).(\\w+)");

    static final String TEXT_PLAIN = "text/plain";

    public TileServer(int port) throws IOException {
        super(port, null);
    }

    @Override
    public Response serve(String uri, String method, Properties header, Properties parms, 
        Properties files) {

        logRequest(uri, method, header, parms, files);

        Matcher m = TILES_URI_RE.matcher(uri);
        if (m.matches()) {
            return handleTileRequest(uri, method, header, parms, files, m);
        }
        else {
            return new Response(HTTP_BADREQUEST, TEXT_PLAIN, "bad request");
        }
    }

    Response handleTileRequest(String uri, String method, Properties header, Properties parms, 
        Properties files, Matcher m) {

        //look for the sqlite db file for this math
        String map = m.group(1);
        File dbFile = new File(new File(
            Environment.getExternalStorageDirectory(), "Maps"), map + ".db");

        if (!dbFile.exists()) {
            return new Response(HTTP_NOTFOUND, TEXT_PLAIN, "No such map: " + map);
        }

        //TODO: cache this connection
        SQLiteDatabase db = SQLiteDatabase.openDatabase(
            dbFile.getPath(), null, SQLiteDatabase.OPEN_READONLY);
        try {

            //check format requested = format provided
            String format = m.group(5);

            String availFormat = lookupFormat(db);
            if (!availFormat.equalsIgnoreCase(format)) {
                return new Response(HTTP_NOTFOUND, TEXT_PLAIN, 
                    String.format("Tile format %s not available, must be %s.", format, availFormat));
            }

            //load the tile
            //parse out the tile index
            long z = Long.parseLong(m.group(2));
            long x = Long.parseLong(m.group(3));
            long y = Long.parseLong(m.group(4));

            byte[] tile = loadTile(z,x,y,db);
            if (tile == null) {
                return new Response(HTTP_NOTFOUND, TEXT_PLAIN, 
                    String.format("No tile for z = %d, x = %d, y = %d.", z, x, y));
            }

            String mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(format);
            if (mimeType == null) {
                mimeType = "image/" + format;
            }
            return new Response(HTTP_OK, mimeType, new ByteArrayInputStream(tile));
        }
        finally {
            db.close();
        }
    }

    String lookupFormat(SQLiteDatabase db) {
        //ensure the format matches the metadata
        Cursor c = db.query(true, "metadata", new String[]{"value"}, "name = ?", 
            new String[]{"format"}, null, null, null, null);
        try {
            if (c.moveToFirst()) {
                return c.getString(0);
            }
            return null;
        }
        finally {
            c.close();
        }
    }

    byte[] loadTile(long z, long x, long y, SQLiteDatabase db) {
        Cursor c = db.query(true, "tiles", new String[]{"tile_data"}, 
            "zoom_level = ? AND tile_column = ? AND tile_row = ?", new String[]{""+z, ""+x, ""+y}, 
            null, null, null, null);

        try {
            if (c.moveToFirst()) {
                return c.getBlob(0);
            }
            return null;
        }
        finally {
            c.close();
        }
    }

    void logRequest(String uri, String method, Properties header,
            Properties parms, Properties files) {
        Log.d("request", method + " " + uri);
    }
}
