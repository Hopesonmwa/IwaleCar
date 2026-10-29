package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.AddAlarm
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.CarWashTier
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.BottomNavigationBar
import com.example.myapplication.ui.components.NavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

data class ExtraDurationOption(
    val id: String,
    val durationText: String,
    val minutesValue: Int,
    val description: String,
    val priceText: String,
    val runtimeBadge: String,
    val isMostPopular: Boolean = false,
    val price: Int
)

@Composable
fun BookedSessionsScreen(
    onBaysClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onExtendPaymentClick: (CarWashTier) -> Unit = {}
) {
    val currentUser = UserRepository.getCurrentUser()
    val activeBookings = BookingRepository.getCustomerBookings(currentUser.email, "All")

    var selectedTopTab by remember { mutableStateOf(0) } // 0 = Booked Sessions, 1 = Live Wash Timer
    var isTimerRunning by remember { mutableStateOf(false) }
    var isTimerPaused by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(290) } // ~4 min 50 sec (TC-14 trigger)
    var showExtendModal by remember { mutableStateOf(false) }
    var expiryNotificationVisible by remember { mutableStateOf(false) }

    // Ticking Live Timer & TC-14 Warning Expiry Notification Trigger
    LaunchedEffect(isTimerRunning, isTimerPaused) {
        if (isTimerRunning && !isTimerPaused) {
            while (remainingSeconds > 0) {
                delay(1000)
                remainingSeconds--

                // TC-14: Check warning notification threshold <= 5 mins (300 seconds)
                if (BookingRepository.shouldNotifySessionExpiry("BK-101", remainingSeconds.toLong())) {
                    expiryNotificationVisible = true
                }
            }
        }
    }

    val formatMinutesSeconds: (Int) -> String = { totalSec ->
        val mins = totalSec / 60
        val secs = totalSec % 60
        String.format("%02d:%02d", mins, secs)
    }

    // Extend Session Pop-up Modal Window
    if (showExtendModal) {
        ExtendSessionModal(
            currentCountdownText = formatMinutesSeconds(remainingSeconds),
            onDismiss = { showExtendModal = false },
            onProceedToPayment = { selectedOption, conflictMessage ->
                if (conflictMessage == null) {
                    showExtendModal = false
                    remainingSeconds += selectedOption.minutesValue * 60
                    val tier = CarWashTier(
                        id = selectedOption.id,
                        title = "${selectedOption.durationText} Extra Extension",
                        description = selectedOption.description,
                        price = selectedOption.price,
                        formattedPrice = selectedOption.priceText
                    )
                    onExtendPaymentClick(tier)
                }
            }
        )
    }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_iwale_wash_logo),
                        contentDescription = "IWALE Logo",
                        modifier = Modifier.size(28.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "IWALE CAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7),
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Booked Sessions",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0B132B))
                        .clickable { onProfileClick() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = NavItem.BOOKINGS,
                onTabSelected = { tab ->
                    when (tab) {
                        NavItem.BAYS -> onBaysClick()
                        NavItem.HISTORY -> onHistoryClick()
                        NavItem.PROFILE -> onProfileClick()
                        NavItem.BOOKINGS -> {}
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Title Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Booked Sessions",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBF2FF))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "(${activeBookings.size} Available)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }

            // Expiry Notification Banner (TC-14)
            item {
                if (expiryNotificationVisible) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Session Expiring Soon!",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Text(
                                    text = "Your wash session has less than 5 minutes remaining. Tap 'Extend Session' if you need more time.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 16.sp
                                )
                            }
                            IconButton(onClick = { expiryNotificationVisible = false }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF92400E),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Top Switcher Tabs
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTopTab == 0) Color(0xFF0B132B) else Color.Transparent)
                            .clickable { selectedTopTab = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Booked Sessions (${activeBookings.size})",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selectedTopTab == 0) Color.White else Color(0xFF334155)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (selectedTopTab == 1) Color(0xFF0B132B) else Color.Transparent)
                            .clickable {
                                selectedTopTab = 1
                                isTimerRunning = true
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Live Wash Timer",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTopTab == 1) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Tab 0 View: Booked Sessions List
            if (selectedTopTab == 0) {
                item {
                    Text(
                        text = "Select a paid reservation to start your washing bay session.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B)
                    )
                }

                if (activeBookings.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "No active booked sessions.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                } else {
                    items(items = activeBookings, key = { it.id }) { booking ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "LILONGWE – AREA 4 EXPRESS STATION",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = booking.bayTitle,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(Color(0xFFDCFCE7))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = "PAID & READY • #${booking.bookingReference}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF16A34A)
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color(0xFFF0F5FA))
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.AccessTime,
                                                contentDescription = null,
                                                tint = Color(0xFF0284C7),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = booking.timeSlotText,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )
                                        }

                                        Text(
                                            text = "${booking.amountFormatted} Paid via ${booking.paymentMethod}",
                                            fontSize = 11.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                Button(
                                    onClick = {
                                        selectedTopTab = 1
                                        isTimerRunning = true
                                        isTimerPaused = false
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0B132B),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(48.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "START SESSION  ",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Icon(
                                            imageVector = Icons.Default.PlayArrow,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Info Note Box
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBF2FF))
                            .padding(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Tapping Start Session activates the bay equipment, automatically activates high-pressure valves, and initiates your authoritative runtime.",
                                fontSize = 12.sp,
                                color = Color(0xFF334155),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            } else {
                // Tab 1 View: Live Wash Timer View
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "LILONGWE – AREA 4 EXPRESS STATION",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                    Text(
                                        text = "Bay 02: High-Pressure Wash & Foam",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isTimerPaused) Color(0xFFFEF3C7) else Color(0xFFDCFCE7))
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = if (isTimerPaused) "PAUSED" else "RUNNING",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isTimerPaused) Color(0xFFD97706) else Color(0xFF16A34A)
                                    )
                                }
                            }

                            // Circular Progress Dial
                            Box(
                                modifier = Modifier.size(180.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val strokeWidth = 14.dp.toPx()
                                    drawCircle(
                                        color = Color(0xFFE2E8F0),
                                        style = Stroke(strokeWidth)
                                    )
                                    drawArc(
                                        color = Color(0xFF0284C7),
                                        startAngle = -90f,
                                        sweepAngle = (remainingSeconds / 1800f) * 360f,
                                        useCenter = false,
                                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                                    )
                                }

                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = "REMAINING",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = formatMinutesSeconds(remainingSeconds),
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "MINUTES : SECONDS",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                }
                            }

                            // Telemetry Stats Row
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Water Pressure", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(text = "140 Bar", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Active Foam", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(text = "Ready", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(text = "Rinse", fontSize = 10.sp, color = Color(0xFF64748B))
                                    Text(text = "Standby", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                                }
                            }

                            // Action Buttons Row: Pause Equipment & Extend Session
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Button(
                                    onClick = { isTimerPaused = !isTimerPaused },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isTimerPaused) Color(0xFFDCFCE7) else Color(0xFFFEF3C7),
                                        contentColor = if (isTimerPaused) Color(0xFF14532D) else Color(0xFF78350F)
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isTimerPaused) Icons.Default.PlayArrow else Icons.Default.Pause,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isTimerPaused) "Resume" else "Pause Equipment",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                Button(
                                    onClick = { showExtendModal = true },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF0284C7),
                                        contentColor = Color.White
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Outlined.AddAlarm,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Extend Session",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pause Notice Box
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                        border = BorderStroke(1.dp, Color(0xFFFDE68A))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Warning,
                                contentDescription = null,
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Important Notice:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF92400E)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pausing is strictly valid for up to 5 minutes. If pause is not resumed within 5 minutes, your bay session will automatically expire and remaining time will be forfeited.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF78350F),
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Extend Wash Session Pop-up Modal Window (TC-12, TC-13)
 */
