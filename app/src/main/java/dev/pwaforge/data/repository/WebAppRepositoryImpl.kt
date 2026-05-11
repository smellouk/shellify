package dev.pwaforge.data.repository

import dev.pwaforge.data.local.dao.WebAppDao
import dev.pwaforge.data.mapper.toDomain
import dev.pwaforge.data.mapper.toEntity
import dev.pwaforge.domain.model.WebApp
import dev.pwaforge.domain.repository.WebAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class WebAppRepositoryImpl(private val dao: WebAppDao) : WebAppRepository {

    override fun getAll(): Flow<List<WebApp>> =
        dao.getAll().map { list -> list.map { it.toDomain() } }

    override fun getByCategory(categoryId: Long): Flow<List<WebApp>> =
        dao.getByCategory(categoryId).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): WebApp? =
        dao.getById(id)?.toDomain()

    override suspend fun getByUrl(url: String): WebApp? =
        dao.getByUrl(url)?.toDomain()

    override suspend fun save(app: WebApp): Long {
        val entity = app.toEntity()
        return if (entity.id == 0L) dao.insert(entity)
        else { dao.update(entity); entity.id }
    }

    override suspend fun delete(app: WebApp) = dao.delete(app.toEntity())

    override suspend fun deleteById(id: Long) = dao.deleteById(id)

    override suspend fun clearIsolatedData(isolationId: String) {
        // Actual WebView profile/cookie clearing is handled in IsolationManager.
        // Nothing to do at the DB layer.
    }
}
