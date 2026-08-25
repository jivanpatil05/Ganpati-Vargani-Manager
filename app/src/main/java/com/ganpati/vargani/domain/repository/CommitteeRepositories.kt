package com.ganpati.vargani.domain.repository

import com.ganpati.vargani.domain.model.AppUserProfile
import com.ganpati.vargani.domain.model.FestivalEvent
import com.ganpati.vargani.domain.model.Member
import com.ganpati.vargani.domain.model.UserRole
import kotlinx.coroutines.flow.Flow

interface UserManagementRepository {
    fun observeCommitteeUsers(): Flow<List<AppUserProfile>>
    suspend fun setUserRole(uid: String, role: UserRole)
    suspend fun inviteViewer(email: String, name: String, password: String, mobile: String = "")
}

interface MemberRepository {
    fun observeMembers(): Flow<List<Member>>
    suspend fun saveMember(member: Member): String
    suspend fun deleteMember(id: String)
}

interface EventRepository {
    fun observeEvents(): Flow<List<FestivalEvent>>
    suspend fun saveEvent(event: FestivalEvent): String
    suspend fun deleteEvent(id: String)
}
