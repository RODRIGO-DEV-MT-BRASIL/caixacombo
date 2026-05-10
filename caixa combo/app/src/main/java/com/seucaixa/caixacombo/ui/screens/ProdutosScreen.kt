package com.seucaixa.caixacombo.ui.screens

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

    LaunchedEffect(Unit) {
        viewModel.recarregarVendas()
    }

    var showAddDialog by remember { mutableStateOf(false) }
    var selectedMainTab by remember { mutableStateOf(0) } // 0=Produtos, 1=Categorias
    var selectedCategoriaId by remember { mutableStateOf<Long?>(null) }
    var showExportMenu by remember { mutableStateOf(false) }

    val createFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        uri?.let {
            viewModel.exportarProdutosCSV(context, it)
            Toast.makeText(context, "Produtos exportados para Excel", Toast.LENGTH_SHORT).show()
        }
    }

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            viewModel.importarProdutosCSV(context, it)
            Toast.makeText(context, "Produtos importados de Excel", Toast.LENGTH_SHORT).show()
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Gestão de Produtos", fontWeight = FontWeight.Bold)
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Voltar")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.MoreVert, "Opções")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Exportar CSV") },
                                onClick = {
                                    showExportMenu = false
                                    createFileLauncher.launch("produtos_${System.currentTimeMillis()}.csv")
                                },
                                leadingIcon = { Icon(Icons.Outlined.Download, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Importar CSV") },
                                onClick = {
                                    showExportMenu = false
                                    openFileLauncher.launch(arrayOf("text/csv"))
                                },
                                leadingIcon = { Icon(Icons.Outlined.Upload, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (selectedMainTab == 0) {
                ExtendedFloatingActionButton(
                    onClick = {
                        viewModel.novoProduto()
                        showAddDialog = true
                    },
                    icon = { Icon(Icons.Default.Add, "Novo Produto") },
                    text = { Text("Novo Produto") },
                    containerColor = primaryColor,
                    contentColor = Color.White
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Abas principais: Produtos | Categorias
            TabRow(
                selectedTabIndex = selectedMainTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = primaryColor,
                divider = {}
            ) {
                Tab(
                    selected = selectedMainTab == 0,
                    onClick = { selectedMainTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Inventory2, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Produtos", fontWeight = if (selectedMainTab == 0) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = primaryColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Tab(
                    selected = selectedMainTab == 1,
                    onClick = { selectedMainTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Outlined.Folder, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Categorias", fontWeight = if (selectedMainTab == 1) FontWeight.Bold else FontWeight.Normal)
                        }
                    },
                    selectedContentColor = primaryColor,
                    unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            HorizontalDivider()

            if (selectedMainTab == 0) {
                // ====== ABA PRODUTOS ======
                Column(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)
                ) {
                    Spacer(Modifier.height(12.dp))

                    // Busca com estilo moderno
                    OutlinedTextField(
                        value = busca,
                        onValueChange = viewModel::buscarProdutos,
                        placeholder = { Text("Buscar produto...") },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryColor) },
                        trailingIcon = {
                            if (busca.isNotEmpty()) {
                                IconButton(onClick = { viewModel.buscarProdutos("") }) {
                                    Icon(Icons.Default.Clear, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )

                    Spacer(Modifier.height(10.dp))

                    // Chips de filtro por categoria com scroll horizontal
                    val todasCategorias = listOf(null to "Todos") + categorias.map { it.id to it.nome }
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        todasCategorias.forEach { (catId, catNome) ->
                            val selected = selectedCategoriaId == catId
                            FilterChip(
                                selected = selected,
                                onClick = { selectedCategoriaId = catId },
                                label = { Text(catNome, fontSize = 12.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal) },
                                shape = RoundedCornerShape(20.dp),
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = primaryColor,
                                    selectedLabelColor = Color.White
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = MaterialTheme.colorScheme.outlineVariant,
                                    selectedBorderColor = primaryColor,
                                    borderWidth = 1.dp,
                                    selectedBorderWidth = 1.dp
                                )
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Contador
                    val produtosFiltrados = if (selectedCategoriaId == null) produtos else produtos.filter { it.categoriaId == selectedCategoriaId }
                    val filtrados = if (busca.isBlank()) produtosFiltrados else produtosFiltrados.filter {
                        it.nome.contains(busca, ignoreCase = true) || (it.codigoBarras?.contains(busca) == true)
                    }

                    Text(
                        "${filtrados.size} produto${if (filtrados.size != 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                    )

                    // Lista de produtos
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        items(filtrados, key = { it.id }) { produto ->
                            ProdutoCardModerno(
                                produto = produto,
                                categoria = categorias.find { it.id == produto.categoriaId },
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
            } else {
                // ====== ABA CATEGORIAS ======
                CategoriasTabContent(
                    categorias = categorias,
                    onAdicionar = { viewModel.salvarCategoria(it) },
                    onExcluir = { viewModel.excluirCategoria(it) }
                )
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
}

@Composable
fun ProdutoCardModerno(
    produto: Produto,
    categoria: Categoria?,
    vendidos: Int,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val catColor = try {
        categoria?.cor?.let { Color(android.graphics.Color.parseColor(it)) } ?: primaryColor
    } catch (_: Exception) { primaryColor }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagem do produto com gradiente de fundo
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(catColor.copy(alpha = 0.15f), catColor.copy(alpha = 0.05f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                com.seucaixa.caixacombo.ui.components.ProdutoImagem(
                    imagem = produto.imagem,
                    contentDescription = produto.nome,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)),
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    serverUrl = PollingService.getServerUrl(),
                    placeholderIcon = {
                        Icon(
                            Icons.Default.Inventory,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = catColor
                        )
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Info do produto
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    produto.nome,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                produto.descricao?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Badge de categoria
                    if (categoria != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = catColor.copy(alpha = 0.15f),
                            modifier = Modifier.height(18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(catColor))
                                Text(categoria.nome, fontSize = 10.sp, color = catColor, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                    Text(
                        "Estoque: ${produto.estoqueFormatado()}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (vendidos > 0) {
                    Text(
                        "$vendidos vendido${if (vendidos != 1) "s" else ""} hoje",
                        style = MaterialTheme.typography.labelSmall,
                        color = primaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Preço e ações
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    produto.precoFormatado(),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor
                )
                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Edit, "Editar", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Delete, "Excluir", modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                    }
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
    val context = LocalContext.current

    // Launcher para selecionar imagem da galeria
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream = context.contentResolver.openInputStream(it)
                val bytes = inputStream?.readBytes()
                inputStream?.close()
                if (bytes != null) {
                    val base64 = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                    val mimeType = context.contentResolver.getType(it) ?: "image/jpeg"
                    imagem = "data:$mimeType;base64,$base64"
                }
            } catch (e: Exception) {
                android.util.Log.e("ProdutoDialog", "Erro ao converter imagem: ${e.message}")
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val isEditing = produto.id != 0L

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Icon(
                            if (isEditing) Icons.Default.Edit else Icons.Default.AddCircle,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            if (isEditing) "Editar Produto" else "Novo Produto",
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onCancelar) {
                        Icon(Icons.Default.Close, "Fechar", modifier = Modifier.size(22.dp))
                    }
                },
                actions = {
                    FilledTonalButton(
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
                        modifier = Modifier.padding(end = 8.dp).height(36.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = Color.White.copy(alpha = 0.2f),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Salvar", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Salvar", fontWeight = FontWeight.Bold, fontSize = 13.sp)
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
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Linha 1: Nome + Preço
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedTextFieldWithCustomKeyboard(
                    value = nome,
                    onValueChange = { nome = it },
                    label = "Nome *",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.weight(1f)
                )
                PrecoTextField(
                    value = preco,
                    onValueChange = { preco = it },
                    modifier = Modifier.width(120.dp)
                )
            }

            // Linha 2: Estoque + Unidade + Código de Barras + Auto
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextFieldWithCustomKeyboard(
                    value = estoque,
                    onValueChange = { estoque = it },
                    label = "Estq",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                    modifier = Modifier.weight(0.25f),
                    allowDecimal = false
                )
                OutlinedTextFieldWithCustomKeyboard(
                    value = unidade,
                    onValueChange = { unidade = it.uppercase() },
                    label = "Und",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.weight(0.15f),
                    maxLength = 3
                )
                OutlinedTextFieldWithCustomKeyboard(
                    value = codigoBarras,
                    onValueChange = { codigoBarras = it },
                    label = "Cód. Barras",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                    modifier = Modifier.weight(0.45f),
                    maxLength = 13,
                    allowDecimal = false
                )
                FilledTonalButton(
                    onClick = { codigoBarras = viewModel.gerarCodigoBarrasAutomatico() },
                    modifier = Modifier.height(52.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = primaryColor.copy(alpha = 0.1f)),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp), tint = primaryColor)
                    Spacer(Modifier.width(3.dp))
                    Text("Auto", color = primaryColor, fontWeight = FontWeight.Medium, fontSize = 12.sp, maxLines = 1)
                }
            }

            // Linha 3: Descrição
            OutlinedTextFieldWithCustomKeyboard(
                value = descricao,
                onValueChange = { descricao = it },
                label = "Descrição (opcional)",
                keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                modifier = Modifier.fillMaxWidth()
            )

            // Linha 4: Categoria chips
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cat:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Medium)
                FilterChip(
                    selected = categoriaId == null,
                    onClick = { categoriaId = null },
                    label = { Text("Nenhuma", fontSize = 11.sp) },
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.height(28.dp),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = primaryColor,
                        selectedLabelColor = Color.White
                    )
                )
                categorias.forEach { cat ->
                    val catColor = try {
                        cat.cor?.let { Color(android.graphics.Color.parseColor(it)) } ?: primaryColor
                    } catch (_: Exception) { primaryColor }
                    FilterChip(
                        selected = categoriaId == cat.id,
                        onClick = { categoriaId = cat.id },
                        label = { Text(cat.nome, fontSize = 11.sp) },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.height(28.dp),
                        leadingIcon = {
                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(catColor))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = catColor,
                            selectedLabelColor = Color.White,
                            selectedLeadingIconColor = Color.White
                        )
                    )
                }
            }

            // Linha 5: Imagem (compacta)
            Row(
                modifier = Modifier.fillMaxWidth().weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Preview ou placeholder
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (imagem != null) {
                        com.seucaixa.caixacombo.ui.components.ProdutoImagem(
                            imagem = imagem,
                            contentDescription = "Preview",
                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                            serverUrl = PollingService.getServerUrl()
                        )
                        IconButton(
                            onClick = { imagem = null },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .size(20.dp)
                                .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                        ) {
                            Icon(Icons.Default.Close, "Remover", tint = Color.White, modifier = Modifier.size(12.dp))
                        }
                    } else {
                        Icon(Icons.Outlined.AddPhotoAlternate, null, modifier = Modifier.size(28.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                    }
                }
                // Botões de imagem
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    OutlinedButton(
                        onClick = { imagePickerLauncher.launch("image/*") },
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                        contentPadding = PaddingValues(horizontal = 10.dp)
                    ) {
                        Icon(Icons.Outlined.Image, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text(if (imagem != null) "Trocar" else "Adicionar", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    if (imagem != null) {
                        TextButton(
                            onClick = { imagem = null },
                            modifier = Modifier.height(28.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Outlined.Delete, null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.width(4.dp))
                            Text("Remover", fontSize = 11.sp, color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(icon: ImageVector, title: String, color: Color) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
        Text(title, style = MaterialTheme.typography.titleSmall, color = color, fontWeight = FontWeight.Bold)
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
fun CategoriasTabContent(
    categorias: List<Categoria>,
    onAdicionar: (Categoria) -> Unit,
    onExcluir: (Categoria) -> Unit
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 12.dp, vertical = 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Card: Nova Categoria
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.06f))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.AddCircle, null, tint = primaryColor, modifier = Modifier.size(20.dp))
                    Text("Nova Categoria", style = MaterialTheme.typography.titleSmall, color = primaryColor, fontWeight = FontWeight.Bold)
                }
                OutlinedTextFieldWithCustomKeyboard(
                    value = novaCategoriaNome,
                    onValueChange = { novaCategoriaNome = it },
                    label = "Nome da categoria *",
                    keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Cor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    coresPredefinidas.forEach { (hex, label) ->
                        val selected = novaCategoriaCor == hex
                        FilterChip(
                            selected = selected,
                            onClick = { novaCategoriaCor = hex },
                            label = { Text(label, fontSize = 11.sp) },
                            shape = RoundedCornerShape(20.dp),
                            leadingIcon = {
                                Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(android.graphics.Color.parseColor(hex)),
                                selectedLabelColor = Color.White,
                                selectedLeadingIconColor = Color.White
                            )
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextFieldWithCustomKeyboard(
                        value = novaCategoriaOrdem,
                        onValueChange = { novaCategoriaOrdem = it },
                        label = "Ordem",
                        keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                        modifier = Modifier.width(100.dp),
                        allowDecimal = false
                    )
                    Spacer(Modifier.weight(1f))
                    Button(
                        onClick = {
                            if (novaCategoriaNome.isNotBlank()) {
                                onAdicionar(Categoria(nome = novaCategoriaNome, cor = novaCategoriaCor, ordem = novaCategoriaOrdem.toIntOrNull() ?: 0))
                                novaCategoriaNome = ""
                                novaCategoriaCor = "#2196F3"
                                novaCategoriaOrdem = "0"
                            }
                        },
                        enabled = novaCategoriaNome.isNotBlank(),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Adicionar", fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // Header: Categorias Existentes
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(top = 4.dp, start = 4.dp)
        ) {
            Icon(Icons.Outlined.Folder, null, tint = primaryColor, modifier = Modifier.size(18.dp))
            Text(
                "${categorias.size} categoria${if (categorias.size != 1) "s" else ""}",
                style = MaterialTheme.typography.titleSmall,
                color = primaryColor,
                fontWeight = FontWeight.Bold
            )
        }

        // Lista de categorias
        if (categorias.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Outlined.FolderOff, null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Spacer(Modifier.height(8.dp))
                    Text("Nenhuma categoria cadastrada", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        categorias.sortedBy { it.ordem }.forEach { categoria ->
            val catColor = try {
                categoria.cor?.let { Color(android.graphics.Color.parseColor(it)) } ?: primaryColor
            } catch (_: Exception) { primaryColor }

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (editandoCategoria?.id == categoria.id)
                        catColor.copy(alpha = 0.08f)
                    else
                        MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = if (editandoCategoria?.id == categoria.id) 2.dp else 0.dp)
            ) {
                if (editandoCategoria?.id == categoria.id) {
                    // Modo edição
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextFieldWithCustomKeyboard(
                            value = editandoNome,
                            onValueChange = { editandoNome = it },
                            label = "Nome",
                            keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.ALPHANUMERIC,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text("Cor", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            coresPredefinidas.forEach { (hex, label) ->
                                FilterChip(
                                    selected = editandoCor == hex,
                                    onClick = { editandoCor = hex },
                                    label = { Text(label, fontSize = 11.sp) },
                                    shape = RoundedCornerShape(20.dp),
                                    leadingIcon = {
                                        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(hex))))
                                    },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = Color(android.graphics.Color.parseColor(hex)),
                                        selectedLabelColor = Color.White,
                                        selectedLeadingIconColor = Color.White
                                    )
                                )
                            }
                        }
                        OutlinedTextFieldWithCustomKeyboard(
                            value = editandoOrdem,
                            onValueChange = { editandoOrdem = it },
                            label = "Ordem",
                            keyboardType = com.seucaixa.caixacombo.ui.components.KeyboardType.NUMERIC,
                            modifier = Modifier.width(100.dp),
                            allowDecimal = false
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    onAdicionar(categoria.copy(nome = editandoNome, cor = editandoCor, ordem = editandoOrdem.toIntOrNull() ?: 0))
                                    editandoCategoria = null
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = catColor)
                            ) {
                                Icon(Icons.Default.Check, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Salvar")
                            }
                            OutlinedButton(
                                onClick = { editandoCategoria = null },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Cancelar")
                            }
                        }
                    }
                } else {
                    // Modo visualização
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(catColor.copy(alpha = 0.2f), catColor.copy(alpha = 0.08f))
                                        )
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Folder, null, tint = catColor, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text(categoria.nome, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyLarge)
                                if (categoria.ordem > 0) {
                                    Text(
                                        "Ordem: ${categoria.ordem}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                        Row {
                            IconButton(onClick = {
                                editandoCategoria = categoria
                                editandoNome = categoria.nome
                                editandoCor = categoria.cor ?: "#2196F3"
                                editandoOrdem = categoria.ordem.toString()
                            }) {
                                Icon(Icons.Outlined.Edit, "Editar", tint = primaryColor)
                            }
                            IconButton(onClick = { onExcluir(categoria) }) {
                                Icon(Icons.Outlined.Delete, "Excluir", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(80.dp))
    }
}
