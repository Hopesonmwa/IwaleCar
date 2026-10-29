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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import com.example.myapplication.ui.components.AdminBottomNavigationBar
import com.example.myapplication.ui.components.AdminNavItem
import com.example.myapplication.ui.theme.MyApplicationTheme

@Composable
fun AdminNotificationsScreen(
    onDashboardClick: () -> Unit = {},
    onFacilitiesClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    var selectedFilter by remember { mutableStateOf("All 12") }
    var unreadCount by remember { mutableStateOf(2) }

    val filterChips = listOf("All 12", "Unread 2", "Applications", "Registrations")

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
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "IWALE CAR ",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                letterSpacing = 0.5.sp
                            )
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFDBF2FE))
                                    .padding(horizontal = 6.dp, vertical = 1.dp)
                            ) {
                                Text(
                                    text = "ADMIN",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }
                        Text(
                            text = "Admin Alerts",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = {}) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .padding(top = 8.dp, end = 8.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFDC2626))
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF0B132B))
                            .clickable { onAccountClick() },
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
            AdminBottomNavigationBar(
                selectedTab = AdminNavItem.ALERTS,
                onTabSelected = { tab ->
                    when (tab) {
                        AdminNavItem.DASHBOARD -> onDashboardClick()
                        AdminNavItem.FACILITIES -> onFacilitiesClick()
                        AdminNavItem.ALERTS -> {}
                        AdminNavItem.ACCOUNT -> onAccountClick()
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
            // Header Row: Admin Notifications + Unread Pill + Mark All Read
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Admin\nNotifications",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            lineHeight = 26.sp
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        if (unreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF006699))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "$unreadCount Unread",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEBF2FF))
                            .clickable { unreadCount = 0 }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Mark All\nRead", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7), lineHeight = 13.sp)
                        }
                    }
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(filterChips) { filter ->
                        val isSelected = filter == selectedFilter
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) Color(0xFF006699) else Color(0xFFEBF2FF))
                                .clickable { selectedFilter = filter }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = filter,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color(0xFF334155)
                            )
                        }
                    }
                }
            }

            // TODAY Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = "TODAY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                    }

                    Text(text = "4 Updates", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            // Notification 1: New Facility Application
            item {
                AdminNotificationCard(
                    category = "APPLICATIONS",
                    timeAgo = "22 mins ago",
                    isUnread = unreadCount > 0,
                    title = "New Facility Application Submitted",
                    body = "Silver Lake AutoSpa (Lilongwe) submitted documentation and photos for 4 self-service bays awaiting station compliance review.",
                    chipText = "Area 47, Lilongwe",
                    actionButtonText = "Review Application",
                    icon = Icons.Outlined.Storefront,
                    onActionClick = onFacilitiesClick
                )
            }

            // Notification 2: New Car Wash Owner Registered
            item {
                AdminNotificationCard(
                    category = "REGISTRATIONS",
                    timeAgo = "1 hour ago",
                    isUnread = unreadCount > 0,
                    title = "New Car Wash Owner Registered",
                    body = "Kondwani Mwale registered Capital City Wash Hub. Airtel Money merchant KYC submitted for automated disbursements.",
                    chipText = "Airtel KYC Ready",
                    actionButtonText = "View Owner",
                    icon = Icons.Outlined.PersonAdd,
                    onActionClick = { }
                )
            }

            // Notification 3: Facility Inspection Complete
            item {
                AdminNotificationCard(
                    category = "APPROVALS",
                    timeAgo = "5 hours ago",
                    isUnread = false,
                    title = "Facility Inspection Complete",
                    body = "Area 4 Express Station completed pressure certification and added 2 new high-pressure lances ready for customer booking.",
                    chipText = "+2 Lances Configured",
                    actionButtonText = "View Facility",
                    icon = Icons.Outlined.Settings,
                    onActionClick = onFacilitiesClick
                )
            }

            // EARLIER THIS WEEK Section
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "EARLIER THIS WEEK", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    Text(text = "2 Updates", fontSize = 12.sp, color = Color(0xFF64748B))
                }
            }

            // Notification 4: Application Approved
            item {
                AdminNotificationCard(
                    category = "APPLICATIONS",
                    timeAgo = "Yesterday",
                    isUnread = false,
                    statusBadgeText = "Resolved",
                    title = "Application Approved",
                    body = "Chilambula Speed Wash status changed to Approved. Facility is now live and accepting customer reservations.",
                    chipText = "Live on Customer App",
                    actionButtonText = "Details",
                    icon = Icons.Outlined.CheckCircle,
                    onActionClick = { }
                )
            }
        }
    }
}

@Composable
private fun AdminNotificationCard(
    category: String,
    timeAgo: String,
    isUnread: Boolean,
    title: String,
    body: String,
    chipText: String,
    actionButtonText: String,
    icon: ImageVector,
    statusBadgeText: String? = null,
    onActionClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = icon, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(text = category, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "• $timeAgo", fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (statusBadgeText != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFFF1F5F9))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(text = statusBadgeText, fontSize = 10.sp, color = Color(0xFF64748B))
                        }
                    }

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0284C7))
                        )
                    }
                }
            }

            Text(text = title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))

            Text(text = body, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(imageVector = Icons.Outlined.LocationOn, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = chipText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                }

                Button(
                    onClick = onActionClick,
                    shape = RoundedCornerShape(8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B132B),
                        contentColor = Color.White
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = actionButtonText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminNotificationsScreenPreview() {
    MyApplicationTheme {
        AdminNotificationsScreen()
    }
}
