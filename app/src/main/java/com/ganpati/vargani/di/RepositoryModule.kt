package com.ganpati.vargani.di

import com.ganpati.vargani.data.repository.BackupRepositoryImpl
import com.ganpati.vargani.data.repository.ExportRepositoryImpl
import com.ganpati.vargani.data.repository.FirebaseAuthRepositoryImpl
import com.ganpati.vargani.data.repository.FirestoreDonationRepository
import com.ganpati.vargani.data.repository.FirestoreEventRepository
import com.ganpati.vargani.data.repository.FirestoreExpenseRepository
import com.ganpati.vargani.data.repository.FirestoreMemberRepository
import com.ganpati.vargani.data.repository.FirestoreSettingsRepository
import com.ganpati.vargani.data.repository.FirestoreUserManagementRepository
import com.ganpati.vargani.data.repository.ReceiptRepositoryImpl
import com.ganpati.vargani.domain.repository.AuthRepository
import com.ganpati.vargani.domain.repository.BackupRepository
import com.ganpati.vargani.domain.repository.DonationRepository
import com.ganpati.vargani.domain.repository.EventRepository
import com.ganpati.vargani.domain.repository.ExpenseRepository
import com.ganpati.vargani.domain.repository.ExportRepository
import com.ganpati.vargani.domain.repository.MemberRepository
import com.ganpati.vargani.domain.repository.ReceiptRepository
import com.ganpati.vargani.domain.repository.SettingsRepository
import com.ganpati.vargani.domain.repository.UserManagementRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindDonationRepository(impl: FirestoreDonationRepository): DonationRepository

    @Binds @Singleton
    abstract fun bindSettingsRepository(impl: FirestoreSettingsRepository): SettingsRepository

    @Binds @Singleton
    abstract fun bindBackupRepository(impl: BackupRepositoryImpl): BackupRepository

    @Binds @Singleton
    abstract fun bindExportRepository(impl: ExportRepositoryImpl): ExportRepository

    @Binds @Singleton
    abstract fun bindReceiptRepository(impl: ReceiptRepositoryImpl): ReceiptRepository

    @Binds @Singleton
    abstract fun bindAuthRepository(impl: FirebaseAuthRepositoryImpl): AuthRepository

    @Binds @Singleton
    abstract fun bindExpenseRepository(impl: FirestoreExpenseRepository): ExpenseRepository

    @Binds @Singleton
    abstract fun bindUserManagementRepository(
        impl: FirestoreUserManagementRepository,
    ): UserManagementRepository

    @Binds @Singleton
    abstract fun bindMemberRepository(impl: FirestoreMemberRepository): MemberRepository

    @Binds @Singleton
    abstract fun bindEventRepository(impl: FirestoreEventRepository): EventRepository
}
