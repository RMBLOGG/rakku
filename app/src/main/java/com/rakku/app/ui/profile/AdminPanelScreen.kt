package com.rakku.app.ui.profile

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    var selectedTab by remember { mutableStateOf(0) } // 0: Users, 1: Pengumuman, 2: Laporan, 3: Border

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
                Text(
                    "Border",
                    fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal,
                    color = if (selectedTab == 3) CyanAccent else TextSecondary,
                    modifier = Modifier.clickable {
                        selectedTab = 3
                        viewModel.loadAdminBorders()
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (selectedTab == 3) {
                BorderManagementTab(viewModel = viewModel)
            } else {
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
}

@Composable
private fun BorderManagementTab(viewModel: ProfileViewModel) {
    val context = LocalContext.current
    val borders by viewModel.adminBorders.collectAsState()

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var borderName by remember { mutableStateOf("") }
    var borderPrice by remember { mutableStateOf("0") }
    var isUploading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadAdminBorders()
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    Column(modifier = Modifier.fillMaxSize()) {
        // Form upload border baru
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Tambah Border Baru", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurface)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Icon(Icons.Filled.Image, contentDescription = "Pilih gambar", tint = TextSecondary, modifier = Modifier.size(24.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        OutlinedTextField(
                            value = borderName,
                            onValueChange = { borderName = it },
                            label = { Text("Nama Border", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = borderPrice,
                            onValueChange = { borderPrice = it.filter { c -> c.isDigit() } },
                            label = { Text("Harga (Rakku Coin)", fontSize = 11.sp) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = {
                        val uri = selectedImageUri
                        if (uri == null) {
                            Toast.makeText(context, "Pilih gambar border dulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (borderName.isBlank()) {
                            Toast.makeText(context, "Isi nama border dulu", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        isUploading = true
                        viewModel.adminUploadBorder(context, uri, borderName, borderPrice.toIntOrNull() ?: 0) { success, errorMsg ->
                            isUploading = false
                            if (success) {
                                Toast.makeText(context, "Border berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                                selectedImageUri = null
                                borderName = ""
                                borderPrice = "0"
                            } else {
                                Toast.makeText(context, errorMsg ?: "Gagal menambahkan border", Toast.LENGTH_LONG).show()
                            }
                        }
                    },
                    enabled = !isUploading,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isUploading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    } else {
                        Text("Upload & Tambah Border")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))
        Text("Daftar Border (${borders.size})", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
        Spacer(modifier = Modifier.height(4.dp))

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(borders) { border ->
                var showDeleteConfirm by remember { mutableStateOf(false) }

                if (showDeleteConfirm) {
                    AlertDialog(
                        onDismissRequest = { showDeleteConfirm = false },
                        title = { Text("Hapus Border?") },
                        text = { Text("\"${border.name}\" akan dihapus permanen dan tidak bisa dibeli/dipasang lagi.") },
                        confirmButton = {
                            Button(
                                onClick = {
                                    border.id?.let { viewModel.adminDeleteBorder(it) }
                                    showDeleteConfirm = false
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                            ) { Text("Hapus") }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteConfirm = false }) { Text("Batal") }
                        }
                    )
                }

                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = border.image_url,
                            contentDescription = null,
                            modifier = Modifier.size(44.dp),
                            contentScale = ContentScale.Fit
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(border.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                            Text(
                                if (border.price_coin <= 0) "Gratis" else "${border.price_coin} Koin",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }
                        Button(
                            onClick = { border.id?.let { viewModel.adminSetBorderActive(it, !border.is_active) } },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (border.is_active) CyanAccent else DarkSurface
                            ),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text(if (border.is_active) "Aktif" else "Mati", fontSize = 10.sp, color = if (border.is_active) Color.Black else TextSecondary)
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Button(
                            onClick = { showDeleteConfirm = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Text("Hapus", fontSize = 10.sp)
                        }
                    }
                }
            }
        }
    }
}
