package com.weather.airlock.sdk.cache;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.cache.BasePersistenceHandler;
import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SharedPreferences;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.Constants;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * Created by Denis Voloshin on 01/11/2017.
 */

public class AndroidPersistenceHandler extends BasePersistenceHandler {


    public AndroidPersistenceHandler(Context c) {
        super(c);
        init(c);
    }

    public void init(Context c, AirlockCallback callback) {
        final long startTime = System.currentTimeMillis();
        preferences = c.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        Logger.log.d(TAG, "Initialization of sharedPreferences too : " + (System.currentTimeMillis() - startTime) + " millisecond");
        context = c;
        //If first time the app starts or it is a test mock app (files dir is null) - do not read from file system
        if (isInitialized() && context.getFilesDir() != null) {
            new Thread(new FilePreferencesReader(callback)).start();
        }
    }


    public void init(Context c) {
        final long startTime = System.currentTimeMillis();
        preferences = c.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        Logger.log.d(TAG, "Initialization of sharedPreferences too : " + (System.currentTimeMillis() - startTime) + " millisecond");
        context = c;
        //If first time the app starts or it is a test mock app (files dir is null) - do not read from file system
        if (isInitialized() && context.getFilesDir() != null) {
            new Thread(new FilePreferencesReader(null)).start();
        }
    }

    public synchronized void reset(Context c) {
        this.context = c;
        preferences = c.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        clear();
    }

    public void write(String key, JSONObject value) {
        synchronized (lock) {
            if (value != null && value.length() > 0) {
                inMemoryPreferences.put(key, value);
                //if it is a test mock app (files dir is null) - do not write to file system
                if (this.context.getFilesDir() != null) {
                    new Thread(new FilePreferencesPersister(key, value.toString())).start();
                }
            } else {
                //remove data
                inMemoryPreferences.remove(key);
                context.deleteFile(key);
                new Thread(new FilePreferencesPersister(key, value.toString())).start();
            }
        }
    }

    public void write(String key, String value) {
        if (filePersistPreferences.contains(key)) {
            synchronized (lock) {

                if (value != null && !value.isEmpty()) {
                    if (saveAsJSONPreferences.contains(key)) {
                        try {
                            inMemoryPreferences.put(key, new JSONObject(value));
                        } catch (JSONException e) {
                            Logger.log.w(TAG, "Failed to convert content of: " + key + " to JSONObject.");
                        }
                    } else {
                        inMemoryPreferences.put(key, value);
                    }
                    //if it is a test mock app (files dir is null) - do not write to file system
                    if (this.context.getFilesDir() != null) {
                        new Thread(new FilePreferencesPersister(key, value)).start();
                    }
                } else {
                    //remove data
                    inMemoryPreferences.remove(key);
                    context.deleteFile(key);
                    if (filePersistPreferences.contains(key)) {
                        new Thread(new FilePreferencesPersister(key, value)).start();
                    }
                }
            }
        } else {
            Logger.log.d(TAG, "Write to SP  " + key + " = " + value);
            if (key.equals(Constants.SP_SEASON_ID)) {
                updateSeasonIdAndClearRuntimeData(value);
                return;
            }
            SharedPreferences.Editor spEditor = preferences.edit();
            spEditor.putString(key, value);
            spEditor.apply();
        }
    }

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     *
     * @param jsonAsString
     */
    public void writeStream(String name, String jsonAsString) {
        if (jsonAsString != null && !jsonAsString.isEmpty()) {
            //if it is a test mock app (files dir is null) - do not write to file system
            if (this.context.getFilesDir() != null) {
                final long startTime = System.currentTimeMillis();
                try {
                    FileOutputStream fos = context.openFileOutput(Constants.STREAM_PREFIX + name, android.content.Context.MODE_PRIVATE);
                    if (fos == null) {
                        //On tests that use mock context the FileOutputStream could be null...
                        return;
                    }
                    fos.write(jsonAsString.getBytes());
                    fos.close();
                    Logger.log.d(TAG, "Write to file system of : " + name + " took : " + (System.currentTimeMillis() - startTime));
                } catch (IOException e) {
                    Logger.log.w(TAG, "Failed to persist content of: " + name + " to file system.");
                }
            }
        } else {
            deleteStream(name);
        }
    }

