package com.seucaixa.caixacombo.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.seucaixa.caixacombo.data.database.AppDatabase
import com.seucaixa.caixacombo.data.model.Cliente
import com.seucaixa.caixacombo.data.model.Empresa
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val clienteDao = remember { AppDatabase.getDatabase(context).clienteDao() }
    val empresaDao = remember { AppDatabase.getDatabase(context).empresaDao() }
    val scope = rememberCoroutineScope()

    val sharedPreferences = remember { context.getSharedPreferences("cores_sistema", android.content.Context.MODE_PRIVATE) }
    val primaryColor by remember { mutableStateOf(Color(sharedPreferences.getInt("primary_color", 0xFF6200EE.toInt()))) }
    val backgroundColor by remember { mutableStateOf(Color(sharedPreferences.getInt("background_color", 0xFFFFFBFE.toInt()))) }

    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cadastros", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
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
                .background(backgroundColor)
                .padding(padding)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = primaryColor.copy(alpha = 0.1f),
                contentColor = primaryColor
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Clientes", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.People, null, modifier = Modifier.size(20.dp)) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Empresa", fontWeight = FontWeight.Bold) },
                    icon = { Icon(Icons.Default.Business, null, modifier = Modifier.size(20.dp)) }
                )
            }

            when (selectedTab) {
                0 -> ClientesTab(clienteDao, primaryColor, backgroundColor, scope)
                1 -> EmpresaTab(empresaDao, primaryColor, backgroundColor, scope)
            }
        }
    }
}

