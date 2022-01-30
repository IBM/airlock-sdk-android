package com.ibm.airlock.sdk.util;

import android.util.Base64;
import com.ibm.airlock.common.util.Base64Decoder;


/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public class AndroidBase64 implements Base64Decoder {
    @Override
    public byte[] decode(String str) {
        return Base64.decode(str, Base64.DEFAULT);
    }
}
