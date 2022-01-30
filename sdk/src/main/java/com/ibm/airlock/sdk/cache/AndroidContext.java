package com.ibm.airlock.sdk.cache;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.SharedPreferences;


/**
 * Created by Denis Voloshin on 01/11/2017.
 */
public class AndroidContext implements Context {

    public android.content.Context context;

    public AndroidContext(android.content.Context context) {
        this.context = context;
    }

    public void setContext(android.content.Context context) {
        this.context = context;
    }

    @Override
    public File getFilesDir() {
        return context.getFilesDir();
    }

    @Override
    public SharedPreferences getSharedPreferences(String spName, int modePrivate) {
        return new AndroidSharedPreferences(this.context.getSharedPreferences(spName, modePrivate));
    }

    @Override
    public void deleteFile(String key) {
        this.context.deleteFile(key);
    }

    @Override
    public FileInputStream openFileInput(String preferenceName) throws FileNotFoundException {
        return this.context.openFileInput(preferenceName);
    }

    @Override
    public FileOutputStream openFileOutput(String name, int mode) throws FileNotFoundException {
        return this.context.openFileOutput(name, mode);
    }

    @Override
    public Object getSystemService(String name) {
        return this.context.getSystemService(name);
    }

    @Override
    public InputStream openRawResource(int name) {
        return this.context.getResources().openRawResource(name);
    }
}
