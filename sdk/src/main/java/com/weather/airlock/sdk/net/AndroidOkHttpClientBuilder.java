package com.weather.airlock.sdk.net;

import android.os.Build;
import android.util.Log;

import com.ibm.airlock.common.net.OkHttpClientBuilder;
import com.ibm.airlock.common.net.interceptors.ResponseDecryptor;
import com.ibm.airlock.common.net.interceptors.ResponseExtractor;

import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

import okhttp3.ConnectionSpec;
import okhttp3.OkHttpClient;
import okhttp3.TlsVersion;


/**
 * Created by Denis Voloshin on 02/11/2017.
 */

public class AndroidOkHttpClientBuilder implements OkHttpClientBuilder {

    private boolean enableTls12O = false;

    public AndroidOkHttpClientBuilder(){
        super();
    }

    public AndroidOkHttpClientBuilder(boolean enableTls12O) {
        this.enableTls12O = enableTls12O;
    }

    /**
     * Enables TLSv1.2 protocol (which is disabled by default)
     * on pre-Lollipop devices, as well as on Lollipop, because some issues can take place on Samsung devices.
     *
     * @param client OKHttp client builder
     */
    private OkHttpClient.Builder enableTls12OnPreLollipop(OkHttpClient.Builder client) {
        if ((Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) || enableTls12O) {
            try {

                final TrustManager[] trustAllCerts = new TrustManager[]{
                        new X509TrustManager() {
                            @Override
                            public void checkClientTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public void checkServerTrusted(java.security.cert.X509Certificate[] chain, String authType) throws CertificateException {
                            }

                            @Override
                            public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                                return new java.security.cert.X509Certificate[]{};
                            }
                        }
                };

                SSLContext sc = SSLContext.getInstance("TLSv1.2");
                sc.init(null, trustAllCerts, new java.security.SecureRandom());

                TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init((KeyStore) null);
                TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
                if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
                    return client;
                }
                X509TrustManager trustManager = (X509TrustManager) trustManagers[0];
                client.sslSocketFactory(new Tls12SocketFactory(sc.getSocketFactory()), trustManager);

                ConnectionSpec cs = new ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                        .tlsVersions(TlsVersion.TLS_1_2)
                        .build();

                List<ConnectionSpec> specs = new ArrayList<>();
                specs.add(cs);
                specs.add(ConnectionSpec.COMPATIBLE_TLS);
                specs.add(ConnectionSpec.CLEARTEXT);
                client.connectionSpecs(specs);
            } catch (Exception exc) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc);
            }
        }
        return client;
    }


    @Override
    public OkHttpClient create(String encryptionKey) {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS).
                        addInterceptor(new ResponseExtractor()).
                        addInterceptor(new ResponseDecryptor(encryptionKey));
        return enableTls12OnPreLollipop(client).build();
    }

    @Override
    public OkHttpClient create() {
        OkHttpClient.Builder client = new OkHttpClient.Builder()
                .followRedirects(true)
                .followSslRedirects(true)
                .cache(null)
                .connectTimeout(15, TimeUnit.SECONDS)
                .writeTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS);
        return enableTls12OnPreLollipop(client).build();
    }
}
