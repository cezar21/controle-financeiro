package br.com.cezar.controlefinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Green = Color(0xFF176B50)
private val Teal = Color(0xFF087F8C)
private val Purple = Color(0xFF6650A4)
private fun brl(value: Double) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

data class TransactionItem(
    val title: String,
    val amount: Double,
    val paymentType: String, // "Pix" ou "Cartão"
    val date: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MaterialTheme(colorScheme = lightColorScheme(primary = Green)) { FinanceDashboard() } }
    }
}

@Composable
fun FinanceDashboard() {
    var profile by remember { mutableStateOf("PF") }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf<String?>(null) } // null = Todos, "Pix" ou "Cartão"
    
    val currentDate = SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date())

    var transactions by remember {
        mutableStateOf(
            listOf(
                TransactionItem("Mercado", 185.50, "Pix", currentDate),
                TransactionItem("Internet", 99.90, "Cartão", currentDate),
                TransactionItem("Transporte", 48.00, "Pix", currentDate)
            )
        )
    }

    val total = transactions.sumOf { it.amount }
    val totalPix = transactions.filter { it.paymentType == "Pix" }.sumOf { it.amount }
    val totalCard = transactions.filter { it.paymentType == "Cartão" }.sumOf { it.amount }

    val displayedTransactions = if (selectedFilter == null) {
        transactions
    } else {
        transactions.filter { it.paymentType == selectedFilter }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFFF7F8FA)),
        contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Text("Meu financeiro", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(5.dp))
            Text("Controle seus gastos por perfil", color = Color.Gray)
            Spacer(Modifier.height(14.dp))
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                SegmentedButton(selected = profile == "PF", onClick = { profile = "PF" }, shape = SegmentedButtonDefaults.itemShape(0, 2)) { Text("Pessoa Física") }
                SegmentedButton(selected = profile == "PJ", onClick = { profile = "PJ" }, shape = SegmentedButtonDefaults.itemShape(1, 2)) { Text("Pessoa Jurídica") }
            }
        }
        item {
            Card(colors = CardDefaults.cardColors(containerColor = Green, contentColor = Color.White), shape = RoundedCornerShape(24.dp), modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("Total de despesas • ${if (profile == "PF") "Pessoa Física" else "Pessoa Jurídica"}")
                    Text(brl(total), style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
                    Text("Outubro 2026", color = Color.White.copy(alpha = .85f))
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
                    if (selectedFilter != null) {
                        TextButton(onClick = { selectedFilter = null }) {
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
                        isSelected = selectedFilter == "Pix",
                        modifier = Modifier.weight(1f).clickable {
                            selectedFilter = if (selectedFilter == "Pix") null else "Pix"
                        }
                    )
                    SummaryCard(
                        title = "Cartão",
                        amount = totalCard,
                        color = Purple,
                        isSelected = selectedFilter == "Cartão",
                        modifier = Modifier.weight(1f).clickable {
                            selectedFilter = if (selectedFilter == "Cartão") null else "Cartão"
                        }
                    )
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                val titleList = if (selectedFilter == null) "Últimas transações" else "Transações via $selectedFilter"
                Text(titleList, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showAddDialog = true }) { Text("+ Adicionar") }
            }
        }
        
        if (displayedTransactions.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma transação encontrada para este filtro.", color = Color.Gray)
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
                            // Remove da lista original
                            transactions = transactions.filterNot { it == tx }
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
            title = { Text("Nova despesa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, singleLine = true)
                    OutlinedTextField(amount, { amount = it }, label = { Text("Valor (R$)") }, singleLine = true)
                    OutlinedTextField(date, { date = it }, label = { Text("Data (DD/MM/AAAA)") }, singleLine = true)
                    
                    Text("Tipo de pagamento:", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = paymentType == "Pix", onClick = { paymentType = "Pix" })
                            Text("Pix")
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(selected = paymentType == "Cartão", onClick = { paymentType == "Cartão" })
                            Text("Cartão")
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val parsed = amount.replace(',', '.').toDoubleOrNull()
                    if (description.isNotBlank() && parsed != null && parsed > 0) {
                        transactions = listOf(TransactionItem(description, parsed, paymentType, if(date.isBlank()) currentDate else date)) + transactions
                    }
                    showAddDialog = false
                }) { Text("Adicionar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
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
