package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import com.example.domain.model.CanvasAspectRatio
import com.example.domain.model.PawnSyntaxMode
import com.example.domain.model.TdAlignment
import com.example.domain.model.TdFont
import com.example.domain.model.TdProject
import com.example.domain.model.TextDrawElement
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "projects")
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val aspectRatio: String,
    val syntaxMode: String,
    val updatedAt: Long
)

@Entity(
    tableName = "textdraw_elements",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["projectId"])]
)
data class TextDrawEntity(
    @PrimaryKey(autoGenerate = true) val rowId: Long = 0,
    val projectId: Long,
    val elementId: Long,
    val varName: String,
    val isPlayerTextDraw: Boolean,
    val x: Float,
    val y: Float,
    val text: String,
    val font: Int,
    val alignment: Int,
    val letterSizeX: Float,
    val letterSizeY: Float,
    val textSizeX: Float,
    val textSizeY: Float,
    val colorRgba: Long,
    val boxColorRgba: Long,
    val backgroundColorRgba: Long,
    val useBox: Boolean,
    val outline: Int,
    val shadow: Int,
    val proportional: Boolean,
    val selectable: Boolean,
    val previewModel: Int,
    val previewRotX: Float,
    val previewRotY: Float,
    val previewRotZ: Float,
    val previewZoom: Float,
    val previewVehCol1: Int,
    val previewVehCol2: Int,
    val zOrder: Int,
    val isVisible: Boolean,
    val isLocked: Boolean
)

data class ProjectWithElements(
    @Embedded val project: ProjectEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "projectId"
    )
    val elements: List<TextDrawEntity>
)

@Dao
interface ProjectDao {
    @Transaction
    @Query("SELECT * FROM projects ORDER BY updatedAt DESC")
    fun observeAllProjects(): Flow<List<ProjectWithElements>>

    @Transaction
    @Query("SELECT * FROM projects WHERE id = :projectId")
    suspend fun getProjectWithElements(projectId: Long): ProjectWithElements?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: ProjectEntity): Long

    @Query("UPDATE projects SET name = :name, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun renameProject(projectId: Long, name: String, updatedAt: Long)

    @Query("UPDATE projects SET aspectRatio = :aspectRatio, syntaxMode = :syntaxMode, updatedAt = :updatedAt WHERE id = :projectId")
    suspend fun updateProjectSettings(projectId: Long, aspectRatio: String, syntaxMode: String, updatedAt: Long)

    @Query("DELETE FROM projects WHERE id = :projectId")
    suspend fun deleteProject(projectId: Long)

    @Query("DELETE FROM textdraw_elements WHERE projectId = :projectId")
    suspend fun deleteElementsForProject(projectId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertElements(elements: List<TextDrawEntity>)

    @Transaction
    suspend fun replaceProjectSnapshot(
        project: ProjectEntity,
        elements: List<TextDrawEntity>
    ) {
        insertProject(project)
        deleteElementsForProject(project.id)
        if (elements.isNotEmpty()) {
            insertElements(elements)
        }
    }
}

@Database(
    entities = [ProjectEntity::class, TextDrawEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun projectDao(): ProjectDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "td_studio.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}

fun ProjectWithElements.toDomain(): TdProject {
    return TdProject(
        id = project.id,
        name = project.name,
        aspectRatio = CanvasAspectRatio.fromStorageKey(project.aspectRatio),
        syntaxMode = PawnSyntaxMode.fromStorageKey(project.syntaxMode),
        updatedAt = project.updatedAt,
        elements = elements.sortedBy { it.zOrder }.map { it.toDomain() }
    )
}

fun TextDrawEntity.toDomain(): TextDrawElement {
    return TextDrawElement(
        id = elementId,
        varName = varName,
        isPlayerTextDraw = isPlayerTextDraw,
        x = x,
        y = y,
        text = text,
        font = TdFont.fromInt(font),
        alignment = TdAlignment.fromInt(alignment),
        letterSizeX = letterSizeX,
        letterSizeY = letterSizeY,
        textSizeX = textSizeX,
        textSizeY = textSizeY,
        colorRgba = colorRgba,
        boxColorRgba = boxColorRgba,
        backgroundColorRgba = backgroundColorRgba,
        useBox = useBox,
        outline = outline,
        shadow = shadow,
        proportional = proportional,
        selectable = selectable,
        previewModel = previewModel,
        previewRotX = previewRotX,
        previewRotY = previewRotY,
        previewRotZ = previewRotZ,
        previewZoom = previewZoom,
        previewVehCol1 = previewVehCol1,
        previewVehCol2 = previewVehCol2,
        zOrder = zOrder,
        isVisible = isVisible,
        isLocked = isLocked
    )
}

fun TextDrawElement.toEntity(projectId: Long, zIndex: Int): TextDrawEntity {
    return TextDrawEntity(
        projectId = projectId,
        elementId = id,
        varName = varName,
        isPlayerTextDraw = isPlayerTextDraw,
        x = x,
        y = y,
        text = text,
        font = font.value,
        alignment = alignment.value,
        letterSizeX = letterSizeX,
        letterSizeY = letterSizeY,
        textSizeX = textSizeX,
        textSizeY = textSizeY,
        colorRgba = colorRgba,
        boxColorRgba = boxColorRgba,
        backgroundColorRgba = backgroundColorRgba,
        useBox = useBox,
        outline = outline,
        shadow = shadow,
        proportional = proportional,
        selectable = selectable,
        previewModel = previewModel,
        previewRotX = previewRotX,
        previewRotY = previewRotY,
        previewRotZ = previewRotZ,
        previewZoom = previewZoom,
        previewVehCol1 = previewVehCol1,
        previewVehCol2 = previewVehCol2,
        zOrder = zIndex,
        isVisible = isVisible,
        isLocked = isLocked
    )
}
