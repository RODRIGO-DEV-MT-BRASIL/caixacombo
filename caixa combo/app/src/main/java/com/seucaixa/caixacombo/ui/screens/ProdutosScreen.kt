package com.seucaixa.caixacombo.ui.screens

import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.model.Categoria
import com.seucaixa.caixacombo.data.model.Produto
import com.seucaixa.caixacombo.data.model.estoqueFormatado
import com.seucaixa.caixacombo.data.model.precoFormatado
import com.seucaixa.caixacombo.ui.viewmodel.ProdutosViewModel
import com.seucaixa.caixacombo.ui.components.CustomKeyboard
import com.seucaixa.caixacombo.ui.components.OutlinedTextFieldWithCustomKeyboard
import com.seucaixa.caixacombo.ui.components.toDoubleSafe

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutosScreen(
    viewModel: ProdutosViewModel,
    onNavigateBack: () -> Unit
) {
    val produtos by viewModel.produtos.collectAsState()
    val categorias by viewModel.categorias.collectAsState()
    val busca by viewModel.busca.collectAsState()
    val produtoEditando by viewModel.produtoEditando.collectAsState()
    val vendidosPorProduto by viewModel.vendidosPorProduto.collectAsState()
    val context = LocalContext.current

    var showAddDialog by remember { mutableStateOf(false) }
    var showCategoriaDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    var selectedCategoriaId by remember { mutableStateOf<Long?>(null) }

    // Launcher para criar arquivo CSV (Excel pode abrir)
    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportarProdutosCSV(context, it)
            Toast.makeText(context, "Produtos exportados para Excel", Toast.LENGTH_SHORT).show()
        }
    }

    // Launcher para selecionar arquivo CSV
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importarProdutosCSV(context, it)
            Toast.makeText(context, "Produtos importados de Excel", Toast.LENGTH_SHORT).show()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gestão de Produtos") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Botões de ação - Linha 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { showCategoriaDialog = true },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Folder, "Categorias")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Categorias")
                }
                Button(
                    onClick = {
                        createFileLauncher.launch("produtos_${System.currentTimeMillis()}.csv")
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Download, "Exportar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Exportar")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Botões de ação - Linha 2
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        openFileLauncher.launch(arrayOf("text/csv"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Upload, "Importar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Importar")
                }
                Button(
                    onClick = {
                        viewModel.novoProduto()
                        showAddDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Add, "Adicionar")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Produto")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Busca
            OutlinedTextField(
                value = busca,
                onValueChange = viewModel::buscarProdutos,
                label = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Abas de categorias
            val todasCategorias = listOf(null) + categorias
            TabRow(selectedTabIndex = selectedTabIndex) {
                todasCategorias.forEachIndexed { index, categoria ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = {
                            selectedTabIndex = index
                            selectedCategoriaId = categoria?.id
                        },
                        text = { Text(categoria?.nome ?: "Todos", fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lista de produtos filtrados por categoria
            val produtosFiltrados = if (selectedCategoriaId == null) {
                produtos
            } else {
                produtos.filter { it.categoriaId == selectedCategoriaId }
            }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(produtosFiltrados) { produto: com.seucaixa.caixacombo.data.model.Produto ->
                    ProdutoGerenciamentoItem(
                        produto = produto,
                        categoria = categorias.find { it.id == produto.categoriaId }?.nome ?: "Sem categoria",
                        vendidos = vendidosPorProduto[produto.id] ?: 0,
                        onEdit = {
                            viewModel.editarProduto(produto)
                            showAddDialog = true
                        },
                        onDelete = { viewModel.excluirProduto(produto) }
                    )
                }
            }
        }
    }

    // Dialog de adicionar/editar produto
    if (showAddDialog && produtoEditando != null) {
        ProdutoDialog(
            produto = produtoEditando!!,
            categorias = categorias,
            viewModel = viewModel,
            onSalvar = { viewModel.salvarProduto(it) },
            onCancelar = {
                showAddDialog = false
                viewModel.cancelarEdicao()
            }
        )
    }
    
    // Dialog de categorias
    if (showCategoriaDialog) {
        CategoriasDialog(
            categorias = categorias,
            onAdicionar = { viewModel.salvarCategoria(it) },
            onExcluir = { viewModel.excluirCategoria(it) },
            onFechar = { showCategoriaDialog = false }
        )
    }
}

@Composable
fun ProdutoGerenciamentoItem(
    produto: Produto,
    categoria: String,
    vendidos: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                produto.descricao?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Text(
                    "$categoria • Estoque: ${produto.estoqueFormatado()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Vendidos hoje: $vendidos",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    produto.precoFormatado(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Editar")
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProdutoDialog(
    produto: Produto,
    categorias: List<Categoria>,
    viewModel: ProdutosViewModel,
    onSalvar: (Produto) -> Unit,
    onCancelar: () -> Unit
) {
    var nome by remember { mutableStateOf(produto.nome) }
    var descricao by remember { mutableStateOf(produto.descricao ?: "") }
    var preco by remember { mutableStateOf(if (produto.precoVenda > 0) formatarPreco(produto.precoVenda) else "") }
    var estoque by remember { mutableStateOf(if (produto.estoque > 0) produto.estoque.toInt().toString() else "") }
    var codigoBarras by remember { mutableStateOf(produto.codigoBarras ?: "") }
    var categoriaId by remember { mutableStateOf(produto.categoriaId) }
    var unidade by remember { mutableStateOf(produto.unidade) }
    var tipoPreco by remember { mutableStateOf(produto.tipoPreco) }
    var imagem by remember { mutableStateOf(produto.imagem) }

    // Estados para controle do teclado
    var campoAtivo by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text(if (produto.id == 0L) "Novo Produto" else "Editar Produto") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Nome com teclado alfanumérico
                OutlinedTextFieldWithCustomKeyboard(
                    value = nome,
                    onValueChange = { nome = it },
                    label = "Nome *",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.fillMaxWidth()
                )

                // Descrição com teclado alfanumérico
                OutlinedTextFieldWithCustomKeyboard(
                    value = descricao,
                    onValueChange = { descricao = it },
                    label = "Descrição",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.fillMaxWidth()
                )

                // Preço com máscara monetária
                PrecoTextField(
                    value = preco,
                    onValueChange = { preco = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Estoque - numérico
                    OutlinedTextFieldWithCustomKeyboard(
                        value = estoque,
                        onValueChange = { estoque = it },
                        label = "Estoque",
                        keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                        modifier = Modifier.weight(1f),
                        allowDecimal = false
                    )

                    // Unidade - alfanumérico
                    OutlinedTextFieldWithCustomKeyboard(
                        value = unidade,
                        onValueChange = { unidade = it.uppercase() },
                        label = "Unidade",
                        keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                        modifier = Modifier.width(100.dp),
                        maxLength = 3
                    )
                }

                // Código de barras com botão de gerar automático
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextFieldWithCustomKeyboard(
                        value = codigoBarras,
                        onValueChange = { codigoBarras = it },
                        label = "Código de Barras",
                        keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                        modifier = Modifier.weight(1f),
                        maxLength = 13,
                        allowDecimal = false
                    )

                    // Botão para gerar código automático
                    IconButton(
                        onClick = { codigoBarras = viewModel.gerarCodigoBarrasAutomatico() },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Gerar automático",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                // Dropdown de categoria
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = categorias.find { it.id == categoriaId }?.nome ?: "Selecionar categoria",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Categoria") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Nenhuma") },
                            onClick = {
                                categoriaId = null
                                expanded = false
                            }
                        )
                        categorias.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.nome) },
                                onClick = {
                                    categoriaId = cat.id
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                // Botão para adicionar imagem (placeholder - funcionalidade futura)
                OutlinedButton(
                    onClick = { /* Seleção de imagem requer implementação completa */ },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        Icons.Default.Image,
                        contentDescription = "Adicionar imagem"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (imagem != null) "Imagem selecionada" else "Adicionar imagem")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val precoValor = parsePreco(preco)
                    val estoqueValor = estoque.toDoubleSafe()

                    onSalvar(
                        produto.copy(
                            nome = nome,
                            descricao = descricao.ifEmpty { null },
                            precoVenda = precoValor,
                            estoque = estoqueValor,
                            codigoBarras = codigoBarras.ifEmpty { null },
                            categoriaId = categoriaId,
                            unidade = unidade.uppercase(),
                            tipoPreco = tipoPreco,
                            imagem = imagem
                        )
                    )
                },
                enabled = nome.isNotBlank() && preco.isNotBlank()
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

// Função para formatar preço com máscara
private fun formatarPreco(valor: Double): String {
    return String.format("%.2f", valor)
}

// Função para converter string formatada em double
private fun parsePreco(valor: String): Double {
    return valor.replace(".", "").replace(",", ".").toDoubleSafe()
}

@Composable
fun PrecoTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showKeyboard by remember { mutableStateOf(false) }

    // Formatar valor para exibição com vírgula
    val valorExibicao = if (value.isEmpty()) "" else {
        val valorLimpo = value.filter { it.isDigit() }
        if (valorLimpo.isEmpty()) "" else {
            val valorCentavos = valorLimpo.toLongOrNull() ?: 0L
            val reais = valorCentavos / 100
            val centavos = valorCentavos % 100
            String.format("%d,%02d", reais, centavos)
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = valorExibicao,
            onValueChange = { },
            label = { Text("Preço de Venda *") },
            prefix = { Text("R$") },
            readOnly = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                IconButton(onClick = { showKeyboard = !showKeyboard }) {
                    Icon(
                        if (showKeyboard) Icons.Default.KeyboardArrowDown else Icons.Default.Edit,
                        contentDescription = if (showKeyboard) "Fechar teclado" else "Abrir teclado"
                    )
                }
            }
        )

        if (showKeyboard) {
            CustomKeyboard(
                value = value,
                onValueChange = { novoValor ->
                    // Limita a 10 dígitos (999.999.999,99)
                    if (novoValor.length <= 10) {
                        onValueChange(novoValor)
                    }
                },
                onDone = { showKeyboard = false },
                keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                allowDecimal = false
            )
        }
    }
}

@Composable
fun CategoriasDialog(
    categorias: List<Categoria>,
    onAdicionar: (Categoria) -> Unit,
    onExcluir: (Categoria) -> Unit,
    onFechar: () -> Unit
) {
    var novaCategoria by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onFechar,
        title = { Text("Categorias") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Adicionar nova
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = novaCategoria,
                        onValueChange = { novaCategoria = it },
                        label = { Text("Nova Categoria") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    Button(
                        onClick = {
                            if (novaCategoria.isNotBlank()) {
                                onAdicionar(Categoria(nome = novaCategoria))
                                novaCategoria = ""
                            }
                        }
                    ) {
                        Icon(Icons.Default.Add, "Adicionar")
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                // Lista
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    categorias.forEach { categoria ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(categoria.nome)
                            IconButton(onClick = { onExcluir(categoria) }) {
                                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onFechar) {
                Text("Fechar")
            }
        }
    )
}
