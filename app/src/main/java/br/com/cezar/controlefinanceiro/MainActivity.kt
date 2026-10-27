package br.com.cezar.controlefinanceiro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.text.NumberFormat
import java.util.Locale

private val Green = Color(0xFF176B50)
private val Teal = Color(0xFF087F8C)
private val Purple = Color(0xFF6650A4)
private fun brl(value: Double) = NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(value)

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
    var transactions by remember { mutableStateOf(listOf("Mercado" to 185.50, "Internet" to 99.90, "Transporte" to 48.00)) }
    val total = transactions.sumOf { it.second }

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
            Text("Gastos por meio de pagamento", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard("Pix", total * .55, Teal, Modifier.weight(1f))
                SummaryCard("Cartão", total * .45, Purple, Modifier.weight(1f))
            }
        }
        item {
            Text("Gastos por categoria", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            listOf("Alimentação" to .55f, "Moradia" to .28f, "Transporte" to .17f).forEach { (name, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text(name); Text("${(value * 100).toInt()}%") }
                LinearProgressIndicator(progress = { value }, modifier = Modifier.fillMaxWidth().padding(top = 5.dp, bottom = 10.dp), color = Green)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Últimas transações", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { showAddDialog = true }) { Text("+ Adicionar") }
            }
        }
        items(transactions.size) { index ->
            val transaction = transactions[index]
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                Row(Modifier.padding(16.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text(transaction.first, fontWeight = FontWeight.SemiBold); Text(if (index % 2 == 0) "Pix • Despesa" else "Cartão • Despesa", color = Color.Gray, style = MaterialTheme.typography.bodySmall) }
                    Text("− ${brl(transaction.second)}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
    if (showAddDialog) {
        var description by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Nova despesa") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(description, { description = it }, label = { Text("Descrição") }, singleLine = true)
                    OutlinedTextField(amount, { amount = it }, label = { Text("Valor (R$)") }, singleLine = true)
                    Text("Protótipo demonstrativo: os dados ainda não são salvos no banco.", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            },
            confirmButton = { TextButton(onClick = {
                val parsed = amount.replace(',', '.').toDoubleOrNull()
                if (description.isNotBlank() && parsed != null && parsed > 0) transactions = listOf(description to parsed) + transactions
                showAddDialog = false
            }) { Text("Adicionar") } },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun SummaryCard(title: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Card(modifier, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Surface(color = color.copy(alpha = .12f), shape = RoundedCornerShape(12.dp)) {
                Text(title.take(1), modifier = Modifier.padding(horizontal = 13.dp, vertical = 8.dp), color = color, fontWeight = FontWeight.Bold)
            }
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(brl(amount), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}
