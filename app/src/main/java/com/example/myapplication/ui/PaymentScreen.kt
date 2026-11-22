package com.example.myapplication.ui

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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.data.BadgeType
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.CarWashTier
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.FirebaseServices
import com.example.myapplication.data.NetworkUtils
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.BottomNavigationBar
import com.example.myapplication.ui.components.CarWashBayPlaceholder
import com.example.myapplication.ui.components.CarWashIcons
import com.example.myapplication.ui.components.NavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay
import java.util.UUID

enum class MobilePaymentProvider(val title: String) {
    AIRTEL_MONEY("Airtel Money"),
    TNM_MPAMBA("TNM Mpamba"),
    IWALE_WALLET("Iwale Wallet")
}

private var simulatedPaymentAttemptCounter = 0

@Composable
fun PaymentScreen(
    tier: CarWashTier = CarWashTier(
        id = "3",
        title = "40 Minutes- Deep Clean",
        description = "Full exterior wax, high-suction vacuum & mats blast",
        price = 15000,
        formattedPrice = "MWK 15,000"
    ),
    bayId: String = "1_1",
    startTimeMs: Long = System.currentTimeMillis() + 3600_000L,
    endTimeMs: Long = System.currentTimeMillis() + 5400_000L,
    onBackClick: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onBaysClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {}
) {
    val context = LocalContext.current
    var selectedProvider by remember { mutableStateOf(MobilePaymentProvider.AIRTEL_MONEY) }
    var airtelPhoneNumber by remember { mutableStateOf("991428903") }
    var tnmPhoneNumber by remember { mutableStateOf("881428903") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var isPaymentSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var bookingReferenceText by remember { mutableStateOf("") }

    val currentUser = UserRepository.getCurrentUser()
    val facility = FacilityRepository.getFacilityById("1") // Active booking facility
    val durationMins = maxOf(1, ((endTimeMs - startTimeMs) / (60 * 1000L)).toInt())
    val targetBay = facility?.bays?.find { it.id == bayId } ?: facility?.bays?.firstOrNull()

    fun validateAndProcessPayment() {
        errorMessage = null

        // 1. Internet connection requirement check
        if (!NetworkUtils.isNetworkAvailable(context)) {
            errorMessage = "No internet connection. Payment completion requires an active internet connection."
            return
        }

        if (facility == null || facility.bays.isEmpty() || targetBay == null) {
            errorMessage = "Selected washing bay is unavailable."
            return
        }

        // Check Phone Number Length
        val activePhone = if (selectedProvider == MobilePaymentProvider.AIRTEL_MONEY) airtelPhoneNumber else tnmPhoneNumber
        val cleanPhone = activePhone.replace(" ", "").removePrefix("+265")
        if (cleanPhone.length != 9 || !cleanPhone.all { it.isDigit() }) {
            errorMessage = "Please enter a valid 9-digit Malawi phone number."
            return
        }

        // Check Overlapping Reservation
        if (BookingRepository.hasOverlappingReservation(facility.id, targetBay.id, startTimeMs, endTimeMs)) {
            errorMessage = "This bay is already reserved for the selected time slot."
            return
        }

        isProcessingPayment = true
    }

    // Success Pop-Up Overlay Dialog (Requirement 3)
    if (isPaymentSuccess) {
        Dialog(onDismissRequest = { onPaymentSuccess() }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Payment Successful",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "Your payment was a success. You're session has successfully been booked",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = onPaymentSuccess,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "View Booked Session", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Failure Pop-Up Overlay Dialog (Requirement 3)
    if (errorMessage != null && !isProcessingPayment && !isPaymentSuccess) {
        Dialog(onDismissRequest = { errorMessage = null }) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEE2E2)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Text(
                        text = "Payment Failed",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Text(
                        text = "The payment process run into an error please try again",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF0F172A),
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )

                    Button(
                        onClick = { errorMessage = null },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Text(text = "Try Again", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }

    // Simulated USSD Processing with 1 among 3 payments successful rule & Firestore payment log
    if (isProcessingPayment) {
        LaunchedEffect(Unit) {
            delay(2200)

            simulatedPaymentAttemptCounter++
            val currentAttemptNumber = simulatedPaymentAttemptCounter
            // 1 among 3 payments successful rule: attempt 1 is successful, attempts 2 & 3 fail
            val isSuccessSimulation = (currentAttemptNumber % 3 == 1)

            val activePhone = if (selectedProvider == MobilePaymentProvider.AIRTEL_MONEY) airtelPhoneNumber else tnmPhoneNumber
            val paymentId = "PAY_${UUID.randomUUID()}"

            if (isSuccessSimulation) {
                val result = BookingRepository.createBooking(
                    facilityId = facility?.id ?: "1",
                    bayId = targetBay?.id ?: "1_1",
                    ownerId = facility?.ownerId ?: "owner@gmail.com",
                    customerName = currentUser.fullName,
                    customerEmail = currentUser.email,
                    customerPhone = currentUser.phoneNumber,
                    bayTitle = "${targetBay?.bayNumber ?: "Bay 01"} — ${tier.title}",
                    amountValue = tier.price,
                    paymentMethod = selectedProvider.title,
                    paymentStatus = "SUCCESSFUL",
                    durationMinutes = durationMins,
                    startTimeMs = startTimeMs
                )

                val record = result.getOrNull()
                bookingReferenceText = record?.bookingReference ?: "IW-8924"

                // Save SUCCESSFUL payment record to Firestore collection "payments"
                try {
                    val customerUid = try { FirebaseServices.auth.currentUser?.uid ?: "" } catch (e: Throwable) { "" }
                    val paymentRecord = hashMapOf<String, Any>(
                        "paymentId" to paymentId,
                        "bookingId" to (record?.id ?: "BK-101"),
                        "bookingReference" to bookingReferenceText,
                        "facilityId" to (facility?.id ?: "1"),
                        "customerId" to customerUid,
                        "customerEmail" to currentUser.email,
                        "customerName" to currentUser.fullName,
                        "amountValue" to tier.price,
                        "amountFormatted" to tier.formattedPrice,
                        "paymentMethod" to selectedProvider.title,
                        "phoneNumber" to activePhone,
                        "status" to "SUCCESSFUL",
                        "failureReason" to "",
                        "simulationAttempt" to currentAttemptNumber,
                        "timestampMs" to System.currentTimeMillis()
                    )
                    FirebaseServices.db.collection("payments").document(paymentId).set(paymentRecord)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                isProcessingPayment = false
                isPaymentSuccess = true
            } else {
                // Save FAILED payment record to Firestore collection "payments"
                val failureMessage = "The payment process run into an error please try again"
                try {
                    val customerUid = try { FirebaseServices.auth.currentUser?.uid ?: "" } catch (e: Throwable) { "" }
                    val paymentRecord = hashMapOf<String, Any>(
                        "paymentId" to paymentId,
                        "bookingId" to "BK-FAILED",
                        "bookingReference" to "IW-FAILED",
                        "facilityId" to (facility?.id ?: "1"),
                        "customerId" to customerUid,
                        "customerEmail" to currentUser.email,
                        "customerName" to currentUser.fullName,
                        "amountValue" to tier.price,
                        "amountFormatted" to tier.formattedPrice,
                        "paymentMethod" to selectedProvider.title,
                        "phoneNumber" to activePhone,
                        "status" to "FAILED",
                        "failureReason" to failureMessage,
                        "simulationAttempt" to currentAttemptNumber,
                        "timestampMs" to System.currentTimeMillis()
                    )
                    FirebaseServices.db.collection("payments").document(paymentId).set(paymentRecord)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                isProcessingPayment = false
                errorMessage = failureMessage
            }
        }

        Dialog(onDismissRequest = {}) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF0284C7),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(48.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = "Processing Payment...",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Please confirm the ${selectedProvider.title} prompt on your phone to complete ${tier.formattedPrice}.",
                        fontSize = 13.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }

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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Payment Page",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
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
                        imageVector = CarWashIcons.Person,
                        contentDescription = "Profile",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        bottomBar = {
            BottomNavigationBar(
                selectedTab = NavItem.BAYS,
                onTabSelected = { tab ->
                    when (tab) {
                        NavItem.BAYS -> onBaysClick()
                        NavItem.HISTORY -> onHistoryClick()
                        NavItem.BOOKINGS -> onBookingsClick()
                        NavItem.PROFILE -> onProfileClick()
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Facility & Order Summary Card
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
                        Column {
                            Text(
                                text = facility?.name ?: "Capital Sparkle Car Wash",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = CarWashIcons.LocationPin,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = facility?.address ?: "Area 3, Lilongwe",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        // Banner Placeholder
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(130.dp)
                                .clip(RoundedCornerShape(12.dp))
                        ) {
                            CarWashBayPlaceholder(badgeType = BadgeType.HIGH_PRESSURE_JET)
                        }

                        // Wash Duration Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFEBF2FF))
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                        ) {
                            Column {
                                Text(
                                    text = "Wash Duration",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$durationMins Minutes",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }

                        // Total Amount Due Box
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFE2EDFF))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Total Amount Due",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF334155)
                                )

                                Text(
                                    text = tier.formattedPrice,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            // Payment Method Section
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
                        Column {
                            Text(
                                text = "Payment Method",
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Select Mobile Payment Method",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }

                        // Airtel Money Option Card
                        val isAirtelSelected = selectedProvider == MobilePaymentProvider.AIRTEL_MONEY
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isAirtelSelected) Color(0xFFEBF2FF) else Color(0xFFF8FAFC))
                                .clickable { selectedProvider = MobilePaymentProvider.AIRTEL_MONEY }
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AirtelLogo()

                                    // Radio dot indicator
                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isAirtelSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isAirtelSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }

                                if (isAirtelSelected) {
                                    Text(
                                        text = "Airtel Phone Number (+265)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "+265 ",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )

                                            BasicTextField(
                                                value = airtelPhoneNumber,
                                                onValueChange = { input ->
                                                    if (input.length <= 9 && input.all { it.isDigit() }) {
                                                        airtelPhoneNumber = input
                                                        errorMessage = null
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                textStyle = TextStyle(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // TNM Mpamba Option Card (TC-10)
                        val isMpambaSelected = selectedProvider == MobilePaymentProvider.TNM_MPAMBA
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isMpambaSelected) Color(0xFFEBF2FF) else Color(0xFFF8FAFC))
                                .clickable { selectedProvider = MobilePaymentProvider.TNM_MPAMBA }
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        MpambaLogo()
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Text(
                                            text = "Pay with TNM Mpamba wallet",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color(0xFF334155)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .size(20.dp)
                                            .clip(CircleShape)
                                            .background(if (isMpambaSelected) Color(0xFF2563EB) else Color(0xFFCBD5E1)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isMpambaSelected) {
                                            Box(
                                                modifier = Modifier
                                                    .size(8.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.White)
                                            )
                                        }
                                    }
                                }

                                if (isMpambaSelected) {
                                    Text(
                                        text = "TNM Mpamba Phone Number (+265)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(Color.White)
                                            .padding(horizontal = 14.dp, vertical = 12.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "+265 ",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A)
                                            )

                                            BasicTextField(
                                                value = tnmPhoneNumber,
                                                onValueChange = { input ->
                                                    if (input.length <= 9 && input.all { it.isDigit() }) {
                                                        tnmPhoneNumber = input
                                                        errorMessage = null
                                                    }
                                                },
                                                singleLine = true,
                                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                                textStyle = TextStyle(
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFF0F172A)
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Prompt Instruction Banner
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(14.dp)
                ) {
                    Text(
                        text = "Your phone will prompt for your mobile money PIN to confirm ${tier.formattedPrice}.",
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        lineHeight = 18.sp
                    )
                }
            }

            // Pay Button (Unified Brand Blue Color)
            item {
                Button(
                    onClick = { validateAndProcessPayment() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PrimaryBlue,
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Pay ${tier.formattedPrice}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun AirtelLogo() {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(Color(0xFFE11D48)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "airtel",
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
fun MpambaLogo() {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(Color(0xFF22C55E)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.AccountBalanceWallet,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun PaymentScreenPreview() {
    MyApplicationTheme {
        PaymentScreen()
    }
}
