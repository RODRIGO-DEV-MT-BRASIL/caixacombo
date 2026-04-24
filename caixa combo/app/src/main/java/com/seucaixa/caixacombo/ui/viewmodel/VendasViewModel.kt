package com.seucaixa.caixacombo.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.seucaixa.caixacombo.data.model.Venda
import com.seucaixa.caixacombo.data.repository.VendaRepository
import com.seucaixa.caixacombo.ui.screens.Periodo
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*

class VendasViewModel(
    private val vendaRepository: VendaRepository
) : ViewModel() {
    
    private val _vendas = MutableStateFlow<List<Venda>>(emptyList())
    val vendas: StateFlow<List<Venda>> = _vendas.asStateFlow()
    
    private val _totalVendas = MutableStateFlow(0.0)
    val totalVendas: StateFlow<Double> = _totalVendas.asStateFlow()
    
    private val _periodoSelecionado = MutableStateFlow(Periodo.TODOS)
    val periodoSelecionado: StateFlow<Periodo> = _periodoSelecionado.asStateFlow()
    
    init {
        carregarVendas()
    }
    
    private fun carregarVendas() {
        viewModelScope.launch {
            val (inicio, fim) = calcularPeriodo(_periodoSelecionado.value)
            
            vendaRepository.getVendasByPeriodo(inicio, fim).collect { lista ->
                _vendas.value = lista
                calcularTotal(lista)
            }
        }
    }
    
    fun setPeriodo(periodo: Periodo) {
        _periodoSelecionado.value = periodo
        carregarVendas()
    }
    
    private fun calcularPeriodo(periodo: Periodo): Pair<Long, Long> {
        val calendar = Calendar.getInstance()
        val fim = calendar.timeInMillis
        
        when (periodo) {
            Periodo.HOJE -> {
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
            }
            Periodo.SEMANA -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
            }
            Periodo.MES -> {
                calendar.add(Calendar.MONTH, -1)
            }
            Periodo.TODOS -> {
                calendar.set(Calendar.YEAR, 2000)
            }
        }
        
        return Pair(calendar.timeInMillis, fim)
    }
    
    private fun calcularTotal(vendas: List<Venda>) {
        _totalVendas.value = vendas.sumOf { it.total }
    }
    
    // Factory para criar ViewModel com repositório
    class Factory(
        private val vendaRepository: com.seucaixa.caixacombo.data.repository.VendaRepository
    ) : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return VendasViewModel(vendaRepository) as T
        }
    }
}
