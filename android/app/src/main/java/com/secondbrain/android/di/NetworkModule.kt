package com.secondbrain.android.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Room
import com.secondbrain.android.capture.SecondBrainDatabase
import com.secondbrain.android.BuildConfig
import com.secondbrain.android.data.remote.SecondBrainApi
import com.secondbrain.android.data.session.SessionStore
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

private val Context.sessionDataStore by preferencesDataStore(name = "secondbrain_session")

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides @Singleton
    fun provideStore(@ApplicationContext context: Context) = context.sessionDataStore

    @Provides @Singleton
    fun provideDatabase(@ApplicationContext context: Context): SecondBrainDatabase =
        Room.databaseBuilder(context, SecondBrainDatabase::class.java, "secondbrain.db").build()

    @Provides @Singleton
    fun provideClient(sessionStore: SessionStore): OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(Interceptor { chain ->
            val token = runBlocking { sessionStore.token() }
            val request = chain.request().newBuilder().apply {
                token?.let { header("Authorization", "Bearer $it") }
            }.build()
            chain.proceed(request)
        })
        .addInterceptor(Interceptor { chain ->
            val response = chain.proceed(chain.request())
            if (response.code == 401) {
                runBlocking { sessionStore.clear() }
            }
            response
        })
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    @Provides @Singleton
    fun provideApi(client: OkHttpClient): SecondBrainApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_BASE_URL)
        .client(client)
        .addConverterFactory(MoshiConverterFactory.create(Moshi.Builder().add(KotlinJsonAdapterFactory()).build()))
        .build()
        .create(SecondBrainApi::class.java)
}
