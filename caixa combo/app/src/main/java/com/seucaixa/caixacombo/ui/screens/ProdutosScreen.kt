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
import coil.compose.AsyncImage
import com.seucaixa.caixacombo.service.PollingService
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.background

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

    // Recarregar vendas quando a tela fica visível
    LaunchedEffect(Unit) {
        viewModel.recarregarVendas()
    }

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
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Busca
            OutlinedTextField(
                value = busca,
                onValueChange = viewModel::buscarProdutos,
                label = { Text("Buscar produto...") },
                leadingIcon = { Icon(Icons.Default.Search, null) },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
            )
            
            Spacer(modifier = Modifier.height(8.dp))

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

            Spacer(modifier = Modifier.height(8.dp))

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
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem do produto
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                if (produto.imagem != null) {
                    val imageUrl = if (produto.imagem!!.startsWith("http") || produto.imagem!!.startsWith("data:")) produto.imagem!! else "${PollingService.getServerUrl()}${produto.imagem}"
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = produto.nome,
                        modifier = Modifier.size(48.dp),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.Inventory,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
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

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (produto.id == 0L) "Novo Produto" else "Editar Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                },
                actions = {
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
                        enabled = nome.isNotBlank() && preco.isNotBlank(),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextFieldWithCustomKeyboard(
                value = nome,
                onValueChange = { nome = it },
                label = "Nome *",
                keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextFieldWithCustomKeyboard(
                value = descricao,
                onValueChange = { descricao = it },
                label = "Descrição",
                keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                modifier = Modifier.fillMaxWidth()
            )

            PrecoTextField(
                value = preco,
                onValueChange = { preco = it },
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextFieldWithCustomKeyboard(
                    value = estoque,
                    onValueChange = { estoque = it },
                    label = "Estoque",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                    modifier = Modifier.weight(1f),
                    allowDecimal = false
                )

                OutlinedTextFieldWithCustomKeyboard(
                    value = unidade,
                    onValueChange = { unidade = it.uppercase() },
                    label = "Unidade",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.width(100.dp),
                    maxLength = 3
                )
            }

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

                IconButton(
                    onClick = { codigoBarras = viewModel.gerarCodigoBarrasAutomatico() },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = "Gerar automático", tint = primaryColor)
                }
            }

            // Categoria - chips selecionáveis
            Text("Categoria", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(selected = categoriaId == null, onClick = { categoriaId = null }, label = { Text("Sem categoria") })
                categorias.forEach { cat ->
                    FilterChip(
                        selected = categoriaId == cat.id,
                        onClick = { categoriaId = cat.id },
                        label = { Text(cat.nome) },
                        leadingIcon = if (cat.cor != null) {
                            {
                                Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(16.dp),
                                    tint = try { Color(android.graphics.Color.parseColor(cat.cor)) } catch (_: Exception) { primaryColor })
                            }
                        } else null
                    )
                }
            }

            OutlinedButton(
                onClick = { /* Seleção de imagem requer implementação completa */ },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Image, contentDescription = "Adicionar imagem")
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (imagem != null) "Imagem selecionada" else "Adicionar imagem")
            }
        }
    }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoriasDialog(
    categorias: List<Categoria>,
    onAdicionar: (Categoria) -> Unit,
    onExcluir: (Categoria) -> Unit,
    onFechar: () -> Unit
) {
    var novaCategoriaNome by remember { mutableStateOf("") }
    var novaCategoriaCor by remember { mutableStateOf("#2196F3") }
    var novaCategoriaOrdem by remember { mutableStateOf("0") }
    var editandoCategoria by remember { mutableStateOf<Categoria?>(null) }
    var editandoNome by remember { mutableStateOf("") }
    var editandoCor by remember { mutableStateOf("#2196F3") }
    var editandoOrdem by remember { mutableStateOf("0") }

    val primaryColor = MaterialTheme.colorScheme.primary
    val coresPredefinidas = listOf(
        "#F44336" to "Vermelho", "#FF9800" to "Laranja", "#FFEB3B" to "Amarelo",
        "#4CAF50" to "Verde", "#2196F3" to "Azul", "#9C27B0" to "Roxo",
        "#795548" to "Marrom", "#607D8B" to "Cinza"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Categorias", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onFechar) {
                        Icon(Icons.Default.Close, "Fechar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp, vertical = 8.dp).verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Nova categoria
            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Nova Categoria", style = MaterialTheme.typography.titleSmall, color = primaryColor)
                    OutlinedTextFieldWithCustomKeyboard(value = novaCategoriaNome, onValueChange = { novaCategoriaNome = it }, label = "Nome *", keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC, modifier = Modifier.fillMaxWidth())
                    Text("Cor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        coresPredefinidas.forEach { (hex, label) ->
                            FilterChip(selected = novaCategoriaCor == hex, onClick = { novaCategoriaCor = hex }, label = { Text(label, fontSize = 11.sp) }, leadingIcon = {
                                Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                            })
                        }
                    }
                    OutlinedTextFieldWithCustomKeyboard(value = novaCategoriaOrdem, onValueChange = { novaCategoriaOrdem = it }, label = "Ordem", keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC, modifier = Modifier.width(100.dp), allowDecimal = false)
                    Button(onClick = {
                        if (novaCategoriaNome.isNotBlank()) {
                            onAdicionar(Categoria(nome = novaCategoriaNome, cor = novaCategoriaCor, ordem = novaCategoriaOrdem.toIntOrNull() ?: 0))
                            novaCategoriaNome = ""; novaCategoriaCor = "#2196F3"; novaCategoriaOrdem = "0"
                        }
                    }, modifier = Modifier.fillMaxWidth(), enabled = novaCategoriaNome.isNotBlank()) {
                        Icon(Icons.Default.Add, contentDescription = null); Spacer(modifier = Modifier.width(4.dp)); Text("Adicionar")
                    }
                }
            }

            // Lista existentes
            Text("Categorias Existentes", style = MaterialTheme.typography.titleSmall, color = primaryColor)
            if (categorias.isEmpty()) {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                    Text("Nenhuma categoria cadastrada", modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            categorias.sortedBy { it.ordem }.forEach { categoria ->
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (editandoCategoria?.id == categoria.id) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface)) {
                    if (editandoCategoria?.id == categoria.id) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextFieldWithCustomKeyboard(value = editandoNome, onValueChange = { editandoNome = it }, label = "Nome", keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC, modifier = Modifier.fillMaxWidth())
                            Text("Cor", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                coresPredefinidas.forEach { (hex, label) ->
                                    FilterChip(selected = editandoCor == hex, onClick = { editandoCor = hex }, label = { Text(label, fontSize = 11.sp) }, leadingIcon = {
                                        Box(modifier = Modifier.size(12.dp).clip(RoundedCornerShape(2.dp)).background(Color(android.graphics.Color.parseColor(hex))))
                                    })
                                }
                            }
                            OutlinedTextFieldWithCustomKeyboard(value = editandoOrdem, onValueChange = { editandoOrdem = it }, label = "Ordem", keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC, modifier = Modifier.width(100.dp), allowDecimal = false)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(onClick = { onAdicionar(categoria.copy(nome = editandoNome, cor = editandoCor, ordem = editandoOrdem.toIntOrNull() ?: 0)); editandoCategoria = null }, modifier = Modifier.weight(1f)) {
                                    Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Salvar")
                                }
                                OutlinedButton(onClick = { editandoCategoria = null }, modifier = Modifier.weight(1f)) { Text("Cancelar") }
                            }
                        }
                    } else {
                        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Box(modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp)).background(try { Color(android.graphics.Color.parseColor(categoria.cor)) } catch (_: Exception) { primaryColor }))
                                Column { Text(categoria.nome, fontWeight = FontWeight.Medium); if (categoria.ordem > 0) Text("Ordem: ${categoria.ordem}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                            }
                            Row {
                                IconButton(onClick = { editandoCategoria = categoria; editandoNome = categoria.nome; editandoCor = categoria.cor ?: "#2196F3"; editandoOrdem = categoria.ordem.toString() }) { Icon(Icons.Default.Edit, "Editar", tint = primaryColor) }
                                IconButton(onClick = { onExcluir(categoria) }) { Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }
}
