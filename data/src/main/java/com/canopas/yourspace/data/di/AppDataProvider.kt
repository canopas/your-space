package com.canopas.yourspace.data.di

import android.content.Context
import com.canopas.yourspace.data.storage.room.LocationTableDatabase
import com.google.firebase.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AppDataProvider {
    @Provides
    @Singleton
    fun provideFirebaseDb(): FirebaseFirestore =
        Firebase.firestore

    @Provides
    @Singleton
    fun provideLocationTableDatabase(@ApplicationContext context: Context): LocationTableDatabase =
        LocationTableDatabase.getInstance(context)
}
