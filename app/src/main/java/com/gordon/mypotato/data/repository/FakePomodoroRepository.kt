package com.gordon.mypotato.data.repository

import com.gordon.mypotato.domain.PomodoroSession
import com.gordon.mypotato.domain.SessionStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakePomodoroRepository private constructor() : PomodoroRepository {

    private val sessions = mutableListOf<PomodoroSession>()
    private val sessionsFlow = MutableStateFlow<List<PomodoroSession>>(emptyList())
    private val sessionIdGenerator = AtomicLong(1)

    companion object {
        @Volatile
        private var instance: FakePomodoroRepository? = null

        fun getInstance(): FakePomodoroRepository {
            return instance ?: synchronized(this) {
                instance ?: FakePomodoroRepository().also { instance = it }
            }
        }
    }

    private fun emitSessions() {
        sessionsFlow.value = ArrayList(sessions)
    }

    override fun getSessionsByTaskId(taskId: Long): Flow<List<PomodoroSession>> {
        return sessionsFlow.asStateFlow().map { it.filter { session -> session.taskId == taskId } }
    }

    override suspend fun getSessionById(id: Long): PomodoroSession? {
        delay(100)
        return sessions.find { it.id == id }
    }

    override suspend fun addSession(session: PomodoroSession): Long {
        delay(100)
        val newId = sessionIdGenerator.getAndIncrement()
        val newSession = session.copy(id = newId)
        sessions.add(newSession)
        emitSessions()
        return newId
    }

    override suspend fun updateSession(session: PomodoroSession) {
        delay(100)
        val index = sessions.indexOfFirst { it.id == session.id }
        if (index != -1) {
            sessions[index] = session
            emitSessions()
        }
    }

    override suspend fun updateSessionStatus(id: Long, status: SessionStatus) {
        delay(100)
        val index = sessions.indexOfFirst { it.id == id }
        if (index != -1) {
            sessions[index] = sessions[index].copy(status = status.value)
            emitSessions()
        }
    }

    override suspend fun deleteSession(id: Long) {
        delay(100)
        sessions.removeAll { it.id == id }
        emitSessions()
    }
}