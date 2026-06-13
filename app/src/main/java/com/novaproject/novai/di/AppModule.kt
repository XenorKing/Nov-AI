package com.novaproject.novai.di

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.CertificatePinner
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)

        if (com.novaproject.novai.BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY }
            )
        } else {
            // Vuln-5: SSL pinning for the proxy host (release builds only).
            // To regenerate pins after a certificate rotation:
            //   openssl s_client -connect novai-proxy.xenortvin.workers.dev:443 | \
            //   openssl x509 -pubkey -noout | openssl pkey -pubin -outform der | \
            //   openssl dgst -sha256 -binary | base64
            val pinner = CertificatePinner.Builder()
                // Cloudflare E6 intermediate CA (backs *.workers.dev)
                .add("novai-proxy.xenortvin.workers.dev", "sha256/klO23nT2ehFDXCfx3eHTDRESMz3asj1muO+4aIdjiuY=")
                // Backup: Cloudflare E5 intermediate CA
                .add("novai-proxy.xenortvin.workers.dev", "sha256/grX4Ta9HpZx6tSHkmCrvpApTQGo67CYDnvprLg5yRME=")
                .build()
            builder.certificatePinner(pinner)
        }

        return builder.build()
    }

    @Provides @Singleton
    fun provideGson(): Gson = Gson()
}
