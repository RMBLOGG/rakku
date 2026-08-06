package com.rakku.app.ui.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.weight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rakku.app.ui.theme.AdminRed
import com.rakku.app.ui.theme.CyanAccent
import com.rakku.app.ui.theme.DarkBackground
import com.rakku.app.ui.theme.DarkSurface
import com.rakku.app.ui.theme.DarkSurfaceVariant
import com.rakku.app.ui.theme.TextPrimary
import com.rakku.app.ui.theme.TextSecondary
import com.rakku.app.ui.theme.VioletPrimary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminPanelScreen(
    viewModel: ProfileViewModel,
    onBack: () -> Unit
) {
    val adminState by viewModel.adminState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Pengumuman, 2: Laporan

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Admin & Mod Control Panel", color = AdminRed, fontWeight = FontWeight.Bold, fontSize = 15.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = DarkSurface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(12.dp)
        ) {
            // Tab Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    "Users",
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 0) CyanAccent else TextSecondary,
                    modifier = Modifier.clickable { selectedTab = 0 }
                )
                Text(
                    "Pengumuman",
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 1) CyanAccent else TextSecondary,
                    modifier = Modifier.clickable { selectedTab = 1 }
                )
                Text(
                    "Laporan",
                    fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 2) CyanAccent else TextSecondary,
                    modifier = Modifier.clickable { selectedTab = 2 }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when (val state = adminState) {
                is AdminUiState.Idle -> {}
                is AdminUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = CyanAccent)
                    }
                }
                is AdminUiState.Error -> {
                    Text(state.message, color = Color.Red)
                }
                is AdminUiState.Success -> {
                    if (selectedTab == 0) {
                        // User Management
                        var searchUser by remember { mutableStateOf("") }
                        OutlinedTextField(
                            value = searchUser,
                            onValueChange = { searchUser = it },
                            placeholder = { Text("Cari User ID / Nama...", fontSize = 11.sp, color = TextSecondary) },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        val filteredUsers = state.users.filter {
                            it.username?.contains(searchUser, true) == true || it.id.contains(searchUser, true)
                        }
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(filteredUsers) { user ->
                                var showBanDialogForUser by remember { mutableStateOf(false) }
                                var showAddCoinDialogForUser by remember { mutableStateOf(false) }

                                if (showBanDialogForUser) {
                                    var banReason by remember { mutableStateOf("") }
                                    var durationHours by remember { mutableStateOf<Int?>(1) } // 1, 5, 7, 720, null

                                    AlertDialog(
                                        onDismissRequest = { showBanDialogForUser = false },
                                        title = { Text("Ban User ${user.username}", color = Color.Red) },
                                        text = {
                                            Column {
                                                OutlinedTextField(
                                                    value = banReason,
                                                    onValueChange = { banReason = it },
                                                    label = { Text("Alasan Banned") }
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                                Text("Durasi Banned:")
                                                listOf(
                                                    1 to "1 Jam",
                                                    5 to "5 Jam",
                                                    7 to "7 Jam",
                                                    720 to "30 Hari",
                                                    null to "Permanen"
                                                ).forEach { (hrs, label) ->
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.clickable { durationHours = hrs }
                                                    ) {
                                                        RadioButton(selected = durationHours == hrs, onClick = { durationHours = hrs })
                                                        Text(label, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        },
                                        confirmButton = {
                                            Button(
                                                onClick = {
                                                    viewModel.adminBanUser(user.id, banReason, durationHours) {
                                                        showBanDialogForUser = false
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                            ) {
                                                Text("Banned Now")
                                            }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showBanDialogForUser = false }) { Text("Batal") }
                                        }
                                    )
                                }

                                if (showAddCoinDialogForUser) {
                                    var coinAmt by remember { mutableStateOf("100") }
                                    AlertDialog(
                                        onDismissRequest = { showAddCoinDialogForUser = false },
                                        title = { Text("Tambah Koin ke ${user.username}") },
                                        text = {
                                            OutlinedTextField(
                                                value = coinAmt,
                                                onValueChange = { coinAmt = it },
                                                label = { Text("Jumlah Koin") }
                                            )
                                        },
                                        confirmButton = {
                                            Button(onClick = {
                                                val amt = coinAmt.toIntOrNull() ?: 0
                                                viewModel.adminAddCoin(user.id, amt) {
                                                    showAddCoinDialogForUser = false
                                                }
                                            }) { Text("Tambah") }
                                        },
                                        dismissButton = {
                                            TextButton(onClick = { showAddCoinDialogForUser = false }) { Text("Batal") }
                                        }
                                    )
                                }

                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("${user.username} (${user.role})", fontWeight = FontWeight.Bold, color = TextPrimary)
                                        Text("ID: ${user.id.take(8)}... | Koin: ${user.rakku_coin ?: 0}", fontSize = 11.sp, color = TextSecondary)
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Button(
                                                onClick = { showAddCoinDialogForUser = true },
                                                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                                                modifier = Modifier.height(30.dp)
                                            ) {
                                                Text("+ Koin", fontSize = 10.sp, color = Color.Black)
                                            }
                                            if (user.is_banned == true) {
                                                Button(
                                                    onClick = { viewModel.adminUnbanUser(user.id) {} },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Green),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Unban", fontSize = 10.sp)
                                                }
                                            } else {
                                                Button(
                                                    onClick = { showBanDialogForUser = true },
                                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                                    modifier = Modifier.height(30.dp)
                                                ) {
                                                    Text("Ban", fontSize = 10.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    } else if (selectedTab == 1) {
                        // Announcements Management
                        var newTitle by remember { mutableStateOf("") }
                        var newContent by remember { mutableStateOf("") }
                        OutlinedTextField(value = newTitle, onValueChange = { newTitle = it }, label = { Text("Judul Pengumuman") }, modifier = Modifier.fillMaxWidth())
                        OutlinedTextField(value = newContent, onValueChange = { newContent = it }, label = { Text("Isi Pengumuman") }, modifier = Modifier.fillMaxWidth())
                        Button(
                            onClick = {
                                if (newTitle.isNotBlank() && newContent.isNotBlank()) {
                                    viewModel.createAnnouncement(newTitle, newContent, true) {
                                        newTitle = ""
                                        newContent = ""
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                        ) { Text("Buat Pengumuman Baru") }

                        Spacer(modifier = Modifier.height(8.dp))

                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(state.announcements) { ann ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(8.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(ann.title, fontWeight = FontWeight.Bold, color = TextPrimary)
                                            Text(ann.content, fontSize = 11.sp, color = TextSecondary)
                                        }
                                        Button(onClick = { ann.id?.let { viewModel.toggleAnnouncement(it, !(ann.is_active ?: true)) } }) {
                                            Text(if (ann.is_active == true) "Aktif" else "Mati", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        // Reports List
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            items(state.commentReports) { r ->
                                Card(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                                ) {
                                    Column(modifier = Modifier.padding(8.dp)) {
                                        Text("Laporan Komentar [${r.category}]", fontWeight = FontWeight.Bold, color = Color.Red)
                                        Text("Ket: ${r.description ?: "-"}", fontSize = 11.sp, color = TextPrimary)
                                        Button(
                                            onClick = { r.comment_id?.let { viewModel.deleteReportedComment(it) } },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                        ) {
                                            Text("Hapus Komentar Ini", fontSize = 10.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
