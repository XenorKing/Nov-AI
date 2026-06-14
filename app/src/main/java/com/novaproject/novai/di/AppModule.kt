package com.novaproject.novai.di

  import android.app.Application
  import com.google.firebase.analytics.FirebaseAnalytics
  import com.google.firebase.auth.FirebaseAuth
  import com.google.firebase.firestore.FirebaseFirestore
  import com.google.gson.Gson
  import dagger.Module
  import dagger.Provides
  import dagger.hilt.InstallIn
  import dagger.hilt.components.SingletonComponent
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
      fun provideFirebaseAnalytics(app: Application): FirebaseAnalytics =
          FirebaseAnalytics.getInstance(app)

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
          }

          return builder.build()
      }

      @Provides @Singleton
      fun provideGson(): Gson = Gson()
  }
  