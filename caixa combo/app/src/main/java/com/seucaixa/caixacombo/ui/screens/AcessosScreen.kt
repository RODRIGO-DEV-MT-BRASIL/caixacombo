package com.seucaixa.caixacombo.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.model.CargoUsuario
import com.seucaixa.caixacombo.data.model.StatusVenda
import com.seucaixa.caixacombo.data.model.Usuario
import com.seucaixa.caixacombo.data.model.Venda
import com.seucaixa.caixacombo.service.StoneDeeplinkService
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AcessosScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val dao = remember { AppDatabase.getDatabase(context).usuarioDao() }
    val vendaDao = remember { AppDatabase.getDatabase(context).vendaDao() }
    val scope = rememberCoroutineScope()
    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", android.content.Context.MODE_PRIVATE) }

    val primaryColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt())))
    }
    val backgroundColor by remember {
        mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt())))
    }

    val usuarios by dao.getAllUsuarios().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingUsuario by remember { mutableStateOf<Usuario?>(null) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Auditoria", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    if (selectedTab == 0) {
                        IconButton(onClick = { showAddDialog = true }) {
                            Icon(Icons.Default.PersonAdd, contentDescription = "Adicionar")
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
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab, containerColor = MaterialTheme.colorScheme.surface, contentColor = primaryColor) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }, text = { Text("Acessos") }, icon = { Icon(Icons.Default.People, null, modifier = Modifier.size(18.dp)) })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }, text = { Text("Reimprimir") }, icon = { Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp)) })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, text = { Text("Cancelar") }, icon = { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp)) })
            }
            when (selectedTab) {
                0 -> AcessosContent(usuarios, primaryColor, backgroundColor, onEdit = { editingUsuario = it }, onToggleAtivo = { scope.launch { dao.update(it.copy(ativo = !it.ativo)) } }, onDelete = { scope.launch { dao.delete(it) } })
                1 -> ReimpressaoTab(vendaDao, primaryColor, context as android.app.Activity)
                2 -> CancelamentoTab(vendaDao, primaryColor, context as android.app.Activity, scope)
            }
        }
    }

    if (showAddDialog) {
        UsuarioDialog(
            usuario = null,
            primaryColor = primaryColor,
            onDismiss = { showAddDialog = false },
            onSave = { novo ->
                scope.launch {
                    dao.insert(novo)
                    showAddDialog = false
                }
            }
        )
    }

    if (editingUsuario != null) {
        UsuarioDialog(
            usuario = editingUsuario,
            primaryColor = primaryColor,
            onDismiss = { editingUsuario = null },
            onSave = { editado ->
                scope.launch {
                    dao.update(editado)
                    editingUsuario = null
                }
            }
        )
    }
}

