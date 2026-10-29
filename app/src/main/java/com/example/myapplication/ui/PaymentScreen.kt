package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.BottomNavigationBar
import com.example.myapplication.ui.components.CarWashBayPlaceholder
import com.example.myapplication.ui.components.CarWashIcons
import com.example.myapplication.ui.components.NavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay

enum class MobilePaymentProvider(val title: String) {
    AIRTEL_MONEY("Airtel Money"),
    TNM_MPAMBA("TNM Mpamba"),
    IWALE_WALLET("Iwale Wallet")
}

@Composable
fun PaymentScreen(
    tier: CarWashTier = CarWashTier(
        id = "3",
        title = "40 Minutes- Deep Clean",
        description = "Full exterior wax, high-suction vacuum & mats blast",
        price = 15000,
        formattedPrice = "MWK 15,000"
    ),
    onBackClick: () -> Unit = {},
    onPaymentSuccess: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onBaysClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {}
) {
    var selectedProvider by remember { mutableStateOf(MobilePaymentProvider.AIRTEL_MONEY) }
    var airtelPhoneNumber by remember { mutableStateOf("991428903") }
    var tnmPhoneNumber by remember { mutableStateOf("881428903") }
    var isProcessingPayment by remember { mutableStateOf(false) }
    var isPaymentSuccess by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var bookingReferenceText by remember { mutableStateOf("") }

    val currentUser = UserRepository.getCurrentUser()
    val facility = FacilityRepository.getFacilityById("1") // Active booking facility
    val durationMins = when {
        tier.title.contains("10") -> 10
        tier.title.contains("20") -> 20
        tier.title.contains("40") -> 40
        else -> 60
    }

    fun validateAndProcessPayment() {
        errorMessage = null

        // Check if facility has available bays (TC-08)
        val availableBay = facility?.bays?.find { it.status == "Available" }
        if (facility == null || facility.bays.isEmpty() || availableBay == null) {
            errorMessage = "No bays are currently available at this facility."
            return
        }

        // Check Phone Number Length (TC-10)
        val activePhone = if (selectedProvider == MobilePaymentProvider.AIRTEL_MONEY) airtelPhoneNumber else tnmPhoneNumber
        val cleanPhone = activePhone.replace(" ", "").removePrefix("+265")
        if (cleanPhone.length != 9 || !cleanPhone.all { it.isDigit() }) {
            errorMessage = "Please enter a valid 9-digit Malawi phone number."
            return
        }

        // Check Overlapping Reservation (TC-09)
        val startTimeMs = System.currentTimeMillis()
        val endTimeMs = startTimeMs + durationMins * 60 * 1000L
        if (BookingRepository.hasOverlappingReservation(facility.id, availableBay.id, startTimeMs, endTimeMs)) {
            errorMessage = "This bay is already reserved for the selected time slot."
            return
        }

        isProcessingPayment = true
    }

    // Simulated USSD Processing Timer (TC-07, TC-10, TC-11)
    if (isProcessingPayment) {
        LaunchedEffect(Unit) {
            delay(2500)
            val availableBay = facility?.bays?.find { it.status == "Available" } ?: facility?.bays?.firstOrNull()
            val startTimeMs = System.currentTimeMillis()

            val result = BookingRepository.createBooking(
                facilityId = facility?.id ?: "1",
                bayId = availableBay?.id ?: "1_1",
                ownerId = facility?.ownerId ?: "owner@gmail.com",
                customerName = currentUser.fullName,
                customerEmail = currentUser.email,
                customerPhone = currentUser.phoneNumber,
                bayTitle = "${availableBay?.bayNumber ?: "Bay 01"} — ${tier.title}",
                amountValue = tier.price,
                paymentMethod = selectedProvider.title,
                paymentStatus = "SUCCESSFUL",
                durationMinutes = durationMins,
                startTimeMs = startTimeMs
            )

            isProcessingPayment = false
            if (result.isSuccess) {
                val record = result.getOrNull()
                bookingReferenceText = record?.bookingReference ?: "IW-8924"
                isPaymentSuccess = true
                delay(1500)
                onPaymentSuccess()
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Payment processing failed. Temporary hold released."
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
            // Error Message Banner
            item {
                AnimatedVisibility(visible = errorMessage != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }
            }

            // Success Message Banner (TC-07)
            item {
                AnimatedVisibility(visible = isPaymentSuccess) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFDCFCE7))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF16A34A),
                                modifier = Modifier.size(32.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Reservation Confirmed!",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF14532D)
                                )

                                Text(
                                    text = "Booking Reference: $bookingReferenceText. Your bay has been unlocked for ${tier.title}.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF166534)
                                )
                            }
                        }
                    }
                }
            }

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

            // Pay Button
            item {
                Button(
                    onClick = { validateAndProcessPayment() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B132B),
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
