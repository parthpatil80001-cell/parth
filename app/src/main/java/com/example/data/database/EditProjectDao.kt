package com.example.data.database

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface EditProjectDao {
    @Query("SELECT * FROM edit_projects ORDER BY timestamp DESC")
    fun getAllProjects(): Flow<List<EditProject>>

    @Query("SELECT * FROM edit_projects WHERE id = :id")
    suspend fun getProjectById(id: Int): EditProject?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProject(project: EditProject): Long

    @Update
    suspend fun updateProject(project: EditProject)

    @Delete
    suspend fun deleteProject(project: EditProject)

    @Query("DELETE FROM edit_projects WHERE id = :id")
    suspend fun deleteProjectById(id: Int)

    @Query("DELETE FROM edit_projects")
    suspend fun deleteAllProjects()
}
