package com.example.myapplication.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.OwnerBottomNavigationBar
import com.example.myapplication.ui.components.OwnerNavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.NumberFormat

private data class RevenueSummaryData(
    val title: String,
    val amount: String,
    val subtitle: String,
    val icon: ImageVector
)

private data class RevenueBarItem(
    val label: String,
    val amountText: String,
    val fraction: Float,
    val isCurrent: Boolean = false
)

private data class RevenueTrendData(
    val subtitle: String,
    val totalText: String,
    val bars: List<RevenueBarItem>
)

private data class VerifiedSessionsData(
    val total: Int,
    val successful: Int,
    val failed: Int
)

private data class FacilityRevenueData(
    val summary: RevenueSummaryData,
    val trend: RevenueTrendData,
    val sessions: VerifiedSessionsData
)

private fun getRevenueDataForFacilityAndTimeRange(
    facilityId: String,
    facilityName: String,
    timeRange: String,
    ownerEmail: String
): FacilityRevenueData {
    val allFacilityBookings = BookingRepository.getBookingsForFacilityAndOwner(facilityId, ownerEmail)
    val paidBookings = allFacilityBookings.filter {
        it.paymentStatus == "SUCCESSFUL" || it.status.equals("Completed", ignoreCase = true) || it.status.equals("In Progress", ignoreCase = true)
    }

    val totalPaidAmount = paidBookings.sumOf { it.amountValue }
    val totalSessionsCount = allFacilityBookings.size
    val successfulSessionsCount = paidBookings.size
    val failedSessionsCount = allFacilityBookings.count { it.paymentStatus == "FAILED" || it.status.equals("Cancelled", ignoreCase = true) }

    val formattedTotalAmount = "MK " + NumberFormat.getNumberInstance().format(totalPaidAmount)

    val bars: List<RevenueBarItem> = when (timeRange) {
        "Today" -> {
            val slot1 = paidBookings.filter { it.timeSlotText.contains("08:") || it.timeSlotText.contains("09:") }.sumOf { it.amountValue }
            val slot2 = paidBookings.filter { it.timeSlotText.contains("10:") || it.timeSlotText.contains("11:") }.sumOf { it.amountValue }
            val slot3 = paidBookings.filter { it.timeSlotText.contains("12:") || it.timeSlotText.contains("13:") }.sumOf { it.amountValue }
            val slot4 = paidBookings.filter { it.timeSlotText.contains("14:") || it.timeSlotText.contains("15:") }.sumOf { it.amountValue }
            val slot5 = paidBookings.filter { it.timeSlotText.contains("16:") || it.timeSlotText.contains("17:") || it.timeSlotText.contains("18:") }.sumOf { it.amountValue }

            val maxSlot = maxOf(slot1, slot2, slot3, slot4, slot5, 1)
            listOf(
                RevenueBarItem("16:00 – 18:00 (Current)", "MK " + NumberFormat.getNumberInstance().format(slot5), (slot5.toFloat() / maxSlot).coerceIn(0f, 1f), isCurrent = true),
                RevenueBarItem("14:00 – 16:00", "MK " + NumberFormat.getNumberInstance().format(slot4), (slot4.toFloat() / maxSlot).coerceIn(0f, 1f)),
                RevenueBarItem("12:00 – 14:00", "MK " + NumberFormat.getNumberInstance().format(slot3), (slot3.toFloat() / maxSlot).coerceIn(0f, 1f)),
                RevenueBarItem("10:00 – 12:00", "MK " + NumberFormat.getNumberInstance().format(slot2), (slot2.toFloat() / maxSlot).coerceIn(0f, 1f)),
                RevenueBarItem("08:00 – 10:00", "MK " + NumberFormat.getNumberInstance().format(slot1), (slot1.toFloat() / maxSlot).coerceIn(0f, 1f))
            )
        }
        "This Week" -> {
            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            val dayAmounts = days.associateWith { dayName ->
                paidBookings.filter { it.timeSlotText.contains(dayName, ignoreCase = true) || it.slotDetailText.contains(dayName, ignoreCase = true) }.sumOf { it.amountValue }
            }
            val maxDay = dayAmounts.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            days.map { day ->
                val amt = dayAmounts[day] ?: 0
                RevenueBarItem(day, "MK " + NumberFormat.getNumberInstance().format(amt), (amt.toFloat() / maxDay).coerceIn(0f, 1f), isCurrent = (day == "Fri"))
            }
        }
        else -> {
            val week1 = paidBookings.take(maxOf(1, paidBookings.size / 4)).sumOf { it.amountValue }
            val week2 = paidBookings.drop(paidBookings.size / 4).take(maxOf(1, paidBookings.size / 4)).sumOf { it.amountValue }
            val week3 = paidBookings.drop(paidBookings.size / 2).take(maxOf(1, paidBookings.size / 4)).sumOf { it.amountValue }
            val week4 = paidBookings.drop(paidBookings.size * 3 / 4).sumOf { it.amountValue }

            val maxWeek = maxOf(week1, week2, week3, week4, 1)
            listOf(
                RevenueBarItem("Week 4 (Current)", "MK " + NumberFormat.getNumberInstance().format(week4), (week4.toFloat() / maxWeek).coerceIn(0f, 1f), isCurrent = true),
                RevenueBarItem("Week 3", "MK " + NumberFormat.getNumberInstance().format(week3), (week3.toFloat() / maxWeek).coerceIn(0f, 1f)),
                RevenueBarItem("Week 2", "MK " + NumberFormat.getNumberInstance().format(week2), (week2.toFloat() / maxWeek).coerceIn(0f, 1f)),
                RevenueBarItem("Week 1", "MK " + NumberFormat.getNumberInstance().format(week1), (week1.toFloat() / maxWeek).coerceIn(0f, 1f))
            )
        }
    }

    return FacilityRevenueData(
        summary = RevenueSummaryData(
            title = "$timeRange Revenue Total",
            amount = formattedTotalAmount,
            subtitle = "$successfulSessionsCount paid car wash sessions so far",
            icon = Icons.Outlined.BarChart
        ),
        trend = RevenueTrendData(
            subtitle = "$facilityName — $timeRange Real Paid Inflow",
            totalText = "Total: $formattedTotalAmount",
            bars = bars
        ),
        sessions = VerifiedSessionsData(
            total = totalSessionsCount,
            successful = successfulSessionsCount,
            failed = failedSessionsCount
        )
    )
}

