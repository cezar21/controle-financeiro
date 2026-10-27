package br.com.cezar.controlefinanceiro

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF176B50)
private val Teal = Color(0xFF087F8C)
private val Purple = Color(0xFF6650A4)
private fun brl(value: Double) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

data class TransactionItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val amount: Double,
    val paymentType: String, // "Pix" ou "Cartão"
    val date: String,
    val profile: String // "PF" ou "PJ"
)

class MainActivity : ComponentActivity() {
    
    var exportCallback: ((Uri) -> Unit)? = null
    var importCallback: ((Uri) -> Unit)? = null

    private val createJsonFileLauncher = registerForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { exportCallback?.invoke(it) }
    }

    private val openJsonFileLauncher = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let { importCallback?.invoke(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { 
            MaterialTheme(colorScheme = lightColorScheme(primary = Green)) { 
                FinanceDashboard(
                    onExport = { createJsonFileLauncher.launch("backup_financeiro.json") },
                    onImport = { openJsonFileLauncher.launch(arrayOf("application/json", "*/*")) }
                ) 
            } 
        }
    }
}

private fun saveTransactions(context: Context, list: List<TransactionItem>) {
    val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    val array = JSONArray()
    list.forEach { item ->
        val obj = JSONObject().apply {
            put("id", item.id)
            put("title", item.title)
            put("amount", item.amount)
            put("paymentType", item.paymentType)
            put("date", item.date)
            put("profile", item.profile)
        }
        array.put(obj)
    }
    prefs.edit().putString("transactions_data", array.toString()).apply()
}

