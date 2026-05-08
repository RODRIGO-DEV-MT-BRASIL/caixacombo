package com.seucaixa.caixacombo.ui.screens.configuracao

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Print
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
import com.seucaixa.caixacombo.R
import androidx.lifecycle.viewmodel.compose.viewModel
import com.seucaixa.caixacombo.ui.viewmodel.ConfiguracaoImpressaoViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguracaoImpressaoScreen(
    viewModel: ConfiguracaoImpressaoViewModel = viewModel(),
    onBack: () -> Unit
) {
    val configuracao by viewModel.configuracao.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    var selectedTabIndex by remember { mutableStateOf(0) }

    val tabs = listOf("Rodapé", "Cabeçalho")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuração de Impressão") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(padding)
        ) {
            // Mostrar erro se houver
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            TabRow(selectedTabIndex = selectedTabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = { Text(title) }
                    )
                }
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTabIndex) {
                    0 -> {
                        // Rodapé
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Rodapé",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha1 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha1,
                                    label = { Text("Linha 1") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha2 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha2,
                                    label = { Text("Linha 2") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha3 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha3,
                                    label = { Text("Linha 3") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.rodapeLinha4 ?: "",
                                    onValueChange = viewModel::updateRodapeLinha4,
                                    label = { Text("Linha 4") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                            }
                        }
                    }
                    1 -> {
                        // Cabeçalho
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Cabeçalho",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.titulo ?: "",
                                    onValueChange = viewModel::updateTitulo,
                                    label = { Text("Título (ex: Quintal Bar)") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = configuracao?.cnpj ?: "",
                                        onValueChange = viewModel::updateCnpj,
                                        label = { Text("CNPJ") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    
                                    OutlinedTextField(
                                        value = configuracao?.inscricaoEstadual ?: "",
                                        onValueChange = viewModel::updateInscricaoEstadual,
                                        label = { Text("IE") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                                
                                OutlinedTextField(
                                    value = configuracao?.telefone ?: "",
                                    onValueChange = viewModel::updateTelefone,
                                    label = { Text("Telefone") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.email ?: "",
                                    onValueChange = viewModel::updateEmail,
                                    label = { Text("Email") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.endereco ?: "",
                                    onValueChange = viewModel::updateEndereco,
                                    label = { Text(stringResource(R.string.endereco)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.cidade ?: "",
                                    onValueChange = viewModel::updateCidade,
                                    label = { Text(stringResource(R.string.cidade_uf)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
                                )
                                
                                OutlinedTextField(
                                    value = configuracao?.cep ?: "",
                                    onValueChange = viewModel::updateCep,
                                    label = { Text(stringResource(R.string.cep)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        viewModel.clearError()
                        viewModel.salvarConfiguracao(
                            onSuccess = { onBack() },
                            onError = { error -> /* erro já é tratado no ViewModel */ }
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading
                ) {
                    if (isLoading) {
                        Text("Salvando...")
                    } else {
                        Icon(Icons.Default.Print, null)
                        Spacer(modifier = Modifier.padding(8.dp))
                        Text("Salvar Configuração")
                    }
                }
            }
        }
    }
}
