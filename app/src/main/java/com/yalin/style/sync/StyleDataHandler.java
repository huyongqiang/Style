package com.yalin.style.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.net.Uri;
import android.os.RemoteException;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.yalin.style.io.JSONHandler;
import com.yalin.style.io.WallpapersHandler;
import com.yalin.style.provider.StyleContract;
import com.yalin.style.util.LogUtil;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * YaLin 2017/1/3.
 */

public class StyleDataHandler {

  private static final String TAG = "StyleDataHandler";

  private static final String DATA_KEY_WALLPAPER = "wallpapers";

  private static final String[] DATA_KEYS_IN_ORDER = {
      DATA_KEY_WALLPAPER
  };

  Context mContext = null;

  WallpapersHandler mWallpapersHandler = null;

  HashMap<String, JSONHandler> mHandlerForKey = new HashMap<>();

  private int mContentProviderOperationsDone = 0;

  public StyleDataHandler(Context context) {
    mContext = context;
  }

  public void applyStyleData(String[] dataBodies) throws IOException {
    LogUtil.d(TAG, "Applying data from " + dataBodies.length + " files");

    mHandlerForKey.put(DATA_KEY_WALLPAPER, mWallpapersHandler = new WallpapersHandler(mContext));

    LogUtil.d(TAG, "Processing " + dataBodies.length + " JSON objects.");
    for (int i = 0; i < dataBodies.length; i++) {
      LogUtil.d(TAG, "Processing json object #" + (i + 1) + " of " + dataBodies.length);
      processDataBody(dataBodies[i]);
    }

    ArrayList<ContentProviderOperation> batch = new ArrayList<>();
    for (String key : DATA_KEYS_IN_ORDER) {
      LogUtil.d(TAG, "Building content provider operations for: " + key);
      mHandlerForKey.get(key).makeContentProviderOperations(batch);
      LogUtil.d(TAG, "Content provider operations so far: " + batch.size());
    }

    LogUtil.d(TAG, "Applying " + batch.size() + " content provider operations.");
    try {
      int operations = batch.size();
      if (operations > 0) {
        mContext.getContentResolver().applyBatch(StyleContract.AUTHORITY, batch);
      }
      LogUtil.d(TAG, "Successfully applied " + operations + " content provider operations.");
      mContentProviderOperationsDone += operations;
    } catch (RemoteException ex) {
      LogUtil.d(TAG, "RemoteException while applying content provider operations.");
      throw new RuntimeException("Error executing content provider batch operation", ex);
    } catch (OperationApplicationException ex) {
      LogUtil.d(TAG, "OperationApplicationException while applying content provider operations.");
      throw new RuntimeException("Error executing content provider batch operation", ex);
    }

    LogUtil.d(TAG, "Notifying changes on all top-level paths on Content Resolver.");
    ContentResolver resolver = mContext.getContentResolver();
    for (String path : StyleContract.TOP_LEVEL_PATHS) {
      Uri uri = StyleContract.BASE_CONTENT_URI.buildUpon().appendPath(path).build();
      resolver.notifyChange(uri, null);
    }

    LogUtil.d(TAG, "Done applying conference data.");
  }

  private void processDataBody(String dataBody) throws IOException {
    JsonReader reader = new JsonReader(new StringReader(dataBody));
    JsonParser parser = new JsonParser();
    try {
      reader.setLenient(true); // To err is human

      // the whole file is a single JSON object
      reader.beginObject();

      while (reader.hasNext()) {
        String key = reader.nextName();
        if (mHandlerForKey.containsKey(key)) {
          LogUtil.d(TAG, "Processing key in conference data json: " + key);
          mHandlerForKey.get(key).process(parser.parse(reader));
        } else {
          LogUtil.d(TAG, "Skipping unknown key in conference data json: " + key);
          reader.skipValue();
        }
      }
      reader.endObject();
    } finally {
      //noinspection ThrowFromFinallyBlock
      reader.close();
    }
  }
}
