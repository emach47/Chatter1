package com.example.chatter1


import android.app.Application
import io.realm.Realm
import io.realm.RealmConfiguration

class CustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        Realm.init(this)
        val config = RealmConfiguration.Builder()
                // テーブル設計変更
                //..... To prevent Runtime Exception on Record definition cahnge
                //.deleteRealmIfMigrationNeeded()
                .build()

        //..... Always start with an empty Realm DB
        // 全件削除
        Realm.deleteRealm(config)

        Realm.setDefaultConfiguration(config)
    }
}