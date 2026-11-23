package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.ApplicationStatus
import com.example.myapplication.data.OwnerApplication
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.AdminBottomNavigationBar
import com.example.myapplication.ui.components.AdminNavItem
import com.example.myapplication.ui.components.IwaleBrandTitle
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/** Admin screen where Car Wash Owner applications are reviewed, approved or rejected. */
@Composable
fun AdminOwnerApplicationsScreen(
    onBackClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onFacilitiesClick: () -> Unit = {},
    onAlertsClick: () -> Unit = {},
    onAccountClick: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var selectedTab by remember { mutableStateOf(ApplicationStatus.PENDING) }
    var isLoading by remember { mutableStateOf(true) }
    var busyEmail by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }
    var applicationToReject by remember { mutableStateOf<OwnerApplication?>(null) }

    val reviewer = UserRepository.getCurrentUser().email

    LaunchedEffect(Unit) {
        val result = OwnerApplicationRepository.refreshAll()
        if (result.isFailure) {
            message = result.exceptionOrNull()?.message
            isError = true
        }
        isLoading = false
    }

    val all = OwnerApplicationRepository.getAll()
    val counts = ApplicationStatus.entries.associateWith { s -> all.count { it.status == s } }
    val visible = all.filter { it.status == selectedTab }

    applicationToReject?.let { app ->
        RejectApplicationDialog(
            application = app,
            onCancel = { applicationToReject = null },
            onConfirm = { notes ->
                applicationToReject = null
                busyEmail = app.applicantEmail
                scope.launch {
                    val result = OwnerApplicationRepository.reject(app.applicantEmail, reviewer, notes)
                    busyEmail = null
                    isError = result.isFailure
                    message = if (result.isSuccess) "Application for ${app.businessName} rejected."
                    else result.exceptionOrNull()?.message
                }
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick, modifier = Modifier.testTag("applications_back")) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color(0xFF0F172A))
                }
                IwaleBrandTitle(title = "Owner Applications")
            }
        },
        bottomBar = {
            AdminBottomNavigationBar(
                selectedTab = AdminNavItem.DASHBOARD,
                onTabSelected = { tab ->
                    when (tab) {
                        AdminNavItem.DASHBOARD -> onDashboardClick()
                        AdminNavItem.FACILITIES -> onFacilitiesClick()
                        AdminNavItem.ALERTS -> onAlertsClick()
                        AdminNavItem.ACCOUNT -> onAccountClick()
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Tabs
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ApplicationStatus.entries.forEach { status ->
                    val selected = status == selectedTab
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (selected) Color(0xFF0284C7) else Color(0xFFE2E8F0))
                            .clickable { selectedTab = status }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .testTag("applications_tab_${status.name}")
                    ) {
                        Text(
                            text = "${status.label()} ${counts[status] ?: 0}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.White else Color(0xFF334155)
                        )
                    }
                }
            }

            if (message != null) {
                Surface(
                    color = if (isError) Color(0xFFFEF2F2) else Color(0xFFDCFCE7),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 8.dp)
                ) {
                    Text(
                        text = message ?: "",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isError) Color(0xFF991B1B) else Color(0xFF166534),
                        modifier = Modifier.padding(12.dp).testTag("applications_message")
                    )
                }
            }

            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            } else if (visible.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "No ${selectedTab.label().lowercase()} applications.",
                        color = Color(0xFF64748B),
                        fontSize = 14.sp,
                        modifier = Modifier.testTag("applications_empty")
                    )
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(visible, key = { it.applicantEmail }) { app ->
                        ApplicationCard(
                            application = app,
                            isBusy = busyEmail == app.applicantEmail,
                            onApprove = {
                                busyEmail = app.applicantEmail
                                scope.launch {
                                    val result = OwnerApplicationRepository.approve(app.applicantEmail, reviewer)
                                    busyEmail = null
                                    isError = result.isFailure
                                    message = if (result.isSuccess) "${app.businessName} approved. ${app.applicantName} is now a Car Wash Owner."
                                    else result.exceptionOrNull()?.message
                                }
                            },
                            onReject = { applicationToReject = app }
                        )
                    }
                }
            }
        }
    }
}

private fun ApplicationStatus.label() = when (this) {
    ApplicationStatus.PENDING -> "Pending"
    ApplicationStatus.APPROVED -> "Approved"
    ApplicationStatus.REJECTED -> "Rejected"
}

private fun formatDate(ms: Long): String {
    if (ms <= 0) return "-"
    return SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).apply {
        timeZone = TimeZone.getTimeZone("Africa/Blantyre")
    }.format(Date(ms))
}

@Composable
private fun ApplicationCard(
    application: OwnerApplication,
    isBusy: Boolean,
    onApprove: () -> Unit,
    onReject: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        modifier = Modifier.fillMaxWidth().testTag("application_card_${application.applicantEmail}")
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(application.businessName, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
            Text(
                "${application.applicantName} • ${application.applicantEmail}",
                fontSize = 12.sp, color = Color(0xFF475569)
            )
            if (application.phoneNumber.isNotBlank()) {
                Text("Phone: +265 ${application.phoneNumber}", fontSize = 12.sp, color = Color(0xFF475569))
            }
            Text(
                "${application.businessAddress}, ${application.city}",
                fontSize = 13.sp, color = Color(0xFF334155)
            )
            Text(
                buildString {
                    append("${application.numberOfBays} bay(s)")
                    if (application.experienceYears > 0) append(" • ${application.experienceYears} yr(s) in business")
                    if (application.registrationNumber.isNotBlank()) append(" • Reg. ${application.registrationNumber}")
                },
                fontSize = 12.sp, color = Color(0xFF64748B)
            )
            if (application.description.isNotBlank()) {
                Text(application.description, fontSize = 13.sp, color = Color(0xFF334155))
            }
            Text("Submitted ${formatDate(application.submittedAt)}", fontSize = 11.sp, color = Color(0xFF94A3B8))

            when (application.status) {
                ApplicationStatus.PENDING -> {
                    Spacer(Modifier.height(6.dp))
                    if (isBusy) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                        }
                    } else {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = onApprove,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("approve_${application.applicantEmail}")
                            ) { Text("Approve", fontWeight = FontWeight.Bold, color = Color.White) }
                            OutlinedButton(
                                onClick = onReject,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                                modifier = Modifier.weight(1f).height(44.dp).testTag("reject_${application.applicantEmail}")
                            ) { Text("Reject", fontWeight = FontWeight.Bold, color = Color(0xFFDC2626)) }
                        }
                    }
                }
                ApplicationStatus.APPROVED -> Text(
                    "Approved ${formatDate(application.reviewedAt)} by ${application.reviewedBy}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF16A34A)
                )
                ApplicationStatus.REJECTED -> Text(
                    "Rejected ${formatDate(application.reviewedAt)}: ${application.reviewNotes}",
                    fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFFDC2626)
                )
            }
        }
    }
}

@Composable
private fun RejectApplicationDialog(
    application: OwnerApplication,
    onCancel: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var notes by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onCancel) {
        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Reject application", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(
                    "Tell ${application.applicantName} what to correct. They can edit and resubmit.",
                    fontSize = 13.sp, color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { if (it.length <= 300) notes = it },
                    label = { Text("Reason *") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth().testTag("reject_notes")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Cancel") }
                    Button(
                        onClick = { onConfirm(notes) },
                        enabled = notes.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                        modifier = Modifier.weight(1f).testTag("reject_confirm")
                    ) { Text("Reject", color = Color.White) }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AdminOwnerApplicationsScreenPreview() {
    MyApplicationTheme { AdminOwnerApplicationsScreen() }
}