@Composable
private fun UsuarioCardCompact(
    usuario: Usuario,
    onEdit: () -> Unit,
    onToggleAtivo: () -> Unit,
    onDelete: () -> Unit
) {
    val cargoColor = when (usuario.cargo) {
        CargoUsuario.ADMIN -> Color(0xFFE53935)
        CargoUsuario.GERENTE -> Color(0xFFFB8C00)
        CargoUsuario.FUNCIONARIO -> Color(0xFF43A047)
    }
    val cargoLabel = when (usuario.cargo) {
        CargoUsuario.ADMIN -> "ADM"
        CargoUsuario.GERENTE -> "GER"
        CargoUsuario.FUNCIONARIO -> "FUN"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (usuario.ativo) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar com inicial
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(cargoColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    usuario.nome.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = cargoColor
                )
            }
            Spacer(modifier = Modifier.width(8.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        usuario.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (usuario.ativo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(3.dp),
                        color = cargoColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            cargoLabel,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = cargoColor
                        )
                    }
                    if (!usuario.ativo) {
                        Spacer(modifier = Modifier.width(3.dp))
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "OFF",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                if (usuario.cpf.isNotEmpty() || usuario.telefone.isNotEmpty()) {
                    Text(
                        listOfNotNull(
                            if (usuario.cpf.isNotEmpty()) "CPF: ${usuario.cpf}" else null,
                            if (usuario.telefone.isNotEmpty()) "Tel: ${usuario.telefone}" else null
                        ).joinToString(" · "),
                        fontSize = 10.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions compactas
            IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onToggleAtivo, modifier = Modifier.size(28.dp)) {
                Icon(
                    if (usuario.ativo) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, modifier = Modifier.size(16.dp),
                    tint = if (usuario.ativo) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF43A047)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UsuarioCard(
    usuario: Usuario,
    onEdit: () -> Unit,
    onToggleAtivo: () -> Unit,
    onDelete: () -> Unit
) {
    val cargoColor = when (usuario.cargo) {
        CargoUsuario.ADMIN -> Color(0xFFE53935)
        CargoUsuario.GERENTE -> Color(0xFFFB8C00)
        CargoUsuario.FUNCIONARIO -> Color(0xFF43A047)
    }
    val cargoLabel = when (usuario.cargo) {
        CargoUsuario.ADMIN -> "ADMIN"
        CargoUsuario.GERENTE -> "GERENTE"
        CargoUsuario.FUNCIONARIO -> "FUNCIONÁRIO"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (usuario.ativo) MaterialTheme.colorScheme.surface
            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar com inicial
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(cargoColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    usuario.nome.take(1).uppercase(),
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = cargoColor
                )
            }
            Spacer(modifier = Modifier.width(12.dp))

            // Info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        usuario.nome,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (usuario.ativo) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = cargoColor.copy(alpha = 0.15f)
                    ) {
                        Text(
                            cargoLabel,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = cargoColor
                        )
                    }
                    if (!usuario.ativo) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                        ) {
                            Text(
                                "INATIVO",
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (usuario.cpf.isNotEmpty()) {
                        Text("CPF: ${usuario.cpf}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (usuario.telefone.isNotEmpty()) {
                        Text("Tel: ${usuario.telefone}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // Actions
            IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Edit, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
            }
            IconButton(onClick = onToggleAtivo, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (usuario.ativo) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    null, modifier = Modifier.size(20.dp),
                    tint = if (usuario.ativo) MaterialTheme.colorScheme.onSurfaceVariant else Color(0xFF43A047)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(Icons.Default.Delete, null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun UsuarioDialog(
    usuario: Usuario?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (Usuario) -> Unit
) {
    val isEditing = usuario != null

    var nome by remember { mutableStateOf(usuario?.nome ?: "") }
    var codigo by remember { mutableStateOf(usuario?.codigo ?: "") }
    var cpf by remember { mutableStateOf(usuario?.cpf ?: "") }
    var telefone by remember { mutableStateOf(usuario?.telefone ?: "") }
    var email by remember { mutableStateOf(usuario?.email ?: "") }
    var cargo by remember { mutableStateOf(usuario?.cargo ?: CargoUsuario.FUNCIONARIO) }

    var permVender by remember { mutableStateOf(usuario?.permVender ?: true) }
    var permCaixa by remember { mutableStateOf(usuario?.permCaixa ?: false) }
    var permProdutos by remember { mutableStateOf(usuario?.permProdutos ?: false) }
    var permVendas by remember { mutableStateOf(usuario?.permVendas ?: false) }
    var permRelatorios by remember { mutableStateOf(usuario?.permRelatorios ?: false) }
    var permConfiguracoes by remember { mutableStateOf(usuario?.permConfiguracoes ?: false) }
    var permSangria by remember { mutableStateOf(usuario?.permSangria ?: false) }
    var permSuprimento by remember { mutableStateOf(usuario?.permSuprimento ?: false) }
    var permFechamento by remember { mutableStateOf(usuario?.permFechamento ?: false) }
    var permAcessos by remember { mutableStateOf(usuario?.permAcessos ?: false) }

    fun applyCargoPreset(newCargo: CargoUsuario) {
        cargo = newCargo
        when (newCargo) {
            CargoUsuario.FUNCIONARIO -> {
                permVender = true; permCaixa = false; permProdutos = false
                permVendas = false; permRelatorios = false; permConfiguracoes = false
                permSangria = false; permSuprimento = false; permFechamento = false
                permAcessos = false
            }
            CargoUsuario.GERENTE -> {
                permVender = true; permCaixa = true; permProdutos = true
                permVendas = true; permRelatorios = true; permConfiguracoes = false
                permSangria = true; permSuprimento = true; permFechamento = true
                permAcessos = false
            }
            CargoUsuario.ADMIN -> {
                permVender = true; permCaixa = true; permProdutos = true
                permVendas = true; permRelatorios = true; permConfiguracoes = true
                permSangria = true; permSuprimento = true; permFechamento = true
                permAcessos = true
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .widthIn(min = 640.dp, max = 800.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isEditing) Icons.Default.Edit else Icons.Default.PersonAdd,
                        null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        if (isEditing) "Editar Usuário" else "Novo Usuário",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Duas colunas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Coluna esquerda: Dados + Cargo
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Dados Pessoais")

                        OutlinedTextField(
                            value = nome,
                            onValueChange = { nome = it },
                            label = { Text("Nome") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = codigo,
                            onValueChange = { codigo = it },
                            label = { Text("Código (PIN)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Lock, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = cpf,
                            onValueChange = { cpf = it },
                            label = { Text("CPF") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = telefone,
                            onValueChange = { telefone = it },
                            label = { Text("Telefone") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Email") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Cargo
                        SectionLabel("Cargo")
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            CargoUsuario.values().forEach { c ->
                                val selected = cargo == c
                                val color = when (c) {
                                    CargoUsuario.ADMIN -> Color(0xFFE53935)
                                    CargoUsuario.GERENTE -> Color(0xFFFB8C00)
                                    CargoUsuario.FUNCIONARIO -> Color(0xFF43A047)
                                }
                                FilterChip(
                                    selected = selected,
                                    onClick = { applyCargoPreset(c) },
                                    label = { Text(c.name, fontSize = 11.sp, fontWeight = FontWeight.Medium) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = color.copy(alpha = 0.15f),
                                        selectedLabelColor = color
                                    ),
                                    border = FilterChipDefaults.filterChipBorder(
                                        borderColor = if (selected) color else MaterialTheme.colorScheme.outline,
                                        borderWidth = if (selected) 2.dp else 1.dp,
                                        enabled = true,
                                        selected = selected,
                                        selectedBorderColor = color
                                    )
                                )
                            }
                        }
                    }

                    // Coluna direita: Permissões
                    Column(modifier = Modifier.weight(1f)) {
                        SectionLabel("Permissões")

                        val permissoes = listOf(
                            Triple("Vender", permVender, { v: Boolean -> permVender = v }),
                            Triple("Caixa", permCaixa, { v: Boolean -> permCaixa = v }),
                            Triple("Produtos", permProdutos, { v: Boolean -> permProdutos = v }),
                            Triple("Vendas", permVendas, { v: Boolean -> permVendas = v }),
                            Triple("Relatórios", permRelatorios, { v: Boolean -> permRelatorios = v }),
                            Triple("Configurações", permConfiguracoes, { v: Boolean -> permConfiguracoes = v }),
                            Triple("Sangria", permSangria, { v: Boolean -> permSangria = v }),
                            Triple("Suprimento", permSuprimento, { v: Boolean -> permSuprimento = v }),
                            Triple("Fechamento", permFechamento, { v: Boolean -> permFechamento = v }),
                            Triple("Acessos", permAcessos, { v: Boolean -> permAcessos = v })
                        )
                        permissoes.forEach { (label, checked, onCheck) ->
                            PermissaoChip(
                                label = label,
                                checked = checked,
                                onCheckedChange = onCheck,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botões
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancelar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nome.isBlank()) return@Button
                            val u = Usuario(
                                id = usuario?.id ?: 0,
                                nome = nome,
                                codigo = codigo,
                                cpf = cpf,
                                telefone = telefone,
                                email = email,
                                cargo = cargo,
                                ativo = usuario?.ativo ?: true,
                                dataCriacao = usuario?.dataCriacao ?: System.currentTimeMillis(),
                                permVender = permVender,
                                permCaixa = permCaixa,
                                permProdutos = permProdutos,
                                permVendas = permVendas,
                                permRelatorios = permRelatorios,
                                permConfiguracoes = permConfiguracoes,
                                permSangria = permSangria,
                                permSuprimento = permSuprimento,
                                permFechamento = permFechamento,
                                permAcessos = permAcessos
                            )
                            onSave(u)
                        },
                        enabled = nome.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        )
                    ) {
                        Icon(
                            if (isEditing) Icons.Default.Save else Icons.Default.Add,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Salvar" else "Adicionar")
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 4.dp)
    ) {
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        Text(
            "  $text  ",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        HorizontalDivider(
            modifier = Modifier.weight(1f),
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
    }
}

@Composable
private fun PermissaoChip(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = if (checked)
        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    else
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)

    val borderColor = if (checked)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.outlineVariant

    val textColor = if (checked)
        MaterialTheme.colorScheme.primary
    else
        MaterialTheme.colorScheme.onSurfaceVariant

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .border(1.dp, borderColor, RoundedCornerShape(8.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Checkbox(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.size(20.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary
            )
        )
        Text(label, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = textColor)
    }
}

@Composable
private fun AcessosContent(
    usuarios: List<Usuario>,
    primaryColor: Color,
    backgroundColor: Color,
    onEdit: (Usuario) -> Unit,
    onToggleAtivo: (Usuario) -> Unit,
    onDelete: (Usuario) -> Unit
) {
    if (usuarios.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.People, null, modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(8.dp))
                Text("Nenhum usuário cadastrado", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                Spacer(modifier = Modifier.height(4.dp))
                Text("Toque em + para adicionar", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            }
        }
    } else {
        val funcionarios = usuarios.filter { it.cargo == CargoUsuario.FUNCIONARIO }
        val gerentesAdmin = usuarios.filter { it.cargo == CargoUsuario.GERENTE || it.cargo == CargoUsuario.ADMIN }
        val configuration = LocalConfiguration.current
        val isSmallScreen = configuration.screenWidthDp < 600

        if (isSmallScreen) {
            Column(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp).verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(16.dp), tint = Color(0xFFE53935))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Admin/Gerente (${gerentesAdmin.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFFE53935))
                }
                gerentesAdmin.forEach { usuario ->
                    UsuarioCardCompact(usuario, onEdit = { onEdit(usuario) }, onToggleAtivo = { onToggleAtivo(usuario) }, onDelete = { onDelete(usuario) })
                    Spacer(modifier = Modifier.height(4.dp))
                }
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = Color(0xFF43A047))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Funcionários (${funcionarios.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF43A047))
                }
                if (funcionarios.isEmpty()) { Text("Nenhum funcionário", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) }
                funcionarios.forEach { usuario ->
                    UsuarioCardCompact(usuario, onEdit = { onEdit(usuario) }, onToggleAtivo = { onToggleAtivo(usuario) }, onDelete = { onDelete(usuario) })
                    Spacer(modifier = Modifier.height(4.dp))
                }
            }
        } else {
            Row(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp), tint = Color(0xFF43A047))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Funcionários (${funcionarios.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF43A047))
                    }
                    if (funcionarios.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Nenhum funcionário", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } }
                    else { funcionarios.forEach { usuario -> UsuarioCard(usuario, onEdit = { onEdit(usuario) }, onToggleAtivo = { onToggleAtivo(usuario) }, onDelete = { onDelete(usuario) }); Spacer(modifier = Modifier.height(4.dp)) } }
                }
                VerticalDivider(modifier = Modifier.fillMaxHeight().padding(vertical = 8.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant)
                Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)) {
                        Icon(Icons.Default.AdminPanelSettings, null, modifier = Modifier.size(18.dp), tint = Color(0xFFE53935))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Admin / Gerente (${gerentesAdmin.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFFE53935))
                    }
                    if (gerentesAdmin.isEmpty()) { Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { Text("Nenhum admin/gerente", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)) } }
                    else { gerentesAdmin.forEach { usuario -> UsuarioCard(usuario, onEdit = { onEdit(usuario) }, onToggleAtivo = { onToggleAtivo(usuario) }, onDelete = { onDelete(usuario) }); Spacer(modifier = Modifier.height(4.dp)) } }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReimpressaoTab(
    vendaDao: com.seucaixa.caixacombo.data.database.VendaDao,
    primaryColor: Color,
    activity: android.app.Activity
) {
    val vendas by vendaDao.getAllVendas().collectAsState(initial = emptyList())
    val vendasReimprimiveis = vendas.filter { !it.stoneAtk.isNullOrEmpty() && it.status != StatusVenda.CANCELADA }
    var confirmarReimpressao by remember { mutableStateOf<Venda?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (vendasReimprimiveis.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Print, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhuma venda disponível para reimpressão", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("Vendas com pagamento Stone possuem ATK para reimpressão", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(vendasReimprimiveis) { venda ->
                    VendaAuditoriaCard(venda, primaryColor, Icons.Default.Print, "Reimprimir", Color(0xFF2196F3)) { confirmarReimpressao = venda }
                }
            }
        }
    }

    if (confirmarReimpressao != null) {
        AlertDialog(
            onDismissRequest = { confirmarReimpressao = null },
            title = { Text("Reimprimir Venda") },
            text = { Column { Text("Confirma a reimpressão da venda #${confirmarReimpressao!!.id.toString().takeLast(6)}?"); Spacer(modifier = Modifier.height(4.dp)); Text("Total: R$ ${"%.2f".format(confirmarReimpressao!!.total)}", fontWeight = FontWeight.Bold); Text("ATK: ${confirmarReimpressao!!.stoneAtk}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { Button(onClick = { StoneDeeplinkService.sendReprint(activity, confirmarReimpressao!!.stoneAtk); confirmarReimpressao = null }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3))) { Icon(Icons.Default.Print, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Reimprimir") } },
            dismissButton = { OutlinedButton(onClick = { confirmarReimpressao = null }) { Text("Cancelar") } }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CancelamentoTab(
    vendaDao: com.seucaixa.caixacombo.data.database.VendaDao,
    primaryColor: Color,
    activity: android.app.Activity,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val vendas by vendaDao.getAllVendas().collectAsState(initial = emptyList())
    val vendasCancelaveis = vendas.filter { !it.stoneAtk.isNullOrEmpty() && it.status != StatusVenda.CANCELADA }
    var confirmarCancelamento by remember { mutableStateOf<Venda?>(null) }

    Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
        if (vendasCancelaveis.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Cancel, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhuma venda disponível para cancelamento", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                    Text("Vendas com pagamento Stone e ATK podem ser canceladas", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(vendasCancelaveis) { venda ->
                    VendaAuditoriaCard(venda, primaryColor, Icons.Default.Cancel, "Cancelar", MaterialTheme.colorScheme.error) { confirmarCancelamento = venda }
                }
            }
        }
    }

    if (confirmarCancelamento != null) {
        AlertDialog(
            onDismissRequest = { confirmarCancelamento = null },
            title = { Text("Cancelar Venda", color = MaterialTheme.colorScheme.error) },
            text = { Column { Text("⚠️ Esta ação não pode ser desfeita!", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error); Spacer(modifier = Modifier.height(8.dp)); Text("Venda #${confirmarCancelamento!!.id.toString().takeLast(6)}"); Text("Total: R$ ${"%.2f".format(confirmarCancelamento!!.total)}", fontWeight = FontWeight.Bold); Text("ATK: ${confirmarCancelamento!!.stoneAtk}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant) } },
            confirmButton = { Button(onClick = { val amount = Math.round(confirmarCancelamento!!.total * 100); StoneDeeplinkService.sendCancel(activity, confirmarCancelamento!!.stoneAtk!!, amount.toLong(), false); scope.launch { vendaDao.updateStatus(confirmarCancelamento!!.id, StatusVenda.CANCELADA) }; confirmarCancelamento = null }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Icon(Icons.Default.Cancel, null, modifier = Modifier.size(18.dp)); Spacer(modifier = Modifier.width(4.dp)); Text("Confirmar Cancelamento") } },
            dismissButton = { OutlinedButton(onClick = { confirmarCancelamento = null }) { Text("Voltar") } }
        )
    }
}

@Composable
private fun VendaAuditoriaCard(
    venda: Venda,
    primaryColor: Color,
    actionIcon: androidx.compose.ui.graphics.vector.ImageVector,
    actionLabel: String,
    actionColor: Color,
    onAction: () -> Unit
) {
    val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    val dataFormatada = dateFormat.format(Date(venda.dataHora))

    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(8.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("#${venda.id.toString().takeLast(6)}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("R$ ${"%.2f".format(venda.total)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = primaryColor)
                }
                Text(dataFormatada, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (!venda.stoneAtk.isNullOrEmpty()) { Text("ATK: ${venda.stoneAtk}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) }
            }
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = actionColor), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
                Icon(actionIcon, null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(actionLabel, fontSize = 12.sp)
            }
        }
    }
}
