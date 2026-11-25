package com.antigravity.noichuan.ui.screens.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.antigravity.noichuan.R
import com.antigravity.noichuan.ui.theme.BackgroundLight
import com.antigravity.noichuan.ui.theme.PrimaryBlue
import com.antigravity.noichuan.ui.theme.TextPrimary
import com.antigravity.noichuan.ui.theme.TextSecondary

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isSfxEnabled by viewModel.isSfxEnabled.collectAsState()
    val isBgmEnabled by viewModel.isBgmEnabled.collectAsState()
    val speechEngineType by viewModel.speechEngineType.collectAsState()
    var showCreditsDialog by remember { mutableStateOf(false) }

    val hasMicPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    val hasCameraPerm = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            // Header: Back button + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE8F1FD))
                        .clickable { onNavigateBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Text(
                    text = "Settings",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 1. Âm thanh & SFX
            SettingsSectionHeader(title = "Sound & Music")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Toggle SFX
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.VolumeUp,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Sound Effects (SFX)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Ding, buzz, and crash sounds during play", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = isSfxEnabled,
                            onCheckedChange = { viewModel.toggleSfx(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Toggle BGM
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.MusicNote,
                                contentDescription = null,
                                tint = PrimaryBlue,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text("Background Music (BGM)", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("Light background loop during challenges", fontSize = 12.sp, color = TextSecondary)
                            }
                        }
                        Switch(
                            checked = isBgmEnabled,
                            onCheckedChange = { viewModel.toggleBgm(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = PrimaryBlue)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Engine Nhận Diện Giọng Nói
            SettingsSectionHeader(title = "Speech Recognition Engine")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Option A: Vosk
                    EngineOptionCard(
                        title = "Vosk Speech Engine (Recommended)",
                        badge = "On-Device Offline",
                        description = "100% on-device recognition powered by Kaldi AI. Works offline with ultra-low latency and full privacy.",
                        isSelected = speechEngineType == "VOSK",
                        onClick = { viewModel.setSpeechEngine("VOSK") }
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Option B: Google
                    EngineOptionCard(
                        title = "Google SpeechRecognizer",
                        badge = "Android System Online",
                        description = "Uses Android system speech engine. Supports long phrases; requires Internet or downloaded Google voice pack.",
                        isSelected = speechEngineType == "GOOGLE",
                        onClick = { viewModel.setSpeechEngine("GOOGLE") }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. Quản Lý Quyền (Permissions)
            SettingsSectionHeader(title = "Device Permissions")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Mic Status
                    PermissionStatusRow(
                        icon = Icons.Rounded.Mic,
                        title = "Microphone",
                        subtitle = "Required to record audio and score pronunciation",
                        isGranted = hasMicPerm
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Camera Status
                    PermissionStatusRow(
                        icon = Icons.Rounded.Videocam,
                        title = "Front Camera",
                        subtitle = "Optional for AR selfie video effects",
                        isGranted = hasCameraPerm
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Nút mở cài đặt ứng dụng
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF1F5F9))
                            .clickable {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", context.packageName, null)
                                }
                                context.startActivity(intent)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Manage permissions in System Settings", fontSize = 13.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        Icon(imageVector = Icons.Rounded.OpenInNew, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. Google Play Data Safety
            SettingsSectionHeader(title = "Privacy & Data Safety")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF86EFAC)),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
                    Icon(imageVector = Icons.Rounded.Security, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Your Privacy Is Protected", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF15803D))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "All microphone audio is processed locally on-device and never shared with third parties. Videos are only saved when you tap Save.",
                            fontSize = 12.sp,
                            color = Color(0xFF166534),
                            lineHeight = 17.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 5. Version & Credits
            SettingsSectionHeader(title = "App Information")

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Version", fontSize = 14.sp, color = TextSecondary)
                        Text("1.0.0 (Release A7)", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCreditsDialog = true }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Open Source Licenses & Credits", fontSize = 14.sp, color = PrimaryBlue, fontWeight = FontWeight.SemiBold)
                        Icon(imageVector = Icons.Rounded.Info, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }

            Spacer(modifier = Modifier.height(30.dp))
        }

        // Dialog Credits & Licenses
        if (showCreditsDialog) {
            AlertDialog(
                onDismissRequest = { showCreditsDialog = false },
                title = { Text("Credits & Open Source Licenses", fontWeight = FontWeight.Bold, fontSize = 17.sp) },
                text = {
                    Column(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("• Vosk Android Speech Recognizer (Apache License 2.0)", fontSize = 13.sp)
                        Text("• Apache Commons Codec (Apache License 2.0)", fontSize = 13.sp)
                        Text("• Jetpack Compose & AndroidX (Apache License 2.0)", fontSize = 13.sp)
                        Text("• Kotlinx Coroutines & Serialization (Apache License 2.0)", fontSize = 13.sp)
                        Text("• SFX Assets (ding, buzz, crash, bgm): Procedurally synthesized under CC0 / Apache 2.0.", fontSize = 13.sp)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showCreditsDialog = false }) {
                        Text("Close", fontWeight = FontWeight.Bold, color = PrimaryBlue)
                    }
                }
            )
        }
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 13.sp,
        fontWeight = FontWeight.Bold,
        color = TextSecondary,
        modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
    )
}

@Composable
private fun EngineOptionCard(
    title: String,
    badge: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0),
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = isSelected,
                        onClick = onClick,
                        colors = RadioButtonDefaults.colors(selectedColor = PrimaryBlue)
                    )
                    Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) PrimaryBlue.copy(alpha = 0.15f) else Color(0xFFE2E8F0))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(badge, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isSelected) PrimaryBlue else Color.Gray)
                }
            }
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp,
                modifier = Modifier.padding(start = 48.dp, end = 4.dp, bottom = 4.dp)
            )
        }
    }
}

@Composable
private fun PermissionStatusRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = PrimaryBlue, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                Text(subtitle, fontSize = 12.sp, color = TextSecondary)
            }
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isGranted) Color(0xFFDCFCE7) else Color(0xFFFEE2E2))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (isGranted) "Granted" else "Not Granted",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (isGranted) Color(0xFF16A34A) else Color(0xFFDC2626)
            )
        }
    }
}
