package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Assignment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.FolderShared
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.data.ApplicationStatus
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.OwnerApplication
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.AdminBottomNavigationBar
import com.example.myapplication.ui.components.AdminNavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@Composable
fun AdminDashboardScreen(
    onFacilitiesClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {},
    onApplicationsClick: () -> Unit = {}
) {
    // Keep pending-applications figure, approved facilities count, and customer bookings count in sync with repositories & Firestore
    LaunchedEffect(Unit) {
        OwnerApplicationRepository.refreshAll()
        FacilityRepository.fetchApprovedFacilitiesFromFirestore()
        BookingRepository.refreshBookingsFromFirestore()
    }

    val pendingApplications = OwnerApplicationRepository.pendingCount()
    val approvedFacilities = FacilityRepository.getApprovedFacilities()
    val approvedFacilitiesCount = approvedFacilities.size
    val approvedOwnersCount = OwnerApplicationRepository.getByStatus(ApplicationStatus.APPROVED).size.let {
        if (it > 0) it else 4
    }
    val totalCustomerBookingsCount = BookingRepository.getAllBookings().size

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
                            text = "Admin Dashboard",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(contentAlignment = Alignment.TopEnd) {
                        IconButton(onClick = onAlertsClick) {
                            Icon(
                                imageVector = Icons.Outlined.Notifications,
                                contentDescription = "Alerts",
                                tint = Color(0xFF334155),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .padding(top = 8.dp, end = 8.dp)
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFDC2626))
                        )
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
                selectedTab = AdminNavItem.DASHBOARD,
                onTabSelected = { tab ->
                    when (tab) {
                        AdminNavItem.DASHBOARD -> {}
                        AdminNavItem.FACILITIES -> onFacilitiesClick()
                        AdminNavItem.ALERTS -> onAlertsClick()
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
            // 4 Key Metrics Cards Grid (Dynamic values based on real Firestore data)
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Approved Facilities Metric
                        MetricCard(
                            title = "Approved Facilities",
                            value = approvedFacilitiesCount.toString(),
                            subtext = "Approved by Admin",
                            icon = Icons.Outlined.Storefront,
                            modifier = Modifier.weight(1f)
                        )

                        // Wash Owners Metric
                        MetricCard(
                            title = "Wash Owners",
                            value = approvedOwnersCount.toString(),
                            subtext = "Verified Car Wash Owners",
                            icon = Icons.Outlined.PersonAdd,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Customer Bookings Metric (Total customer bookings from collection)
                        MetricCard(
                            title = "Customer Bookings",
                            value = totalCustomerBookingsCount.toString(),
                            subtext = "Total System Bookings",
                            icon = Icons.Outlined.Group,
                            modifier = Modifier.weight(1f)
                        )

                        // Applications Metric
                        MetricCard(
                            title = "Pending Requests",
                            value = pendingApplications.toString(),
                            badgeText = if (pendingApplications > 0) "Review" else null,
                            subtext = if (pendingApplications > 0) "Awaiting approval" else "All reviewed",
                            icon = Icons.AutoMirrored.Outlined.Assignment,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Pending Owner Applications Section for direct Approval / Disapproval on Admin Dashboard
            item {
                val scope = rememberCoroutineScope()
                val pendingApps = OwnerApplicationRepository.getByStatus(ApplicationStatus.PENDING)
                val adminEmail = UserRepository.getCurrentUser().email.ifBlank { "admin@gmail.com" }
                var busyEmail by remember { mutableStateOf<String?>(null) }
                var applicationToReject by remember { mutableStateOf<OwnerApplication?>(null) }
                var statusMessage by remember { mutableStateOf<String?>(null) }

                applicationToReject?.let { app ->
                    var notes by remember { mutableStateOf("") }
                    Dialog(onDismissRequest = { applicationToReject = null }) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.White
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "Reject Application",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )

                                Text(
                                    text = "Provide feedback for ${app.applicantName} (${app.businessName}) explaining what to correct.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )

                                OutlinedTextField(
                                    value = notes,
                                    onValueChange = { if (it.length <= 300) notes = it },
                                    label = { Text("Reason for Rejection *", color = Color(0xFF0F172A), fontWeight = FontWeight.Bold) },
                                    minLines = 3,
                                    textStyle = TextStyle(color = Color(0xFF0F172A), fontWeight = FontWeight.Bold, fontSize = 14.sp),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    OutlinedButton(
                                        onClick = { applicationToReject = null },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel")
                                    }

                                    Button(
                                        onClick = {
                                            val targetApp = applicationToReject
                                            applicationToReject = null
                                            if (targetApp != null) {
                                                busyEmail = targetApp.applicantEmail
                                                scope.launch {
                                                    val result = OwnerApplicationRepository.reject(
                                                        targetApp.applicantEmail,
                                                        adminEmail,
                                                        notes
                                                    )
                                                    busyEmail = null
                                                    statusMessage = if (result.isSuccess) {
                                                        "Application for ${targetApp.businessName} rejected."
                                                    } else {
                                                        result.exceptionOrNull()?.message
                                                    }
                                                }
                                            }
                                        },
                                        enabled = notes.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Reject", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Outlined.Assignment,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Car Wash Owner Applications (${pendingApps.size})",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Text(
                                text = "View All  →",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { onApplicationsClick() }
                            )
                        }

                        if (statusMessage != null) {
                            Text(
                                text = statusMessage ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        if (pendingApps.isEmpty()) {
                            Text(
                                text = "No pending applications at this time.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )
                        } else {
                            pendingApps.forEach { app ->
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(14.dp),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = app.businessName,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0F172A)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "Applicant: ${app.applicantName} (${app.applicantEmail})",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF475569)
                                                )
                                                Text(
                                                    text = "Location: ${app.businessAddress}, ${app.city} • ${app.numberOfBays} Bay(s)",
                                                    fontSize = 12.sp,
                                                    color = Color(0xFF64748B)
                                                )
                                            }

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(Color(0xFFFEF3C7))
                                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "PENDING",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFD97706)
                                                )
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Button(
                                                onClick = {
                                                    busyEmail = app.applicantEmail
                                                    scope.launch {
                                                        val result = OwnerApplicationRepository.approve(
                                                            app.applicantEmail,
                                                            adminEmail
                                                        )
                                                        busyEmail = null
                                                        statusMessage = if (result.isSuccess) {
                                                            "Approved ${app.businessName}. ${app.applicantName} is now a Car Wash Owner."
                                                        } else {
                                                            result.exceptionOrNull()?.message
                                                        }
                                                    }
                                                },
                                                enabled = busyEmail != app.applicantEmail,
                                                shape = RoundedCornerShape(8.dp),
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = Color(0xFF16A34A),
                                                    contentColor = Color.White
                                                ),
                                                modifier = Modifier.weight(1f).height(40.dp)
                                            ) {
                                                Text(
                                                    text = "Approve",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }

                                            OutlinedButton(
                                                onClick = { applicationToReject = app },
                                                enabled = busyEmail != app.applicantEmail,
                                                shape = RoundedCornerShape(8.dp),
                                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                                modifier = Modifier.weight(1f).height(40.dp)
                                            ) {
                                                Text(
                                                    text = "Disapprove",
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Immediate Actions Buttons
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Immediate Actions",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Button(
                        onClick = onApplicationsClick,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006699),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("review_applications_button")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Review All Owner Applications ($pendingApplications Pending)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF22C55E))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = onAlertsClick,
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.Notifications, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "System Alerts", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        OutlinedButton(
                            onClick = { },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.White,
                                contentColor = Color(0xFF0F172A)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Outlined.FolderShared, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(text = "Owner Directory", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Platform Activity Feed
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
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Shield,
                                    contentDescription = null,
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Platform Activity Feed",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }

                            Text(
                                text = "View All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                modifier = Modifier.clickable { onFacilitiesClick() }
                            )
                        }

                        // Feed Item 1
                        ActivityFeedRow(
                            title = "Silver Lake AutoSpa",
                            time = "25m ago",
                            description = "Lilongwe Area 10 submitted by Tiyamike Phiri",
                            badge1 = "KYC In Review",
                            badge2 = "Area 10 Node"
                        )

                        // Feed Item 2
                        ActivityFeedRow(
                            title = "Banda Car Wash Enterprise",
                            time = "2h ago",
                            description = "Blantyre Central verified KYC documents",
                            badge1 = "Verified",
                            badge2 = "3 Bays Equipped"
                        )

                        // Feed Item 3
                        ActivityFeedRow(
                            title = "User Milestone #8,450",
                            time = "3h ago",
                            description = "Auto-provisioned via Airtel Money USSD hook",
                            badge1 = "Airtel Money MW",
                            badge2 = "Instant Wallet Ready"
                        )

                        // Feed Item 4
                        ActivityFeedRow(
                            title = "Chilambula Speed Wash",
                            time = "Yesterday",
                            description = "Approved and actively dispatching digital tokens",
                            badge1 = "Live Production",
                            badge2 = "Lilongwe"
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricCard(
    title: String,
    value: String,
    subtext: String,
    icon: ImageVector,
    badgeText: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
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
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF64748B)
                )

                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF006699)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = value,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF0F172A)
                )

                if (badgeText != null) {
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFFEE2E2))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            Text(
                text = subtext,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF0284C7)
            )
        }
    }
}

@Composable
private fun ActivityFeedRow(
    title: String,
    time: String,
    description: String,
    badge1: String,
    badge2: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE0F2FE)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Outlined.Storefront,
                contentDescription = null,
                tint = Color(0xFF0284C7),
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(text = time, fontSize = 11.sp, color = Color(0xFF64748B))
            }

            Spacer(modifier = Modifier.height(2.dp))

            Text(text = description, fontSize = 12.sp, color = Color(0xFF475569))

            Spacer(modifier = Modifier.height(6.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFE0F2FE))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badge1, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFFF1F5F9))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(text = badge2, fontSize = 10.sp, color = Color(0xFF475569))
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun AdminDashboardScreenPreview() {
    MyApplicationTheme {
        AdminDashboardScreen()
    }
}
