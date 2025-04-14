package com.example.roomkspdemo.ui.adapters

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.roomkspdemo.data.relations.CategoriaConPreguntas
import com.example.roomkspdemo.ui.inspection.CategoryQuestionsFragment
import com.example.roomkspdemo.ui.viewmodels.RespuestaViewModel

/**
 * Adaptador para el ViewPager2 que muestra las diferentes categorías de preguntas.
 *
 * @param activity La actividad host que contiene el ViewPager2
 * @param categorias Lista de categorías con sus preguntas
 * @param modeloCAEX El modelo de CAEX seleccionado (797F o 798AC)
 * @param inspeccionId El ID de la inspección actual
 * @param respuestaViewModel El ViewModel para gestionar las respuestas
 */
class CategoryPagerAdapter(
    activity: FragmentActivity,
    private val categorias: List<CategoriaConPreguntas>,
    private val modeloCAEX: String,
    private val inspeccionId: Long,
    private val respuestaViewModel: RespuestaViewModel
) : FragmentStateAdapter(activity) {

    // Lista de fragmentos creados para poder acceder a ellos después
    private val fragments = mutableMapOf<Int, CategoryQuestionsFragment>()

    override fun getItemCount(): Int = categorias.size

    override fun createFragment(position: Int): Fragment {
        // Crear el fragmento para esta categoría
        val fragment = CategoryQuestionsFragment.newInstance(
            categorias[position],
            modeloCAEX,
            inspeccionId
        )

        // Guardar referencia al fragmento
        fragments[position] = fragment

        return fragment
    }

    /**
     * Obtiene el nombre de la categoría en la posición especificada.
     *
     * @param position La posición de la categoría
     * @return El nombre de la categoría o una cadena vacía si no existe
     */
    fun getCategoriaNombre(position: Int): String {
        return if (position in categorias.indices) {
            categorias[position].categoria.nombre
        } else {
            ""
        }
    }

    /**
     * Verifica si todas las preguntas en una categoría han sido respondidas.
     *
     * @param position La posición de la categoría
     * @return true si todas las preguntas están respondidas, false en caso contrario
     */
    fun verificarRespuestasCategoria(position: Int): Boolean {
        // Obtener el fragmento para esta posición
        val fragment = fragments[position]

        // Si el fragmento no existe o no tiene preguntas, retornar verdadero
        return fragment?.verificarTodasRespuestas() ?: true
    }

    /**
     * Obtiene el ViewModel de respuestas.
     * Este método es utilizado por los fragmentos de categoría.
     *
     * @return El ViewModel de respuestas
     */
    fun getRespuestaViewModel(): RespuestaViewModel {
        return respuestaViewModel
    }
}