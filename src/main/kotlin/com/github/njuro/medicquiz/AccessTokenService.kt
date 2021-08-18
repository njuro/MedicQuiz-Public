package com.github.njuro.medicquiz

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Service
import java.time.Duration
import java.time.LocalDateTime
import javax.annotation.PreDestroy

@Service
class AccessTokenService(val mapper: ObjectMapper) {

    companion object {
        private val alphabet = ('a'..'z') + ('0'..'9')
    }

    val accessTokens = loadAccessTokens()
    val activeSessions = loadActiveSessions()

    fun generateAccessToken(testNumber: Int): String {
        val token = List(16) { alphabet.random() }.joinToString("")
        accessTokens[token] = testNumber
        saveAccessTokens()
        return token
    }

    fun isValidAccessToken(token: String?): Boolean {
        return accessTokens.containsKey(token)
    }

    fun invalidateAccessToken(token: String?) {
        accessTokens.remove(token)
        saveAccessTokens()
    }

    fun startSession(token: String) {
        activeSessions[token] = LocalDateTime.now()
        saveActiveSessions()
    }

    fun isActiveSession(token: String?): Boolean {
        return activeSessions.containsKey(token)
    }

    fun endSession(token: String): Int {
        val start = activeSessions.remove(token)
        val duration = Duration.between(start, LocalDateTime.now()).toMinutes().toInt()
        saveActiveSessions()
        return duration
    }

    private fun saveAccessTokens() {
        mapper.writeValue(Config.tokensPath.toFile(), accessTokens)
    }

    private fun saveActiveSessions() {
        mapper.writeValue(Config.sessionsPath.toFile(), activeSessions)
    }

    private final fun loadAccessTokens(): MutableMap<String, Int> {
        return try {
            mapper.readValue(Config.tokensPath.toFile(), object : TypeReference<MutableMap<String, Int>>() {})
        } catch (ex: Exception) {
            println("Failed to load tokens from file: ${ex.message}")
            mutableMapOf()
        }
    }

    private final fun loadActiveSessions(): MutableMap<String, LocalDateTime> {
        return try {
            mapper.readValue(
                Config.sessionsPath.toFile(),
                object : TypeReference<MutableMap<String, LocalDateTime>>() {})
        } catch (ex: Exception) {
            println("Failed to load sessions from file: ${ex.message}")
            mutableMapOf()
        }
    }

    @PreDestroy
    fun saveData() {
        // just to be sure
        saveAccessTokens()
        saveActiveSessions()
    }
}