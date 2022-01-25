package com.weather.airlytics.net

import android.util.Log
import okhttp3.*
import okio.BufferedSink
import okio.GzipSink
import okio.Okio
import java.io.IOException
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.net.ssl.*

class NetworkClientUtils {

    companion object {

        fun createClient(
            connectTimeOut: Long,
            writeTimeout: Long,
            readTimeout: Long,
            isDebugUser: Boolean
        ): OkHttpClient? {
            val builder =
                enableTls12(
                    OkHttpClient.Builder()
                        .addInterceptor(GzipRequestInterceptor())
                        .connectTimeout(connectTimeOut, TimeUnit.SECONDS)
                        .writeTimeout(writeTimeout, TimeUnit.SECONDS)
                        .readTimeout(readTimeout, TimeUnit.SECONDS)
                        .hostnameVerifier(HostnameVerifier { _: String, _: SSLSession ->
                            return@HostnameVerifier true
                        })
                )
            if (isDebugUser) {
                builder?.addInterceptor(LoggingInterceptor())
            }
            return builder?.build()
        }

        private fun enableTls12(client: OkHttpClient.Builder): OkHttpClient.Builder? {
            try {

                val sc = SSLContext.getInstance("TLS")
                sc.init(null, null, null)
                val trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
                trustManagerFactory.init(null as KeyStore?)
                val trustManagers =
                    trustManagerFactory.trustManagers
                if (trustManagers.size != 1 || trustManagers[0] !is X509TrustManager) {
                    return client
                }
                val trustManager =
                    trustManagers[0] as X509TrustManager
                client.sslSocketFactory(Tls12SocketFactory(sc.socketFactory), trustManager)
                val cs = ConnectionSpec.Builder(ConnectionSpec.MODERN_TLS)
                    .tlsVersions(TlsVersion.TLS_1_2)
                    .build()
                val specs: MutableList<ConnectionSpec> =
                    java.util.ArrayList()
                specs.add(cs)
                specs.add(ConnectionSpec.COMPATIBLE_TLS)
                specs.add(ConnectionSpec.CLEARTEXT)
                client.connectionSpecs(specs)
            } catch (exc: Exception) {
                Log.e("OkHttpTLSCompat", "Error while setting TLS 1.2", exc)
            }
            return client
        }

        /** This interceptor compresses the HTTP request body. Many web servers can't handle this!  */
        internal class GzipRequestInterceptor : Interceptor {
            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val originalRequest = chain.request()
                if (originalRequest.body() == null || originalRequest.header("Content-Encoding") != null) {
                    return chain.proceed(originalRequest)
                }

                val gzip = gzip(originalRequest.body())
                val compressedRequest = originalRequest.newBuilder()
                    .header("Content-Encoding", "deflate")
                    .method(originalRequest.method(), gzip)
                    .build()
                return chain.proceed(compressedRequest)
            }

            private fun gzip(body: RequestBody): RequestBody {
                return object : RequestBody() {

                    override fun contentType(): MediaType {
                        return body.contentType()
                    }

                    override fun contentLength(): Long {
                        return -1 // We don't know the compressed length in advance!
                    }

                    @Throws(IOException::class)
                    override fun writeTo(sink: BufferedSink) {
                        val gzipSink = Okio.buffer(GzipSink(sink))
                        body.writeTo(gzipSink)
                        gzipSink.close()
                    }
                }
            }
        }

        internal class LoggingInterceptor : Interceptor {
            private val logger = Logger.getLogger(LoggingInterceptor::class.java.name)

            @Throws(IOException::class)
            override fun intercept(chain: Interceptor.Chain): Response {
                val request = chain.request()
                val t1 = System.nanoTime()
                logger.info(
                    String.format(
                        "Sending request %s on %s%n%s",
                        request.url(), chain.connection(), request.headers()
                    )
                )

                val response = chain.proceed(request)

                val t2 = System.nanoTime()
                logger.info(
                    String.format(
                        "Received response for %s in %.1fms%n%s%s",
                        response.request().url(),
                        (t2 - t1) / 1e6,
                        response.headers(),
                        response.code()
                    )
                )
                return response
            }
        }
    }
}