    public void deleteStream(String name) {
        //if it is a test mock app (files dir is null) - do not write to file system
        if (this.context.getFilesDir() != null) {
            context.deleteFile(Constants.STREAM_PREFIX + name);
        }
    }

    /**
     * The reason this has a seperate method is because it is called when app stopps - so we need to persist synchronously
     */
    public JSONObject readStream(String name) {

        JSONObject value = null;
        name = Constants.STREAM_PREFIX + name;
        String streamValue = (String) readSinglePreferenceFromFileSystem(name);
        if (streamValue != null) {
            try {
                value = new JSONObject(streamValue);
            } catch (JSONException e) {
                //DO nothing
            }
        }
        if (value == null) {
            value = new JSONObject();
        }
        return value;
    }



    @CheckForNull
    @Nullable
    protected Object readSinglePreferenceFromFileSystem(String preferenceName) {
        //because of synchronization it is possible to reach this method but the value is inMemory...
        Object preferenceValue = null;
        synchronized (lock) {
            if (inMemoryPreferences.containsKey(preferenceName)) {
                return inMemoryPreferences.get(preferenceName);
            }
            final long startTime = System.currentTimeMillis();
            FileInputStream fis = null;
            ByteArrayOutputStream result = new ByteArrayOutputStream();
            try {
                fis = context.openFileInput(preferenceName);
                int fisLength = (int) fis.getChannel().size();
                if (fisLength > 0) {
                    byte[] buffer = new byte[(int) fis.getChannel().size()];
                    int length;
                    while ((length = fis.read(buffer)) != -1) {
                        result.write(buffer, 0, length);
                    }
                }
                if (saveAsJSONPreferences.contains(preferenceName)) {
                    preferenceValue = new JSONObject(result.toString("UTF-8"));
                } else {
                    preferenceValue = result.toString("UTF-8");
                }
                inMemoryPreferences.put(preferenceName, preferenceValue);
            } catch (FileNotFoundException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. File not found.");
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to string");
            } catch (JSONException e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } catch (Exception e) {
                Logger.log.w(TAG, "Failed to get value for: " + preferenceName + " from file system. got exception while converting content to JSON");
            } finally {
                try {
                    fis.close();
                } catch (Throwable ignore) {
                }
            }
            Logger.log.d(TAG, "Read from file system of : " + preferenceName + " took : " + (System.currentTimeMillis() - startTime));
        }
        return preferenceValue;
    }

    public class FilePreferencesPersister implements Runnable {
        private final String key;
        @Nullable
        private final String value;

        public FilePreferencesPersister(String key, @Nullable String value) {
            this.key = key;
            this.value = value;
        }

        public void run() {
            final long startTime = System.currentTimeMillis();
            try {
                FileOutputStream fos = context.openFileOutput(key, android.content.Context.MODE_PRIVATE);
                if (fos == null) {
                    //On tests that use mock context the FileOutputStream could be null...
                    return;
                }
                fos.write(value == null ? "".getBytes() : value.getBytes());
                fos.close();
                Logger.log.d(TAG, "Write to file system of : " + key + " took : " + (System.currentTimeMillis() - startTime));
            } catch (IOException e) {
                Logger.log.w(TAG, "Failed to persist content of: " + key + " to file system.");
            }
        }
    }

    private class FilePreferencesReader implements Runnable {

        @Nullable
        private AirlockCallback callback;

        public FilePreferencesReader(AirlockCallback callback) {
            this.callback = callback;
        }

        public void run() {
            for (String preferenceName : filePersistPreferences) {
                readSinglePreferenceFromFileSystem(preferenceName);
            }
            if (this.callback != null) {
                this.callback.onSuccess("");
            }
        }
    }

    @CheckForNull
    @Override
    public JSONObject getPurchasesRandomMap() {
        return null;
    }

    @CheckForNull
    @Override
    public void setPurchasesRandomMap(JSONObject randomMap) {

    }

}
