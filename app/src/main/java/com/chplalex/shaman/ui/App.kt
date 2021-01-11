package com.chplalex.shaman.ui

import android.app.Application
import android.os.Build
import androidx.room.Room
import com.chplalex.shaman.service.db.ShamanDB
import com.chplalex.shaman.service.db.ShamanDao
import com.chplalex.shaman.service.api.OpenWeatherRetrofit
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory

class App : Application() {

    private lateinit var db: ShamanDB
    private lateinit var openWeatherRetrofit: OpenWeatherRetrofit

    override fun onCreate() {
        super.onCreate()
        instance = this

        db = Room.databaseBuilder(applicationContext, ShamanDB::class.java, ShamanDB.DB_NAME)
            .build()

        val baseURL = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP_MR1) {
            OpenWeatherRetrofit.HTTPS + OpenWeatherRetrofit.BASE_URL
        } else {
            OpenWeatherRetrofit.HTTP + OpenWeatherRetrofit.BASE_URL
        }

        openWeatherRetrofit = Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(GsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
            .create(OpenWeatherRetrofit::class.java)
    }

    val shamanDao: ShamanDao
        get() = db.shamanDao

    val retrofit: OpenWeatherRetrofit
        get() = openWeatherRetrofit

    companion object {
        lateinit var instance: App
            private set
    }
}