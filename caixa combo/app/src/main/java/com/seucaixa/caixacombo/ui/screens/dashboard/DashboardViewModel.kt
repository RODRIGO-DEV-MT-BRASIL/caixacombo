package com.seucaixa.caixacombo.ui.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.data.model.Venda
import com.seucaixa.caixacombo.data.repository.VendaRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.util.*

class DashboardViewModel(
    private val vendaRepository: VendaRepository
) : ViewModel() {

    private val _data = MutableStateFlow(DashboardData())
    val data: StateFlow<DashboardData> = _data

    init {
        carregarDados()
    }

    private fun carregarDados() {
        viewModelScope.launch {
            // Hoje
            val inicioHoje = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val fimHoje = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }.timeInMillis

            // Semana (últimos 7 dias)
            val inicioSemana = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -6)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            // Mês
            val inicioMes = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis

            val vendasHoje = vendaRepository.getVendasByPeriodoList(inicioHoje, fimHoje)
                .filter { it.status.name != "CANCELADA" }
            val vendasSemana = vendaRepository.getVendasByPeriodoList(inicioSemana, fimHoje)
                .filter { it.status.name != "CANCELADA" }
            val vendasMes = vendaRepository.getVendasByPeriodoList(inicioMes, fimHoje)
                .filter { it.status.name != "CANCELADA" }

            _data.value = DashboardData(vendasHoje, vendasSemana, vendasMes)
        }
    }

    fun refresh() = carregarDados()

    class Factory(
        private val vendaRepository: VendaRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DashboardViewModel(vendaRepository) as T
        }
    }
}
