package com.example.data.repository

import com.example.data.database.EditProject
import com.example.data.database.EditProjectDao
import kotlinx.coroutines.flow.Flow

class PhotoEditorRepository(private val dao: EditProjectDao) {
    val allProjects: Flow<List<EditProject>> = dao.getAllProjects()

    suspend fun getProjectById(id: Int): EditProject? {
        return dao.getProjectById(id)
    }

    suspend fun saveProject(project: EditProject): Long {
        return dao.insertProject(project)
    }

    suspend fun deleteProject(id: Int) {
        dao.deleteProjectById(id)
    }
    
    suspend fun clearHistory() {
        dao.deleteAllProjects()
    }
}