@Composable
private fun ClientesTab(
    dao: com.seucaixa.caixacombo.data.database.ClienteDao,
    primaryColor: Color,
    backgroundColor: Color,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val clientes by dao.getAllClientes().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingCliente by remember { mutableStateOf<Cliente?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (clientes.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.People, null, modifier = Modifier.size(64.dp), tint = primaryColor.copy(alpha = 0.3f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Nenhum cliente cadastrado", color = Color.Gray.copy(alpha = 0.5f))
                    Text("Toque em + para adicionar", fontSize = 13.sp, color = Color.Gray.copy(alpha = 0.4f))
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(clientes, key = { it.id }) { cliente ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Avatar
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .background(primaryColor.copy(alpha = 0.15f), RoundedCornerShape(20.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    cliente.nome.take(1).uppercase(),
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp,
                                    color = primaryColor
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            // Info
                            Column(modifier = Modifier.weight(1f)) {
                                Text(cliente.nome, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                if (cliente.cpfCnpj.isNotEmpty()) {
                                    Text(cliente.cpfCnpj, fontSize = 12.sp, color = Color.Gray)
                                }
                                if (cliente.telefone.isNotEmpty()) {
                                    Text(cliente.telefone, fontSize = 12.sp, color = Color.Gray)
                                }
                            }
                            // Actions
                            IconButton(onClick = { editingCliente = cliente }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Edit, null, modifier = Modifier.size(18.dp), tint = primaryColor)
                            }
                            IconButton(
                                onClick = {
                                    scope.launch { dao.update(cliente.copy(ativo = !cliente.ativo)) }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    if (cliente.ativo) Icons.Default.ToggleOn else Icons.Default.ToggleOff,
                                    null, modifier = Modifier.size(22.dp),
                                    tint = if (cliente.ativo) Color(0xFF43A047) else Color.Gray
                                )
                            }
                            IconButton(onClick = {
                                scope.launch { dao.delete(cliente) }
                            }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Delete, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
            containerColor = primaryColor,
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, null)
        }
    }

    if (showAddDialog) {
        ClienteDialog(
            cliente = null,
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

    if (editingCliente != null) {
        ClienteDialog(
            cliente = editingCliente,
            primaryColor = primaryColor,
            onDismiss = { editingCliente = null },
            onSave = { editado ->
                scope.launch {
                    dao.update(editado)
                    editingCliente = null
                }
            }
        )
    }
}

@Composable
private fun ClienteDialog(
    cliente: Cliente?,
    primaryColor: Color,
    onDismiss: () -> Unit,
    onSave: (Cliente) -> Unit
) {
    val isEditing = cliente != null

    var nome by remember { mutableStateOf(cliente?.nome ?: "") }
    var cpfCnpj by remember { mutableStateOf(cliente?.cpfCnpj ?: "") }
    var telefone by remember { mutableStateOf(cliente?.telefone ?: "") }
    var email by remember { mutableStateOf(cliente?.email ?: "") }
    var endereco by remember { mutableStateOf(cliente?.endereco ?: "") }
    var cidade by remember { mutableStateOf(cliente?.cidade ?: "") }
    var cep by remember { mutableStateOf(cliente?.cep ?: "") }
    var observacao by remember { mutableStateOf(cliente?.observacao ?: "") }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier.widthIn(min = 480.dp, max = 640.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                // Header
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(if (isEditing) Icons.Default.Edit else Icons.Default.PersonAdd, null, tint = primaryColor, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(if (isEditing) "Editar Cliente" else "Novo Cliente", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
                Spacer(modifier = Modifier.height(16.dp))

                // Campos em duas colunas
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = nome, onValueChange = { nome = it }, label = { Text("Nome *") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Person, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = cpfCnpj, onValueChange = { cpfCnpj = it }, label = { Text("CPF/CNPJ") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = cep, onValueChange = { cep = it }, label = { Text("CEP") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.MarkunreadMailbox, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(value = observacao, onValueChange = { observacao = it }, label = { Text("Observação") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Notes, null, modifier = Modifier.size(18.dp)) })
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Botões
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancelar") }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (nome.isBlank()) return@Button
                            onSave(Cliente(
                                id = cliente?.id ?: 0,
                                nome = nome,
                                cpfCnpj = cpfCnpj,
                                telefone = telefone,
                                email = email,
                                endereco = endereco,
                                cidade = cidade,
                                cep = cep,
                                observacao = observacao,
                                ativo = cliente?.ativo ?: true,
                                dataCriacao = cliente?.dataCriacao ?: System.currentTimeMillis()
                            ))
                        },
                        enabled = nome.isNotBlank(),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                    ) {
                        Icon(if (isEditing) Icons.Default.Save else Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isEditing) "Salvar" else "Adicionar")
                    }
                }
            }
        }
    }
}

@Composable
private fun EmpresaTab(
    dao: com.seucaixa.caixacombo.data.database.EmpresaDao,
    primaryColor: Color,
    backgroundColor: Color,
    scope: kotlinx.coroutines.CoroutineScope
) {
    val empresa by dao.getEmpresa().collectAsState(initial = null)

    var razaoSocial by remember { mutableStateOf(empresa?.razaoSocial ?: "") }
    var nomeFantasia by remember { mutableStateOf(empresa?.nomeFantasia ?: "") }
    var cnpj by remember { mutableStateOf(empresa?.cnpj ?: "") }
    var inscricaoEstadual by remember { mutableStateOf(empresa?.inscricaoEstadual ?: "") }
    var telefone by remember { mutableStateOf(empresa?.telefone ?: "") }
    var email by remember { mutableStateOf(empresa?.email ?: "") }
    var endereco by remember { mutableStateOf(empresa?.endereco ?: "") }
    var cidade by remember { mutableStateOf(empresa?.cidade ?: "") }
    var cep by remember { mutableStateOf(empresa?.cep ?: "") }
    var estado by remember { mutableStateOf(empresa?.estado ?: "") }

    // Atualizar campos quando empresa carrega
    LaunchedEffect(empresa) {
        empresa?.let {
            razaoSocial = it.razaoSocial
            nomeFantasia = it.nomeFantasia
            cnpj = it.cnpj
            inscricaoEstadual = it.inscricaoEstadual
            telefone = it.telefone
            email = it.email
            endereco = it.endereco
            cidade = it.cidade
            cep = it.cep
            estado = it.estado
        }
    }

    var savedMsg by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Header
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Default.Business, null, tint = primaryColor, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Text("Dados da Empresa", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = primaryColor)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Campos em duas colunas
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = razaoSocial, onValueChange = { razaoSocial = it }, label = { Text("Razão Social") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Business, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = nomeFantasia, onValueChange = { nomeFantasia = it }, label = { Text("Nome Fantasia") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Store, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = cnpj, onValueChange = { cnpj = it }, label = { Text("CNPJ") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Badge, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = inscricaoEstadual, onValueChange = { inscricaoEstadual = it }, label = { Text("Inscrição Estadual") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Receipt, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = telefone, onValueChange = { telefone = it }, label = { Text("Telefone") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Phone, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone))
            }
            Column(modifier = Modifier.weight(1f)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Email, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = endereco, onValueChange = { endereco = it }, label = { Text("Endereço") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = cidade, onValueChange = { cidade = it }, label = { Text("Cidade") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.LocationCity, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = cep, onValueChange = { cep = it }, label = { Text("CEP") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.MarkunreadMailbox, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = estado, onValueChange = { estado = it }, label = { Text("Estado (UF)") }, singleLine = true, modifier = Modifier.fillMaxWidth(), leadingIcon = { Icon(Icons.Default.Map, null, modifier = Modifier.size(18.dp)) }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text))
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                scope.launch {
                    val emp = Empresa(
                        id = 1,
                        razaoSocial = razaoSocial,
                        nomeFantasia = nomeFantasia,
                        cnpj = cnpj,
                        inscricaoEstadual = inscricaoEstadual,
                        telefone = telefone,
                        email = email,
                        endereco = endereco,
                        cidade = cidade,
                        cep = cep,
                        estado = estado
                    )
                    dao.insert(emp)
                    savedMsg = true
                    kotlinx.coroutines.delay(2000)
                    savedMsg = false
                }
            },
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
        ) {
            Icon(Icons.Default.Save, null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Salvar Dados da Empresa", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }

        if (savedMsg) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF43A047), modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Dados salvos com sucesso!", color = Color(0xFF43A047), fontWeight = FontWeight.Medium)
            }
        }
    }
}
