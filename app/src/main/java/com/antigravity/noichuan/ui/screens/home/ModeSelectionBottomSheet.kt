package com.antigravity.noichuan.ui.screens.home

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.SheetState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.noichuan.R
import com.antigravity.noichuan.data.model.GameMode
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.ui.components.BouncyBox
import com.antigravity.noichuan.ui.theme.AccentYellow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModeSelectionBottomSheet(
    sheetState: SheetState,
    subject: Subject,
    onDismiss: () -> Unit,
    onStartChallenge: (selectedMode: GameMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf(GameMode.EASY) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp, top = 8.dp)
        ) {
            // Header with Subject emoji and title
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(subject.cardColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = subject.emoji, fontSize = 28.sp)
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = stringResource(R.string.mode_select_title),
                        fontSize = 14.sp,
                        color = Color(0xFF78909C),
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = subject.titleVi,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Modes List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GameModeOptionCard(
                    mode = GameMode.EASY,
                    isSelected = selectedMode == GameMode.EASY,
                    badgeColor = Color(0xFF43A047),
                    starsText = "⭐ 50+ pts",
                    timeText = "15s / word",
                    onClick = { selectedMode = GameMode.EASY }
                )

                GameModeOptionCard(
                    mode = GameMode.MEDIUM,
                    isSelected = selectedMode == GameMode.MEDIUM,
                    badgeColor = Color(0xFFFB8C00),
                    starsText = "⭐⭐ 65+ pts",
                    timeText = "10s / word",
                    onClick = { selectedMode = GameMode.MEDIUM }
                )

                GameModeOptionCard(
                    mode = GameMode.HARD,
                    isSelected = selectedMode == GameMode.HARD,
                    badgeColor = Color(0xFFE53935),
                    starsText = "⭐⭐⭐ 80+ pts",
                    timeText = "6s / word",
                    onClick = { selectedMode = GameMode.HARD }
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Start Button
            BouncyBox(
                modifier = Modifier.fillMaxWidth(),
                onClick = { onStartChallenge(selectedMode) }
            ) {
                Button(
                    onClick = { onStartChallenge(selectedMode) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0288D1),
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.mode_start_btn),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GameModeOptionCard(
    mode: GameMode,
    isSelected: Boolean,
    badgeColor: Color,
    starsText: String,
    timeText: String,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) Color(0xFF0288D1) else Color(0xFFE0E0E0)
    val bgColor = if (isSelected) Color(0xFFE1F5FE) else Color(0xFFF8FAFC)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(bgColor)
            .border(
                width = if (isSelected) 2.5.dp else 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(18.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0288D1))
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = stringResource(mode.titleRes),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1A237E)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(badgeColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = starsText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Timer,
                        contentDescription = null,
                        tint = Color(0xFF78909C),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = timeText,
                        fontSize = 13.sp,
                        color = Color(0xFF546E7A)
                    )
                }
            }
        }
    }
}
