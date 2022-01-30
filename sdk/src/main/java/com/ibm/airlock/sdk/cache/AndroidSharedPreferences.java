package com.ibm.airlock.sdk.cache;

import java.util.Set;
import javax.annotation.Nullable;

import android.content.SharedPreferences;


/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public class AndroidSharedPreferences implements com.ibm.airlock.common.cache.SharedPreferences {

    private SharedPreferences sp;

    public AndroidSharedPreferences(SharedPreferences sp) {
        this.sp = sp;
    }

    @Override
    public boolean getBoolean(String key, boolean defValue) {
        return sp.getBoolean(key, defValue);
    }


    @Override
    public long getLong(String key, long defValue) {
        return sp.getLong(key, defValue);
    }

    @Nullable
    @Override
    public String getString(String key, @Nullable String defValue) {
        return sp.getString(key, defValue);
    }

    @Override
    public Editor edit() {
        return new AndroidEditor(this.sp.edit());
    }

    @Override
    public int getInt(String key, int defValue) {
        return sp.getInt(key, defValue);
    }

    @Override
    public Set<String> getStringSet(String key, Object o) {
        return sp.getStringSet(key, (Set<String>) o);
    }

    public class AndroidEditor implements Editor {
        private SharedPreferences.Editor editor;

        public AndroidEditor(SharedPreferences.Editor editor) {
            this.editor = editor;
        }

        @Override
        public Editor remove(String key) {
            return new AndroidEditor(editor.remove(key));
        }

        @Override
        public Editor clear() {
            return new AndroidEditor(editor.clear());
        }

        @Override
        public boolean commit() {
            return editor.commit();
        }

        @Override
        public void apply() {
            editor.apply();
        }

        @Override
        public Editor putInt(String key, int value) {
            return new AndroidEditor(editor.putInt(key, value));
        }

        @Override
        public Editor putLong(String key, long value) {
            return new AndroidEditor(editor.putLong(key, value));
        }

        @Override
        public Editor putFloat(String key, float value) {
            return new AndroidEditor(editor.putFloat(key, value));
        }

        @Override
        public Editor putBoolean(String key, boolean value) {
            return new AndroidEditor(editor.putBoolean(key, value));
        }

        @Override
        public void putString(String key, String value) {
            editor.putString(key, value);
        }
    }
}
