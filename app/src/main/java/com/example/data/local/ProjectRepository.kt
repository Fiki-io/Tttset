package com.example.data.local

import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.ProjectSummary
import com.example.domain.model.TdProject
import com.example.domain.model.TextDrawElement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ProjectRepository(private val dao: ProjectDao) {

    val allProjects: Flow<List<TdProject>> = dao.observeAllProjects().map { list ->
        list.map { it.toDomain() }
    }

    val projectSummaries: Flow<List<ProjectSummary>> = dao.observeAllProjects().map { list ->
        list.map { item ->
            ProjectSummary(
                id = item.project.id,
                name = item.project.name,
                elementCount = item.elements.size,
                aspectRatio = CanvasAspectRatio.fromStorageKey(item.project.aspectRatio),
                syntaxMode = PawnSyntaxMode.fromStorageKey(item.project.syntaxMode),
                updatedAt = item.project.updatedAt
            )
        }
    }

    suspend fun createProject(
        name: String,
        aspectRatio: CanvasAspectRatio = CanvasAspectRatio.RATIO_16_9,
        syntaxMode: PawnSyntaxMode = PawnSyntaxMode.SAMP_CLASSIC
    ): TdProject {
        val now = System.currentTimeMillis()
        val id = dao.insertProject(
            ProjectEntity(
                name = name,
                aspectRatio = aspectRatio.name,
                syntaxMode = syntaxMode.name,
                updatedAt = now
            )
        )
        return TdProject(
            id = id,
            name = name,
            aspectRatio = aspectRatio,
            syntaxMode = syntaxMode,
            updatedAt = now,
            elements = emptyList()
        )
    }

    suspend fun loadProject(projectId: Long): TdProject? {
        return dao.getProjectWithElements(projectId)?.toDomain()
    }

    suspend fun saveProject(project: TdProject) {
        val now = System.currentTimeMillis()
        val entity = ProjectEntity(
            id = project.id,
            name = project.name,
            aspectRatio = project.aspectRatio.name,
            syntaxMode = project.syntaxMode.name,
            updatedAt = now
        )
        val elementEntities = project.elements.mapIndexed { index, element ->
            element.toEntity(projectId = project.id, zIndex = index)
        }
        dao.replaceProjectSnapshot(entity, elementEntities)
    }

    suspend fun renameProject(projectId: Long, newName: String) {
        dao.renameProject(projectId, newName, System.currentTimeMillis())
    }

    suspend fun deleteProject(projectId: Long) {
        dao.deleteProject(projectId)
    }
}
