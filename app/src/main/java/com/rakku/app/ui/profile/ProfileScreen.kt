package com.rakku.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.rakku.app.R
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.rakku.app.ui.components.RoleBadge
import com.rakku.app.ui.theme.AdminRed
import com.rakku.app.ui.theme.CyanAccent
import com.rakku.app.ui.theme.DarkBackground
import com.rakku.app.ui.theme.DarkBorder
import com.rakku.app.ui.theme.DarkSurface
import com.rakku.app.ui.theme.DarkSurfaceVariant
import com.rakku.app.ui.theme.IndigoSecondary
import com.rakku.app.ui.theme.TextPrimary
import com.rakku.app.ui.theme.TextSecondary
import com.rakku.app.ui.theme.VioletPrimary

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateToLogin: () -> Unit,
    onSelectAnimeDetail: (String) -> Unit,
    onSelectMangaDetail: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenBookmarks: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val userProfile by viewModel.userProfile.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()
    val history by viewModel.history.collectAsState()

    // Refresh data profil (EXP/level/coin) tiap layar ini kebuka - biar kalau ada
    // perubahan dari layar lain (mis. abis dapet EXP nonton anime), langsung
    // kelihatan update di sini tanpa perlu restart app.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.refreshProfile()
    }

    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showTopupDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }
    var showAdminPanelDialog by remember { mutableStateOf(false) }

    if (userProfile == null) {
        // Guest view
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Anda Belum Login",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Login untuk menyimpan bookmark, riwayat, koin, dan ikut obrolan komunitas.",
                    fontSize = 12.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onNavigateToLogin,
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Masuk / Daftar Akun")
                }
            }
        }
        return
    }

    val profile = userProfile!!

    // Edit Profile Dialog Component
    if (showEditProfileDialog) {
        var newUsername by remember { mutableStateOf(profile.username ?: "") }
        var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
        var isUpdating by remember { mutableStateOf(false) }

        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri -> selectedImageUri = uri }

        AlertDialog(
            onDismissRequest = { showEditProfileDialog = false },
            title = { Text("Edit Profil", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant)
                            .clickable { imagePickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null) {
                            AsyncImage(
                                model = selectedImageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            AsyncImage(
                                model = profile.avatar_url,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.4f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, tint = Color.White)
                        }
                    }
                    Text("Ketuk foto untuk mengganti avatar", fontSize = 11.sp, color = TextSecondary, modifier = Modifier.padding(top = 4.dp))

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = newUsername,
                        onValueChange = { newUsername = it },
                        label = { Text("Username", color = TextSecondary) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isUpdating = true
                        viewModel.updateProfileInfo(context, newUsername, selectedImageUri) {
                            isUpdating = false
                            showEditProfileDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    enabled = !isUpdating
                ) {
                    if (isUpdating) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                    else Text("Simpan")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditProfileDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Topup Dialog
    if (showTopupDialog) {
        val packages = listOf(
            Triple(100, "Rp 10.000", 100),
            Triple(300, "Rp 28.000", 300),
            Triple(600, "Rp 50.000", 600),
            Triple(1200, "Rp 95.000", 1200)
        )
        AlertDialog(
            onDismissRequest = { showTopupDialog = false },
            title = { Text("Top Up Rakku Koin", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Pilih paket koin untuk aktivasi fitur premium:", fontSize = 12.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    packages.forEach { (coinCount, price, amount) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable {
                                    viewModel.createTopupRequest(amount, price) { success ->
                                        showTopupDialog = false
                                        val waMessage = Uri.encode("Halo Admin Rakku, saya ingin topup $coinCount Koin ($price). User ID: ${profile.id}")
                                        val waUrl = "https://wa.me/6288973461209?text=$waMessage"
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(waUrl))
                                        context.startActivity(intent)
                                    }
                                },
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_rakku_coin),
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("$coinCount Koin", fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Text(price, color = CyanAccent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showTopupDialog = false }) {
                    Text("Tutup", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Feedback Dialog
    if (showFeedbackDialog) {
        var feedbackType by remember { mutableStateOf("saran") }
        var feedbackMsg by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showFeedbackDialog = false },
            title = { Text("Saran & Laporan", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Row {
                        RadioButton(selected = feedbackType == "saran", onClick = { feedbackType = "saran" })
                        Text("Saran", color = TextPrimary, modifier = Modifier.align(Alignment.CenterVertically))
                        Spacer(modifier = Modifier.width(16.dp))
                        RadioButton(selected = feedbackType == "laporan", onClick = { feedbackType = "laporan" })
                        Text("Laporan Bug", color = TextPrimary, modifier = Modifier.align(Alignment.CenterVertically))
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = feedbackMsg,
                        onValueChange = { feedbackMsg = it },
                        placeholder = { Text("Tuliskan masukan atau masalah aplikasi...", color = TextSecondary) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyanAccent,
                            unfocusedBorderColor = DarkBorder,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (feedbackMsg.isNotBlank()) {
                            viewModel.submitFeedback(feedbackType, feedbackMsg) {
                                showFeedbackDialog = false
                                Toast.makeText(context, "Saran/Laporan terkirim", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary)
                ) {
                    Text("Kirim")
                }
            },
            dismissButton = {
                TextButton(onClick = { showFeedbackDialog = false }) {
                    Text("Batal", color = TextSecondary)
                }
            },
            containerColor = DarkSurface
        )
    }

    // Admin Panel Screen Dialog
    if (showAdminPanelDialog) {
        AdminPanelDialog(
            viewModel = viewModel,
            onDismiss = { showAdminPanelDialog = false }
        )
    }

    // Main Profile Screen View
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Profile Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(VioletPrimary, CyanAccent)))
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(contentAlignment = Alignment.BottomEnd) {
                    AsyncImage(
                        model = profile.avatar_url,
                        contentDescription = null,
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentScale = ContentScale.Crop
                    )
                    IconButton(
                        onClick = { showEditProfileDialog = true },
                        modifier = Modifier
                            .size(28.dp)
                            .background(CyanAccent, CircleShape)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = profile.username ?: "User",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    RoleBadge(role = profile.role)
                    Spacer(modifier = Modifier.width(8.dp))
                    val truncatedUuid = profile.id.take(8) + "..."
                    Text(
                        text = "ID: $truncatedUuid",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    IconButton(
                        onClick = {
                            clipboardManager.setText(AnnotatedString(profile.id))
                            Toast.makeText(context, "User ID disalin", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Salin ID", tint = CyanAccent, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Level & EXP Progress Bar
                val lvl = profile.level ?: 1
                val exp = profile.exp ?: 0
                val targetExp = lvl * 100
                val progress = (exp.toFloat() / targetExp.toFloat()).coerceIn(0f, 1f)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Level $lvl", fontWeight = FontWeight.Bold, color = CyanAccent, fontSize = 12.sp)
                    Text("$exp / $targetExp EXP", color = TextSecondary, fontSize = 12.sp)
                }
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                    color = CyanAccent,
                    trackColor = DarkSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Rakku Coin Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_rakku_coin),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Rakku Coin", fontSize = 12.sp, color = TextSecondary)
                        Text("${profile.rakku_coin ?: 0} Koin", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }
                Button(
                    onClick = { showTopupDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = VioletPrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Top Up")
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Menu Options
        ProfileMenuItem(icon = Icons.Default.Bookmark, label = "Bookmark Saya", onClick = onOpenBookmarks)
        ProfileMenuItem(icon = Icons.Default.History, label = "Riwayat Tontonan", onClick = onOpenHistory)
        ProfileMenuItem(icon = Icons.Default.Feedback, label = "Saran & Laporan", onClick = { showFeedbackDialog = true })

        // Staff Admin Panel Entry (visible if admin or moderator)
        val isStaff = profile.role in listOf("admin", "moderator")
        if (isStaff) {
            Spacer(modifier = Modifier.height(12.dp))
            ProfileMenuItem(
                icon = Icons.Default.AdminPanelSettings,
                label = "Admin Panel Control",
                tint = AdminRed,
                onClick = {
                    viewModel.loadAdminData()
                    showAdminPanelDialog = true
                }
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        // Logout
        Button(
            onClick = { viewModel.logout() },
            colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Keluar (Logout)", color = Color.Red)
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = CyanAccent,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = TextPrimary)
        }
    }
}

@Composable
fun AdminPanelDialog(
    viewModel: ProfileViewModel,
    onDismiss: () -> Unit
) {
    val adminState by viewModel.adminState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) } // 0: User Mgmt, 1: Ban, 2: Announcements, 3: Reports

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Admin & Mod Control Panel", color = AdminRed, fontWeight = FontWeight.Bold) },
        text = {
            Column(modifier = Modifier.height(450.dp)) {
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
                            LazyColumn {
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

                            LazyColumn {
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
                            LazyColumn {
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
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Tutup", color = TextSecondary) }
        },
        containerColor = DarkSurface
    )
}
