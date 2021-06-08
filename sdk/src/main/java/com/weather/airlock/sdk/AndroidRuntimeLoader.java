package com.weather.airlock.sdk;


import android.content.Context;
import android.content.res.AssetManager;

import com.ibm.airlock.common.cache.RuntimeLoader;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class AndroidRuntimeLoader extends RuntimeLoader {

    Context context;

    AndroidRuntimeLoader(String pathToFiles, String encryptionKey, Context context) {
        super(pathToFiles, encryptionKey);
        this.context = context;
    }

    @Override
    protected InputStream getInputStream(String name) {
        AssetManager mng = context.getAssets();
        InputStream fileIn = null;
        try {
            fileIn = mng.open(pathToFiles + File.separator + name);
        } catch (IOException e) {
            //do nothing - just return null
        }
        return fileIn;
    }
}
