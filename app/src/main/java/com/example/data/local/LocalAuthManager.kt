package com.example.data.local

import android.content.Context
import com.example.data.model.LocalUser
import java.security.MessageDigest
import java.util.UUID

class LocalAuthManager(context: Context, private val dao: AfiDao) {
    private val prefs = context.applicationContext.getSharedPreferences("afi_session", Context.MODE_PRIVATE)

    fun getActiveUserId(): String? = prefs.getString(KEY_ACTIVE_USER_ID, null)

    suspend fun getActiveUser(): LocalUser? {
        val uid = getActiveUserId() ?: return null
        return dao.getUserById(uid)?.toDomain()
    }

    suspend fun register(username: String, displayName: String, password: String): Result<LocalUser> {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.length < 3) return Result.failure(IllegalArgumentException("Username minimal 3 karakter"))
        if (password.length < 4) return Result.failure(IllegalArgumentException("Kata sandi minimal 4 karakter"))

        val existing = dao.getUserByUsername(cleanUser)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Username sudah terdaftar"))
        }

        val entity = LocalUserEntity(
            id = UUID.randomUUID().toString(),
            username = cleanUser,
            displayName = displayName.trim().ifBlank { cleanUser },
            passwordHash = hashPassword(cleanUser, password),
            createdAtMillis = System.currentTimeMillis()
        )
        dao.insertUser(entity)
        prefs.edit().putString(KEY_ACTIVE_USER_ID, entity.id).apply()
        return Result.success(entity.toDomain())
    }

    suspend fun login(username: String, password: String): Result<LocalUser> {
        val cleanUser = username.trim().lowercase()
        if (cleanUser.isBlank() || password.isBlank()) {
            return Result.failure(IllegalArgumentException("Username dan kata sandi wajib diisi"))
        }
        val user = dao.getUserByUsername(cleanUser)
            ?: return Result.failure(IllegalArgumentException("Akun \"$cleanUser\" belum terdaftar"))
        if (user.passwordHash != hashPassword(cleanUser, password)) {
            return Result.failure(IllegalArgumentException("Kata sandi salah"))
        }
        prefs.edit().putString(KEY_ACTIVE_USER_ID, user.id).apply()
        return Result.success(user.toDomain())
    }

    suspend fun quickGuestOwnerLogin(): Result<LocalUser> {
        val existing = dao.getUserByUsername(DEFAULT_OWNER_USERNAME)
        val user = if (existing != null) {
            existing
        } else {
            val created = LocalUserEntity(
                id = "local_owner_afi",
                username = DEFAULT_OWNER_USERNAME,
                displayName = "Pemilik Toko AFI",
                passwordHash = "",
                createdAtMillis = System.currentTimeMillis()
            )
            dao.insertUser(created)
            created
        }
        prefs.edit().putString(KEY_ACTIVE_USER_ID, user.id).apply()
        return Result.success(user.toDomain())
    }

    fun logout() {
        prefs.edit().remove(KEY_ACTIVE_USER_ID).apply()
    }

    private fun hashPassword(username: String, password: String): String {
        val input = "afi_salt_${username}_$password".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        return digest.joinToString("") { "%02x".format(it) }
    }

    private companion object {
        const val KEY_ACTIVE_USER_ID = "active_user_id"
        const val DEFAULT_OWNER_USERNAME = "pemilik"
    }
}