@Composable
fun ExtendSessionModal(
    currentCountdownText: String,
    onDismiss: () -> Unit,
    onProceedToPayment: (ExtraDurationOption, String?) -> Unit
) {
    val extraOptions = remember {
        listOf(
            ExtraDurationOption(
                id = "ext_1",
                durationText = "+10 Minutes",
                minutesValue = 10,
                description = "Light rinse & detail",
                priceText = "MWK 4,500",
                runtimeBadge = "+10m runtime",
                price = 4500
            ),
            ExtraDurationOption(
                id = "ext_2",
                durationText = "+15 Minutes",
                minutesValue = 15,
                description = "Full body foam & wax",
                priceText = "MWK 9,500",
                runtimeBadge = "+15m runtime",
                isMostPopular = true,
                price = 9500
            ),
            ExtraDurationOption(
                id = "ext_3",
                durationText = "+25 Minutes",
                minutesValue = 25,
                description = "Intensive underbody & wax",
                priceText = "MWK 14,000",
                runtimeBadge = "+25m runtime",
                price = 14000
            )
        )
    }

    var selectedOptionId by remember { mutableStateOf("ext_2") }
    val selectedOption = extraOptions.find { it.id == selectedOptionId } ?: extraOptions[1]
    var conflictErrorMessage by remember { mutableStateOf<String?>(null) }

    // Check TC-13: Overlapping reservation check for proposed extension window
    val currentBooking = BookingRepository.getAllBookings().firstOrNull { it.id == "BK-101" }
    val proposedNewEndMs = (currentBooking?.endTimeMs ?: System.currentTimeMillis()) + selectedOption.minutesValue * 60 * 1000L

    val isConflicting = currentBooking != null && BookingRepository.hasOverlappingReservation(
        facilityId = currentBooking.facilityId,
        bayId = currentBooking.bayId,
        newStartMs = currentBooking.endTimeMs,
        newEndMs = proposedNewEndMs,
        excludeBookingId = currentBooking.id
    )

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Extend Wash Session ",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2EDFF))
                                .padding(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "Bay 02",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color(0xFF475569),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Text(
                    text = "Select additional minutes before automatic bay cutoff",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )

                // Active Countdown Box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "Active Countdown", fontSize = 12.sp, color = Color(0xFF475569))
                        }

                        Text(
                            text = "$currentCountdownText remaining",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                // Conflict Check Box (TC-13)
                if (isConflicting) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFFEF2F2))
                            .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Extension unavailable: This bay is reserved for a subsequent session.",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFDCFCE7))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Bay 02 is Available for Extension — No booking conflicts",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF14532D)
                            )
                        }
                    }
                }

                Text(
                    text = "SELECT EXTRA DURATION",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B),
                    letterSpacing = 0.5.sp
                )

                // Options List
                extraOptions.forEach { option ->
                    val isSelected = option.id == selectedOptionId

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) Color(0xFFE2EDFF) else Color.White)
                            .border(
                                1.5.dp,
                                if (isSelected) Color(0xFF0284C7) else Color(0xFFE2E8F0),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                selectedOptionId = option.id
                                conflictErrorMessage = null
                            }
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = {
                                        selectedOptionId = option.id
                                        conflictErrorMessage = null
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = option.durationText,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                        if (option.isMostPopular) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF0284C7))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "MOST POPULAR",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = option.description,
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = option.priceText,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = option.runtimeBadge,
                                    fontSize = 10.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Proceed to Payment Button
                Button(
                    onClick = {
                        if (isConflicting) {
                            conflictErrorMessage = "Extension unavailable: This bay is reserved for a subsequent session."
                        } else {
                            onProceedToPayment(selectedOption, null)
                        }
                    },
                    enabled = !isConflicting,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B132B),
                        contentColor = Color.White,
                        disabledContainerColor = Color(0xFFCBD5E1),
                        disabledContentColor = Color(0xFF64748B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = if (isConflicting) "EXTENSION UNAVAILABLE" else "PROCEED TO PAYMENT (${selectedOption.priceText})  ",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        if (!isConflicting) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Text(
                    text = "Cancel and Keep Current Time",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDismiss() }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookedSessionsScreenPreview() {
    MyApplicationTheme {
        BookedSessionsScreen()
    }
}
