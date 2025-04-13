package com.example.roomkspdemo.data.repository

import com.example.roomkspdemo.data.dao.CategoriaDao
import com.example.roomkspdemo.data.entities.Categoria
import com.example.roomkspdemo.data.relations.CategoriaConPreguntas
import kotlinx.coroutines.flow.Flow

/**
 * Repositorio para operaciones relacionadas con las categorías de inspección.
 *
 * Esta clase proporciona métodos para acceder y manipular datos de categorías,
 * sirviendo como una capa de abstracción entre la base de datos y la UI.
 */
class CategoriaRepository(private val categoriaDao: CategoriaDao) {

    // Obtener todas las categorías como Flow
    val allCategorias: Flow<List<Categoria>> = categoriaDao.getAllCategorias()

    // Obtener categorías por modelo como Flow
    fun getCategoriasByModelo(modelo: String): Flow<List<Categoria>> {
        return categoriaDao.getCategoriasByModelo(modelo)
    }

    // Obtener todas las categorías con sus preguntas como Flow
    val allCategoriasConPreguntas: Flow<List<CategoriaConPreguntas>> = categoriaDao.getAllCategoriasConPreguntas()

    // Obtener categorías con preguntas por modelo como Flow
    fun getCategoriasConPreguntasByModelo(modelo: String): Flow<List<CategoriaConPreguntas>> {
        return categoriaDao.getCategoriasConPreguntasByModelo(modelo)
    }

    // Insertar una nueva categoría
    suspend fun insert(categoria: Categoria): Long {
        return categoriaDao.insertCategoria(categoria)
    }

    // Insertar varias categorías a la vez
    suspend fun insertAll(categorias: List<Categoria>): List<Long> {
        return categoriaDao.insertCategorias(categorias)
    }

    // Actualizar una categoría existente
    suspend fun update(categoria: Categoria) {
        categoriaDao.updateCategoria(categoria)
    }

    // Eliminar una categoría
    suspend fun delete(categoria: Categoria) {
        categoriaDao.deleteCategoria(categoria)
    }

    // Obtener una categoría por su ID
    suspend fun getCategoriaById(id: Long): Categoria? {
        return categoriaDao.getCategoriaById(id)
    }

    // Obtener una categoría con sus preguntas por ID
    suspend fun getCategoriaConPreguntasById(id: Long): CategoriaConPreguntas? {
        return categoriaDao.getCategoriaConPreguntasById(id)
    }

    // Contar el número total de categorías
    suspend fun countCategorias(): Int {
        return categoriaDao.countCategorias()
    }
}