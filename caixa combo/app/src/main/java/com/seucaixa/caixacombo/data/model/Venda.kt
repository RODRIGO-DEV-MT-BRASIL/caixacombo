package com.seucaixa.caixacombo.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

@Entity(tableName = "vendas")
@TypeConverters(Converters::class)
data class Venda(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    val numero: String, // Número único da venda
    val dataHora: Long = System.currentTimeMillis(),
    val itens: List<ItemVenda>,
    
    // Valores
    val subtotal: Double,
    val desconto: Double = 0.0,
    val total: Double,
    
    // Pagamento
    val formaPagamento: FormaPagamento,
    val valorRecebido: Double,
    val troco: Double = 0.0,
    
    // Status
    val status: StatusVenda = StatusVenda.FINALIZADA,
    val sincronizado: Boolean = false,
    
    // Stone
    val stoneAtk: String? = null, // Authorization code da Stone para cancelamento/reimpressao
    
    // Opcional
    val clienteId: Long? = null,
    val vendedorId: Long? = null,
    val observacao: String? = null
)

data class ItemVenda(
    val produtoId: Long,
    val produtoNome: String,
    val quantidade: Double,
    val unidade: String,
    val precoUnitario: Double,
    val desconto: Double = 0.0,
    val total: Double
)

enum class FormaPagamento {
    DINHEIRO,
    CARTAO_CREDITO,
    CARTAO_DEBITO,
    PIX,
    BOLETO,
    FIADO
}

enum class StatusVenda {
    PENDENTE,
    FINALIZADA,
    CANCELADA,
    ESTORNADA
}

// TypeConverters para Room
class Converters {
    private val gson = Gson()
    
    @TypeConverter
    fun fromItemVendaList(value: List<ItemVenda>): String {
        return gson.toJson(value)
    }
    
    @TypeConverter
    fun toItemVendaList(value: String): List<ItemVenda> {
        val listType = object : TypeToken<List<ItemVenda>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromFormaPagamento(value: FormaPagamento): String {
        return value.name
    }
    
    @TypeConverter
    fun toFormaPagamento(value: String): FormaPagamento {
        return FormaPagamento.valueOf(value)
    }
    
    @TypeConverter
    fun fromStatusVenda(value: StatusVenda): String {
        return value.name
    }
    
    @TypeConverter
    fun toStatusVenda(value: String): StatusVenda {
        return StatusVenda.valueOf(value)
    }

    @TypeConverter
    fun fromCargoUsuario(value: CargoUsuario): String {
        return value.name
    }

    @TypeConverter
    fun toCargoUsuario(value: String): CargoUsuario {
        return CargoUsuario.valueOf(value)
    }
}

// Extensions
fun Venda.dataFormatada(): String {
    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
    return sdf.format(java.util.Date(dataHora))
}

fun Venda.totalFormatado(): String {
    return "R$ %.2f".format(total)
}

fun Venda.trocoFormatado(): String {
    return "R$ %.2f".format(troco)
}
