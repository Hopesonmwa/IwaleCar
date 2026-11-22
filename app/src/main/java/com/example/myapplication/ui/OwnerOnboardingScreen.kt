package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BusinessCenter
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.HourglassTop
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.ApplicationStatus
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.theme.PrimaryBlue
import com.example.myapplication.ui.theme.PrimaryBlueLight

@Composable
fun OwnerOnboardingScreen(
    onContinueToCustomerClick: () -> Unit = {},
    onApprovedContinueClick: () -> Unit = {},
    onSignOutClick: () -> Unit = {},
    onOpenApplicationFormClick: () -> Unit = {}
) {
    val currentUser = UserRepository.getCurrentUser()
    LaunchedEffect(currentUser.email) {
        if (currentUser.email.isNotBlank()) {
            OwnerApplicationRepository.refreshOwn(currentUser.email)
        }
    }
    val application = OwnerApplicationRepository.getApplicationFor(currentUser.email)

    var popupDismissed by remember { mutableStateOf(false) }
    var rejectionDialogDismissed by remember { mutableStateOf(false) }
    var resubmitOpen by remember { mutableStateOf(false) }

    // Automatic Denial Pop-Up Dialog when applicant opens onboarding and their application was rejected
    if (application?.status == ApplicationStatus.REJECTED && !rejectionDialogDismissed) {
        Dialog(onDismissRequest = { rejectionDialogDismissed = true }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFFFEE2E2), shape = CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Text(
                        text = "Application Denied",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Your application for \"${application.businessName}\" has been denied by the administrator." +
                                if (!application.reviewNotes.isBlank()) "\n\nReason: ${application.reviewNotes}" else "",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "Would you like to re-apply with updated details or continue with the customer experience?",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            rejectionDialogDismissed = true
                            onOpenApplicationFormClick()
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Re-apply", fontWeight = FontWeight.Bold, color = Color.White)
                    }

                    OutlinedButton(
                        onClick = {
                            rejectionDialogDismissed = true
                            onContinueToCustomerClick()
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, Color(0xFF0284C7)),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Continue with Customer Experience", fontWeight = FontWeight.SemiBold, color = Color(0xFF0284C7))
                    }
                }
            }
        }
    }

    if (resubmitOpen && application?.status == ApplicationStatus.REJECTED) {
        OwnerApplicationDialog(
            applicant = currentUser,
            previous = application,
            onDismiss = { resubmitOpen = false },
            onSubmitted = { resubmitOpen = false }
        )
    }

    val icon: ImageVector
    val iconTint: Color
    val title: String
    val message: String
    when (application?.status) {
        ApplicationStatus.PENDING -> {
            icon = Icons.Outlined.HourglassTop; iconTint = Color(0xFFD97706)
            title = "Application Under Review"
            message = "Thanks, ${currentUser.fullName}! Your application for \"${application?.businessName}\" " +
                    "has been submitted. An administrator will review it shortly."
        }
        ApplicationStatus.APPROVED -> {
            icon = Icons.Outlined.CheckCircle; iconTint = Color(0xFF16A34A)
            title = "Application Approved"
            message = "Congratulations! \"${application?.businessName}\" has been approved. " +
                    "You can now access your Car Wash Owner dashboard."
        }
        ApplicationStatus.REJECTED -> {
            icon = Icons.Outlined.ErrorOutline; iconTint = Color(0xFFDC2626)
            title = "Application Denied"
            message = "Your application for \"${application?.businessName}\" was not approved." +
                    if (!application?.reviewNotes.isNullOrBlank()) " Reviewer notes: ${application?.reviewNotes}" else ""
        }
        null -> {
            icon = Icons.Outlined.BusinessCenter; iconTint = PrimaryBlue
            title = "Car Wash Owner Verification"
            message = "Welcome, ${currentUser.fullName}! To become a verified Car Wash Owner, " +
                    "please complete your detailed business application."
        }
    }

    Scaffold(containerColor = Color.White) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(PrimaryBlueLight, shape = RoundedCornerShape(40.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconTint,
                    modifier = Modifier.size(40.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1F2937),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_title")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = message,
                fontSize = 15.sp,
                color = Color(0xFF4B5563),
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag("onboarding_message")
            )

            Spacer(modifier = Modifier.height(24.dp))

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF3F4F6)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = "Notice",
                            tint = PrimaryBlue,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Account Status: " +
                                    if (application?.status == ApplicationStatus.APPROVED) "Car Wash Owner" else "Customer",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            color = Color(0xFF1F2937)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Until your application is approved you have Customer access only. " +
                                "You can keep using the app as a customer in the meantime.",
                        fontSize = 13.sp,
                        color = Color(0xFF6B7280)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            when (application?.status) {
                null -> PrimaryAction("Complete Full Owner Application", "onboarding_apply") { onOpenApplicationFormClick() }
                ApplicationStatus.REJECTED -> PrimaryAction("Update & Resubmit Application", "onboarding_resubmit") { onOpenApplicationFormClick() }
                ApplicationStatus.APPROVED -> PrimaryAction("Go to Owner Dashboard", "onboarding_approved") { onApprovedContinueClick() }
                ApplicationStatus.PENDING -> Unit
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onContinueToCustomerClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_continue_customer"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Continue to Customer App",
                    color = PrimaryBlue,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedButton(
                onClick = onSignOutClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("onboarding_signout"),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Sign Out",
                    color = Color(0xFFEF4444),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

@Composable
private fun PrimaryAction(label: String, tag: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .testTag(tag),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
    ) {
        Text(
            text = label,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
