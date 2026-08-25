package com.ganpati.vargani.di

import android.content.Context
import androidx.room.Room
import com.ganpati.vargani.core.constants.AppConstants
import com.ganpati.vargani.data.local.room.VarganiDatabase
import com.ganpati.vargani.data.local.room.dao.DonationDao
import com.ganpati.vargani.data.local.room.dao.ExpenseDao
import com.ganpati.vargani.data.local.room.dao.SettingsDao
import com.ganpati.vargani.data.local.room.dao.UserDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): VarganiDatabase =
        Room.databaseBuilder(
            context,
            VarganiDatabase::class.java,
            AppConstants.DATABASE_NAME
        )
            .addMigrations(
                VarganiDatabase.MIGRATION_1_2,
                VarganiDatabase.MIGRATION_2_3,
                VarganiDatabase.MIGRATION_3_4,
                VarganiDatabase.MIGRATION_4_5,
                VarganiDatabase.MIGRATION_5_6,
            )
            .fallbackToDestructiveMigration()
            .build()

    @Provides
    fun provideDonationDao(db: VarganiDatabase): DonationDao = db.donationDao()

    @Provides
    fun provideSettingsDao(db: VarganiDatabase): SettingsDao = db.settingsDao()

    @Provides
    fun provideUserDao(db: VarganiDatabase): UserDao = db.userDao()

    @Provides
    fun provideExpenseDao(db: VarganiDatabase): ExpenseDao = db.expenseDao()
}
