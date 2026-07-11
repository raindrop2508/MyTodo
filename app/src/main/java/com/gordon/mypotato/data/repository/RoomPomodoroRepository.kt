package com.gordon.mypotato.data.repository

import com.gordon.mypotato.data.dao.PomodoroSessionDao
import com.gordon.mypotato.data.mapper.toDomain
import com.gordon.mypotato.data.mapper.toDomainList
import com.gordon.mypotato.data.mapper.toEntity
import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPomodoroRepository(
    private val pomodoroSessionDao: PomodoroSessionDao
) : PomodoroRepository {

    override fun getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> {
        return pomodoroSessionDao.getSessionsByTaskId(taskId).map { it.toDomainList() }
    }

    override suspend fun getSessionById(id: Long): PomodoroSession? {
        return pomodoroSessionDao.getSessionById(id)?.toDomain()
    }

    override suspend fun addSession(session: PomodoroSession): Long {
        return pomodoroSessionDao.insert(session.toEntity())
    }

    override suspend fun updateSession(session: PomodoroSession) {
        pomodoroSessionDao.update(session.toEntity())
    }

    override suspend fun updateSessionStatus(id: Long, status: SessionStatus) {
        pomodoroSessionDao.updateStatus(id, status.value)
    }

    override suspend fun deleteSession(id: Long) {
        pomodoroSessionDao.deleteById(id)
    }
}