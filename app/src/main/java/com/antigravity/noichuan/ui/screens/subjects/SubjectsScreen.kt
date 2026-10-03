package com.antigravity.noichuan.ui.screens.subjects

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Pets
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.SoupKitchen
import androidx.compose.material.icons.rounded.TextFields
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.antigravity.noichuan.data.model.DifficultyLevel
import com.antigravity.noichuan.data.model.Subject
import com.antigravity.noichuan.ui.theme.BackgroundLight
import com.antigravity.noichuan.ui.theme.PrimaryBlue
import com.antigravity.noichuan.ui.theme.SurfaceWhite
import com.antigravity.noichuan.ui.theme.TabExpertBg
import com.antigravity.noichuan.ui.theme.TabExpertText
import com.antigravity.noichuan.ui.theme.TabIntermediateBg
import com.antigravity.noichuan.ui.theme.TabIntermediateText

@Composable
fun SubjectsScreen(
    onBack: () -> Unit,
    onSelectSubject: (subjectKey: String, levelKey: String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedLevel by remember { mutableStateOf(DifficultyLevel.BEGINNER) }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = BackgroundLight
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .padding(horizontal = 16.dp)
        ) {
            // Header: Back button + Title "Subjects"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { onBack() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF1A1D1F),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = "Subjects",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryBlue,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 40.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 3 Tab phân loại cấp độ: Beginner, Intermediate, Expert
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Tab Beginner
                item {
                    val isSelected = selectedLevel == DifficultyLevel.BEGINNER
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) PrimaryBlue else Color(0xFFE8F1FD))
                            .clickable { selectedLevel = DifficultyLevel.BEGINNER }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Beginner",
                            color = if (isSelected) Color.White else PrimaryBlue,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab Intermediate
                item {
                    val isSelected = selectedLevel == DifficultyLevel.INTERMEDIATE
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color(0xFFFFA726) else TabIntermediateBg)
                            .clickable { selectedLevel = DifficultyLevel.INTERMEDIATE }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Intermediate",
                            color = if (isSelected) Color.White else TabIntermediateText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Tab Expert
                item {
                    val isSelected = selectedLevel == DifficultyLevel.EXPERT
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (isSelected) Color(0xFFEF5350) else TabExpertBg)
                            .clickable { selectedLevel = DifficultyLevel.EXPERT }
                            .padding(horizontal = 24.dp, vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Expert",
                            color = if (isSelected) Color.White else TabExpertText,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Lưới 2 cột thẻ danh mục bo góc 24dp
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(Subject.entries) { subject ->
                    SubjectGridCard(
                        subject = subject,
                        onClick = { onSelectSubject(subject.key, selectedLevel.key) }
                    )
                }
            }
        }
    }
}

@Composable
private fun SubjectGridCard(
    subject: Subject,
    onClick: () -> Unit
) {
    val icon = when (subject) {
        Subject.COLORS -> Icons.Rounded.Palette
        Subject.CAR_BRANDS -> Icons.Rounded.DirectionsCar
        Subject.WEEKDAYS -> Icons.Rounded.CalendarToday
        Subject.MONTHS -> Icons.Rounded.CalendarMonth
        Subject.ANIMALS -> Icons.Rounded.Pets
        Subject.VEHICLES -> Icons.Rounded.DirectionsCar
        Subject.KITCHEN -> Icons.Rounded.SoupKitchen
        Subject.COUNTRIES -> Icons.Rounded.Public
        Subject.DAILY_WORDS -> Icons.Rounded.TextFields
        Subject.TRAVEL_FOOD -> Icons.Rounded.Public
        Subject.ENTERTAINMENT_SPORTS -> Icons.Rounded.Palette
        Subject.SLANG_BRAINROT -> Icons.Rounded.Pets
        else -> Icons.Rounded.Public
    }

    val pastelBg = when (subject) {
        Subject.COLORS -> Color(0xFFE8F4FD)
        Subject.CAR_BRANDS -> Color(0xFFFDEEE8)
        Subject.WEEKDAYS -> Color(0xFFE3F7FD)
        Subject.MONTHS -> Color(0xFFF6E8FD)
        Subject.ANIMALS -> Color(0xFFEAF8EC)
        Subject.VEHICLES -> Color(0xFFEFE8FD)
        Subject.KITCHEN -> Color(0xFFFDF3E8)
        Subject.COUNTRIES -> Color(0xFFE8FDF7)
        Subject.DAILY_WORDS -> Color(0xFFE0F7FA)
        Subject.TRAVEL_FOOD -> Color(0xFFFBE9E7)
        Subject.ENTERTAINMENT_SPORTS -> Color(0xFFF3E5F5)
        Subject.SLANG_BRAINROT -> Color(0xFFFFEBEE)
        else -> Color(0xFFF5F5F5)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.0f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = pastelBg),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = subject.title,
                    tint = subject.cardColor,
                    modifier = Modifier.size(64.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = subject.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = PrimaryBlue,
            textAlign = TextAlign.Center
        )
    }
}
