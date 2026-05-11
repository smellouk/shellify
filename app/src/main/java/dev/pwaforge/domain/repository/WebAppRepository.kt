package dev.pwaforge.domain.repository

import dev.pwaforge.domain.model.WebApp
import kotlinx.coroutines.flow.Flow

interface WebAppRepository {
    fun getAll(): Flow<List<WebApp>>
    fun getByCategory(categoryId: Long): Flow<List<WebApp>>
    suspend fun getById(id: Long): WebApp?
    suspend fun getByUrl(url: String): WebApp?
    suspend fun save(app: WebApp): Long
    suspend fun delete(app: WebApp)
    suspend fun deleteById(id: Long)
    suspend fun clearIsolatedData(isolationId: String)
}
