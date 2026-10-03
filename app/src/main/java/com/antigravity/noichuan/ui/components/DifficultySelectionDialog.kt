package com.antigravity.noichuan.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsRun
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.antigravity.noichuan.R
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.ui.theme.LevelBeginner
import com.antigravity.noichuan.ui.theme.LevelExpert
import com.antigravity.noichuan.ui.theme.LevelIntermediate
import com.antigravity.noichuan.ui.theme.PrimaryBlue
import com.antigravity.noichuan.ui.theme.SurfaceWhite
import com.antigravity.noichuan.ui.theme.TextPrimary
import com.antigravity.noichuan.ui.theme.TextSecondary

@Composable
fun DifficultySelectionDialog(
    onDismissRequest: () -> Unit,
    onSelectLevel: (DifficultyLevel) -> Unit = {},
    onSelectConfig: ((DifficultyLevel, String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember { mutableStateOf(DifficultyLevel.BEGINNER) }
    var selectedModeKey by remember { mutableStateOf("voice_runner") }

    Dialog(onDismissRequest = onDismissRequest) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dialog_select_mode_title),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Chế độ chơi (Game Mode)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ModeOptionItem(
                        icon = Icons.AutoMirrored.Rounded.DirectionsRun,
                        title = stringResource(R.string.mode_voice_runner_title),
                        description = stringResource(R.string.mode_voice_runner_desc),
                        isSelected = selectedModeKey == "voice_runner",
                        onClick = { selectedModeKey = "voice_runner" }
                    )
                    ModeOptionItem(
                        icon = Icons.Rounded.RecordVoiceOver,
                        title = stringResource(R.string.mode_tongue_twister_title),
                        description = stringResource(R.string.mode_tongue_twister_desc),
                        isSelected = selectedModeKey == "tongue_twister",
                        onClick = { selectedModeKey = "tongue_twister" }
                    )
                    ModeOptionItem(
                        icon = Icons.Rounded.Speed,
                        title = stringResource(R.string.mode_category_rush_title),
                        description = stringResource(R.string.mode_category_rush_desc),
                        isSelected = selectedModeKey == "category_rush",
                        onClick = { selectedModeKey = "category_rush" }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Chọn Độ khó (Difficulty Level: 3.0s / 2.5s / 2.0s)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    DifficultyChip(
                        text = stringResource(R.string.difficulty_beginner_label),
                        color = LevelBeginner,
                        isSelected = selectedLevel == DifficultyLevel.BEGINNER,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedLevel = DifficultyLevel.BEGINNER }
                    )
                    DifficultyChip(
                        text = stringResource(R.string.difficulty_intermediate_label),
                        color = LevelIntermediate,
                        isSelected = selectedLevel == DifficultyLevel.INTERMEDIATE,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedLevel = DifficultyLevel.INTERMEDIATE }
                    )
                    DifficultyChip(
                        text = stringResource(R.string.difficulty_expert_label),
                        color = LevelExpert,
                        isSelected = selectedLevel == DifficultyLevel.EXPERT,
                        modifier = Modifier.weight(1f),
                        onClick = { selectedLevel = DifficultyLevel.EXPERT }
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // 3. Nút Bắt đầu
                Button(
                    onClick = {
                        if (onSelectConfig != null) {
                            onSelectConfig(selectedLevel, selectedModeKey)
                        } else {
                            onSelectLevel(selectedLevel)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.dialog_start_challenge),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun ModeOptionItem(
    icon: ImageVector,
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryBlue else Color(0xFFE2E8F0)
    val bgColor = if (isSelected) PrimaryBlue.copy(alpha = 0.08f) else Color(0xFFF8FAFC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) PrimaryBlue else TextSecondary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSelected) PrimaryBlue else TextPrimary
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun DifficultyChip(
    text: String,
    color: Color,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .height(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) color else color.copy(alpha = 0.12f))
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) color else color.copy(alpha = 0.35f),
                shape = RoundedCornerShape(10.dp)
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) Color.White else color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