@Composable
fun OwnerRevenueScreen(
    onDashboardClick: () -> Unit = {},
    onFacilitiesClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val currentUser = UserRepository.getCurrentUser()
    val ownerFacilities = FacilityRepository.getFacilitiesForOwner(currentUser.email)
    val selectedFacility = FacilityRepository.getSelectedFacilityForOwner(currentUser.email)

    var isFacilityDropdownExpanded by remember { mutableStateOf(false) }
    var selectedTimeRange by remember { mutableStateOf("This Month") }

    val timeRanges = listOf("Today", "This Week", "This Month")

    val facilityId = selectedFacility?.id ?: ""
    val facilityName = selectedFacility?.name ?: "No Facility Selected"

    val revenueData = getRevenueDataForFacilityAndTimeRange(
        facilityId = facilityId,
        facilityName = facilityName,
        timeRange = selectedTimeRange,
        ownerEmail = currentUser.email
    )

    val summaryData = revenueData.summary
    val trendData = revenueData.trend
    val sessionsData = revenueData.sessions

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFF8FAFC))
                    .statusBarsPadding()
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
                            text = "Revenue",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onNotificationsClick) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = "Notifications",
                            tint = Color(0xFF334155),
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

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
            }
        },
        bottomBar = {
            OwnerBottomNavigationBar(
                selectedTab = OwnerNavItem.REVENUE,
                onTabSelected = { tab ->
                    when (tab) {
                        OwnerNavItem.DASHBOARD -> onDashboardClick()
                        OwnerNavItem.FACILITIES -> onFacilitiesClick()
                        OwnerNavItem.BOOKINGS -> onBookingsClick()
                        OwnerNavItem.REVENUE -> {}
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
            // Selected Facility Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEBF2FF))
                        .clickable { isFacilityDropdownExpanded = true }
                        .padding(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = facilityName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Icon(
                            imageVector = Icons.Outlined.ArrowDropDown,
                            contentDescription = null,
                            tint = Color(0xFF64748B)
                        )
                    }

                    DropdownMenu(
                        expanded = isFacilityDropdownExpanded,
                        onDismissRequest = { isFacilityDropdownExpanded = false }
                    ) {
                        ownerFacilities.forEach { fac ->
                            DropdownMenuItem(
                                text = { Text(fac.name, fontSize = 13.sp) },
                                onClick = {
                                    FacilityRepository.setSelectedFacilityId(fac.id)
                                    isFacilityDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // Time Range Switcher Bar
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    timeRanges.forEach { range ->
                        val isSelected = range == selectedTimeRange
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) Color(0xFF006699) else Color.Transparent)
                                .clickable { selectedTimeRange = range }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = range,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // Revenue Summary Card for Selected Period & Facility
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = summaryData.title, fontSize = 12.sp, color = Color(0xFF64748B))
                            Icon(
                                imageVector = summaryData.icon,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Text(
                            text = summaryData.amount,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = summaryData.subtitle,
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }
            }

            // Revenue Trend Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Revenue Trend",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = trendData.subtitle,
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFEBF2FF))
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = trendData.totalText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }

                        // Horizontal Progress Bars
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            trendData.bars.forEach { bar ->
                                RevenueBarRow(
                                    weekLabel = bar.label,
                                    amountText = bar.amountText,
                                    fraction = bar.fraction,
                                    barColor = if (bar.isCurrent) Color(0xFF006699) else Color(0xFF0B132B)
                                )
                            }
                        }
                    }
                }
            }

            // Verified Sessions Card
            item {
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Verified Sessions",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = "${sessionsData.total} Total",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "All: ${sessionsData.total}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1.2f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Successful: ${sessionsData.successful}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(text = "Failed: ${sessionsData.failed}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevenueBarRow(
    weekLabel: String,
    amountText: String,
    fraction: Float,
    barColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = weekLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(text = amountText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OwnerRevenueScreenPreview() {
    MyApplicationTheme {
        OwnerRevenueScreen()
    }
}