private fun loadTransactions(context: Context): List<TransactionItem> {
    val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    val jsonString = prefs.getString("transactions_data", null) ?: return emptyList()
    val list = mutableListOf<TransactionItem>()
    try {
        val array = JSONArray(jsonString)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            list.add(
                TransactionItem(
                    id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                    title = obj.getString("title"),
                    amount = obj.getDouble("amount"),
                    paymentType = obj.getString("paymentType"),
                    date = obj.getString("date"),
                    profile = obj.optString("profile", "PF")
                )
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return list
}

private fun saveProfileNames(context: Context, pfName: String, pjName: String) {
    val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    prefs.edit().putString("pf_name", pfName).putString("pj_name", pjName).apply()
}

private fun loadProfileNames(context: Context): Pair<String, String> {
    val prefs = context.getSharedPreferences("finance_prefs", Context.MODE_PRIVATE)
    val pf = prefs.getString("pf_name", "Pessoa Física") ?: "Pessoa Física"
    val pj = prefs.getString("pj_name", "Pessoa Jurídica") ?: "Pessoa Jurídica"
    return Pair(pf, pj)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FinanceDashboard(onExport: () -> Unit, onImport: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? MainActivity
    
    var pfName by remember { mutableStateOf("Pessoa Física") }
    var pjName by remember { mutableStateOf("Pessoa Jurídica") }
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }

    var selectedProfileKey by remember { mutableStateOf("PF") }
    
    var transactions by remember { mutableStateOf(emptyList<TransactionItem>()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilterPayment by remember { mutableStateOf<String?>(null) }
    
    // Ano inicial configurado para 2026 com dropdown centralizado
    var selectedYear by remember { mutableStateOf("2026") }
    var showYearMenu by remember { mutableStateOf(false) }
    val yearsList = listOf("2026", "2025", "2024", "2027", "2028")

    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())

    DisposableEffect(transactions, pfName, pjName) {
        activity?.exportCallback = { uri ->
            try {
                val jsonRoot = JSONObject()
                jsonRoot.put("pf_name", pfName)
                jsonRoot.put("pj_name", pjName)
                
                val array = JSONArray()
                transactions.forEach { item ->
                    val obj = JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title)
                        put("amount", item.amount)
                        put("paymentType", item.paymentType)
                        put("date", item.date)
                        put("profile", item.profile)
                    }
                    array.put(obj)
                }
                jsonRoot.put("transactions", array)

                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    outputStream.write(jsonRoot.toString(2).toByteArray())
                }
                Toast.makeText(context, "Backup exportado com sucesso!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao exportar: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }

        activity?.importCallback = { uri ->
            try {
                val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.reader().readText()
                } ?: ""
                
                val jsonRoot = JSONObject(jsonString)
                val newPf = jsonRoot.optString("pf_name", "Pessoa Física")
                val newPj = jsonRoot.optString("pj_name", "Pessoa Jurídica")
                
                val txArray = jsonRoot.getJSONArray("transactions")
                val newList = mutableListOf<TransactionItem>()
                for (i in 0 until txArray.length()) {
                    val obj = txArray.getJSONObject(i)
                    newList.add(
                        TransactionItem(
                            id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                            title = obj.getString("title"),
                            amount = obj.getDouble("amount"),
                            paymentType = obj.getString("paymentType"),
                            date = obj.getString("date"),
                            profile = obj.optString("profile", "PF")
                        )
                    )
                }
                
                pfName = newPf
                pjName = newPj
                transactions = newList
                saveProfileNames(context, pfName, pjName)
                saveTransactions(context, newList)
                
                Toast.makeText(context, "Backup importado com sucesso!", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(context, "Erro ao importar arquivo inválido.", Toast.LENGTH_LONG).show()
            }
        }
        onDispose {
            activity?.exportCallback = null
            activity?.importCallback = null
        }
    }

    LaunchedEffect(Unit) {
        val loadedNames = loadProfileNames(context)
        pfName = loadedNames.first
        pjName = loadedNames.second
        
        val loadedTxs = loadTransactions(context)
        if (loadedTxs.isEmpty()) {
            val initial = listOf(
                TransactionItem(title = "Mercado", amount = 185.50, paymentType = "Pix", date = currentDate, profile = "PF"),
                TransactionItem(title = "Internet", amount = 99.90, paymentType = "Cartão", date = currentDate, profile = "PF"),
                TransactionItem(title = "Fornecedor", amount = 450.00, paymentType = "Pix", date = currentDate, profile = "PJ")
            )
            transactions = initial
            saveTransactions(context, initial)
        } else {
            transactions = loadedTxs
        }
    }

    val profileTransactions = transactions.filter { 
        it.profile == selectedProfileKey && it.date.endsWith(selectedYear)
    }

    val totalYear = profileTransactions.sumOf { it.amount }
    val totalPix = profileTransactions.filter { it.paymentType == "Pix" }.sumOf { it.amount }
    val totalCard = profileTransactions.filter { it.paymentType == "Cartão" }.sumOf { it.amount }

    val displayedTransactions = if (selectedFilterPayment == null) {
        profileTransactions
    } else {
        profileTransactions.filter { it.paymentType == selectedFilterPayment }
    }

    val currentProfileName = if (selectedProfileKey == "PF") pfName else pjName

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA)),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Meu financeiro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Text("Controle seus gastos por perfil", color = Color.Gray)
                }
                
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showEditProfileDialog = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar nomes dos perfis", tint = Green)
                    }
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.Menu, contentDescription = "Menu de Backup", tint = Green)
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Exportar Dados (Backup)") },
                                onClick = { showMenu = false; onExport() }
                            )
                            DropdownMenuItem(
                                text = { Text("Importar Dados (Restaurar)") },
                                onClick = { showMenu = false; onImport() }
                            )
                        }
                    }
                }
            }
            
            Spacer(Modifier.height(14.dp))
            
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = selectedProfileKey == "PF",
                    onClick = { selectedProfileKey = "PF"; selectedFilterPayment = null },
                    shape = SegmentedButtonDefaults.itemShape(0, 2)
                ) { Text(pfName) }
                SegmentedButton(
                    selected = selectedProfileKey == "PJ",
                    onClick = { selectedProfileKey = "PJ"; selectedFilterPayment = null },
                    shape = SegmentedButtonDefaults.itemShape(1, 2)
                ) { Text(pjName) }
            }
        }

        // Card Total com Seletor de Ano Centralizado
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = Green, contentColor = Color.White),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    Modifier.padding(22.dp).fillMaxWidth(), 
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Total de despesas • $currentProfileName", style = MaterialTheme.typography.bodyMedium)
                    
                    // Botão Centralizado para escolher o Ano
                    Box {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White.copy(alpha = 0.2f),
                            modifier = Modifier.clickable { showYearMenu = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text("Ano: $selectedYear", fontWeight = FontWeight.Bold, color = Color.White)
                                Text("▼", style = MaterialTheme.typography.bodySmall, color = Color.White)
                            }
                        }

                        DropdownMenu(
                            expanded = showYearMenu,
                            onDismissRequest = { showYearMenu = false }
                        ) {
                            yearsList.forEach { yr ->
                                DropdownMenuItem(
                                    text = { Text(yr, fontWeight = if (selectedYear == yr) FontWeight.Bold else FontWeight.Normal) },
                                    onClick = {
                                        selectedYear = yr
                                        showYearMenu = false
                                    }
                                )
                            }
                        }
                    }
                    
                    Text(brl(totalYear), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Ano de $selectedYear", color = Color.White.copy(alpha = .85f))
                }
            }
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Gastos por meio de pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    if (selectedFilterPayment != null) {
                        TextButton(onClick = { selectedFilterPayment = null }) {
                            Text("Ver todos", color = Green)
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    SummaryCard(
                        title = "Pix",
                        amount = totalPix,
                        color = Teal,
                        isSelected = selectedFilterPayment == "Pix",
                        modifier = Modifier.weight(1f).clickable {
                            selectedFilterPayment = if (selectedFilterPayment == "Pix") null else "Pix"
                        }
                    )
                    SummaryCard(
                        title = "Cartão",
                        amount = totalCard,
                        color = Purple,
                        isSelected = selectedFilterPayment == "Cartão",
                        modifier = Modifier.weight(1f).clickable {
                            selectedFilterPayment = if (selectedFilterPayment == "Cartão") null else "Cartão"
                        }
                    )
                }
            }
        }

        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val titleList = if (selectedFilterPayment == null) "Transações ($selectedYear)" else "Transações via $selectedFilterPayment ($selectedYear)"
                Text(titleList, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showAddDialog = true }) { Text("+ Adicionar") }
            }
        }
        
        if (displayedTransactions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma transação registrada para $selectedYear.", color = Color.Gray)
                }
            }
        } else {
            items(displayedTransactions.size) { index ->
                val tx = displayedTransactions[index]
                Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(
                        Modifier.padding(16.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(tx.title, fontWeight = FontWeight.SemiBold)
                            Text("${tx.paymentType} • ${tx.date}", color = Color.Gray, style = MaterialTheme.typography.bodySmall)
                        }
                        Text("− ${brl(tx.amount)}", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                        IconButton(onClick = {
                            val newList = transactions.filterNot { it.id == tx.id }
                            transactions = newList
                            saveTransactions(context, newList)
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remover gasto", tint = Color.Red.copy(alpha = 0.7f))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var description by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        var paymentType by remember { mutableStateOf("Pix") }
        var date by remember { mutableStateOf(currentDate) }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nova despesa ($currentProfileName)") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Descrição") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Valor (R$)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = date,
                        onValueChange = { date = it },
                        label = { Text("Data (DD/MM/AAAA)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Text("Tipo de pagamento:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilterChip(
                            selected = paymentType == "Pix",
                            onClick = { paymentType = "Pix" },
                            label = { Text("Pix") },
                            leadingIcon = { RadioButton(selected = paymentType == "Pix", onClick = null) }
                        )
                        FilterChip(
                            selected = paymentType == "Cartão",
                            onClick = { paymentType = "Cartão" },
                            label = { Text("Cartão") },
                            leadingIcon = { RadioButton(selected = paymentType == "Cartão", onClick = null) }
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = amount.replace(',', '.').toDoubleOrNull()
                    if (description.isNotBlank() && parsed != null && parsed > 0) {
                        val newItem = TransactionItem(
                            title = description,
                            amount = parsed,
                            paymentType = paymentType,
                            date = if (date.isBlank()) currentDate else date,
                            profile = selectedProfileKey
                        )
                        val newList = listOf(newItem) + transactions
                        transactions = newList
                        saveTransactions(context, newList)
                    }
                    showAddDialog = false
                }) { Text("Adicionar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }

    if (showEditProfileDialog) {
        var tempPfName by remember { mutableStateOf(pfName) }
        var tempPjName by remember { mutableStateOf(pjName) }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Editar nomes dos perfis") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = tempPfName,
                        onValueChange = { tempPfName = it },
                        label = { Text("Nome do Perfil 1") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = tempPjName,
                        onValueChange = { tempPjName = it },
                        label = { Text("Nome do Perfil 2") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (tempPfName.isNotBlank()) pfName = tempPfName
                    if (tempPjName.isNotBlank()) pjName = tempPjName
                    saveProfileNames(context, pfName, pjName)
                    showEditProfileDialog = false
                }) { Text("Salvar") }
            },
            dismissButton = { TextButton(onClick = { showEditProfileDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, isSelected: Boolean, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) color.copy(alpha = 0.2f) else CardDefaults.cardColors().containerColor
        ),
        border = if (isSelected) BorderStroke(2.dp, color) else null
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                Text(title.take(1), modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold)
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(brl(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
