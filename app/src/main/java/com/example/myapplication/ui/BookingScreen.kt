package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.BookingTimeUtils
import com.example.myapplication.data.CarWashFacility
import com.example.myapplication.data.CarWashTier
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.TimeSlotInfo
import com.example.myapplication.ui.components.CarWashIcons
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Composable
fun BookingScreen(
    facility: CarWashFacility? = null,
    tier: CarWashTier = CarWashTier(
        id = "3",
        title = "40 Minutes- Deep Clean",
        description = "Full exterior wax, high-suction vacuum & mats blast",
        price = 15000,
        formattedPrice = "MWK 15,000"
    ),
    onBackClick: () -> Unit = {},
    onProceedToPayment: (String, Long, Long) -> Unit = { _, _, _ -> },
    onProfileClick: () -> Unit = {}
) {
    val facilityData = FacilityRepository.getFacilityById(facility?.id ?: "1")
    val facilityName = facilityData?.name ?: facility?.name ?: "Capital Sparkle Car Wash"
    val facilityAddress = facilityData?.address ?: facility?.location ?: "Area 3, Lilongwe"
    val openingTime = facilityData?.openingTime ?: "06:30 AM"
    val closingTime = facilityData?.closingTime ?: "08:00 PM"
    val operatingDays = facilityData?.operatingDays ?: listOf(
        Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY
    )

    val bays: List<BayConfig> = facilityData?.bays ?: emptyList()
    var selectedBayId by remember(bays) { mutableStateOf(bays.firstOrNull()?.id ?: "1_1") }
    val selectedBay: BayConfig? = bays.find { it.id == selectedBayId } ?: bays.firstOrNull()

    // Service duration minutes
    val durationMinutes = when {
        tier.title.contains("10") -> 10
        tier.title.contains("15") -> 15
        tier.title.contains("20") -> 20
        tier.title.contains("30") -> 30
        tier.title.contains("40") -> 40
        tier.title.contains("45") -> 45
        else -> 30
    }

    // TimeZone & Today Initialization in Africa/Blantyre
    val timeZone = remember { TimeZone.getTimeZone("Africa/Blantyre") }
    val todayCal = remember {
        Calendar.getInstance(timeZone, Locale.ROOT).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
    }

    // Max 90-day booking horizon
    val maxFutureCal = remember {
        (todayCal.clone() as Calendar).apply {
            add(Calendar.DAY_OF_YEAR, 90)
        }
    }

    // Calendar Display State
    var displayedYear by remember { mutableIntStateOf(todayCal.get(Calendar.YEAR)) }
    var displayedMonth by remember { mutableIntStateOf(todayCal.get(Calendar.MONTH)) }

    // Date Selection State: 0L = Unselected initially
    var selectedDateMs by remember { mutableLongStateOf(0L) }
    var isLoadingAvailability by remember { mutableStateOf(false) }

    // Confirmation Popup Dialog State
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Trigger brief loading animation on date change
    LaunchedEffect(selectedDateMs, selectedBayId) {
        if (selectedDateMs > 0L) {
            isLoadingAvailability = true
            delay(150L) // Brief UI response state
            isLoadingAvailability = false
        }
    }

    // Generate time slots ONLY when selectedDateMs > 0L
    val timeSlots: List<TimeSlotInfo> = remember(selectedDateMs, selectedBayId, durationMinutes, facilityData) {
        if (selectedDateMs > 0L) {
            BookingTimeUtils.generateAvailableSlots(
                dateMs = selectedDateMs,
                durationMinutes = durationMinutes,
                openingTimeStr = openingTime,
                closingTimeStr = closingTime,
                facilityId = facilityData?.id ?: "1",
                bayId = selectedBayId
            )
        } else {
            emptyList()
        }
    }

    // Separate into Available vs Unavailable lists (Chronologically ordered)
    val availableSlots = remember(timeSlots) { timeSlots.filter { it.isAvailable } }
    val unavailableSlots = remember(timeSlots) { timeSlots.filter { !it.isAvailable } }

    // Time Slot Selection Index (-1 = no time picked yet)
    var selectedSlotIndex by remember(selectedDateMs, selectedBayId, durationMinutes) {
        mutableIntStateOf(-1)
    }
    val selectedSlot = timeSlots.getOrNull(selectedSlotIndex)

    var errorMessage by remember { mutableStateOf<String?>(null) }

    val formattedSelectedDate = remember(selectedDateMs) {
        if (selectedDateMs > 0L) {
            SimpleDateFormat("EEEE, d MMMM yyyy", Locale.US).apply {
                this.timeZone = timeZone
            }.format(selectedDateMs)
        } else {
            ""
        }
    }

    fun openConfirmationDialog() {
        errorMessage = null
        if (selectedDateMs <= 0L || selectedDateMs < todayCal.timeInMillis) {
            errorMessage = "Please select a valid booking date from the calendar."
            return
        }

        if (selectedSlot == null || !selectedSlot.isAvailable) {
            errorMessage = "Please select a valid, available time slot."
            return
        }

        showConfirmationDialog = true
    }

    fun confirmAndProceedToPayment() {
        showConfirmationDialog = false
        if (selectedSlot == null) return

        // Pre-confirmation conflict check
        val conflict = BookingRepository.hasOverlappingReservation(
            facilityId = facilityData?.id ?: "1",
            bayId = selectedBayId,
            newStartMs = selectedSlot.startMs,
            newEndMs = selectedSlot.endMs
        )

        if (conflict) {
            errorMessage = "Conflict detected: This bay is already reserved for the selected time slot."
            return
        }

        onProceedToPayment(selectedBayId, selectedSlot.startMs, selectedSlot.endMs)
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
                        text = "Select Date & Time",
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
            Surface(
                color = Color.White,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "TOTAL DUE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.5.sp
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Text(
                                text = tier.formattedPrice,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Button(
                            onClick = { openConfirmationDialog() },
                            enabled = (selectedDateMs > 0L && selectedSlot != null && selectedSlot.isAvailable),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White,
                                disabledContainerColor = Color(0xFFCBD5E1),
                                disabledContentColor = Color(0xFF64748B)
                            ),
                            modifier = Modifier
                                .height(48.dp)
                                .padding(start = 16.dp)
                        ) {
                            Text(
                                text = "Proceed to Payment  →",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8FAFC)),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
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
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF991B1B)
                                )
                            }
                        }
                    }
                }

                // Station & Service Summary Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = facilityName,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Text(
                                text = facilityAddress,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = tier.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = PrimaryBlue
                                )

                                Text(
                                    text = tier.formattedPrice,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }

                // DISPLAY EXCLUSIVELY THE SELECTED WASHING BAY
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Selected Washing Bay",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE2EDFF)),
                            border = BorderStroke(1.5.dp, PrimaryBlue)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlue),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ElectricBolt,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column {
                                        Text(
                                            text = selectedBay?.bayNumber ?: "BAY 01",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )

                                        Text(
                                            text = "${selectedBay?.serviceTitle ?: "Foam Wash"} • ${selectedBay?.durationText ?: "$durationMinutes mins"}",
                                            fontSize = 12.sp,
                                            color = Color(0xFF475569)
                                        )
                                    }
                                }

                                Text(
                                    text = selectedBay?.priceText ?: tier.formattedPrice,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = PrimaryBlue
                                )
                            }
                        }
                    }
                }

                // MONTHLY CALENDAR CARD (ALWAYS VISIBLE)
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.3f)),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            val monthCal = remember(displayedYear, displayedMonth) {
                                Calendar.getInstance(timeZone, Locale.ROOT).apply {
                                    set(Calendar.YEAR, displayedYear)
                                    set(Calendar.MONTH, displayedMonth)
                                    set(Calendar.DAY_OF_MONTH, 1)
                                    set(Calendar.HOUR_OF_DAY, 0)
                                    set(Calendar.MINUTE, 0)
                                    set(Calendar.SECOND, 0)
                                    set(Calendar.MILLISECOND, 0)
                                }
                            }

                            val monthYearTitle = SimpleDateFormat("MMMM yyyy", Locale.US).apply {
                                this.timeZone = timeZone
                            }.format(monthCal.time)

                            val isPrevMonthDisabled = (displayedYear < todayCal.get(Calendar.YEAR)) ||
                                    (displayedYear == todayCal.get(Calendar.YEAR) && displayedMonth <= todayCal.get(Calendar.MONTH))

                            val isNextMonthDisabled = (displayedYear > maxFutureCal.get(Calendar.YEAR)) ||
                                    (displayedYear == maxFutureCal.get(Calendar.YEAR) && displayedMonth >= maxFutureCal.get(Calendar.MONTH))

                            // Header: Month Title & Navigation Arrows
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.DateRange,
                                        contentDescription = null,
                                        tint = PrimaryBlue,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = monthYearTitle,
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            if (!isPrevMonthDisabled) {
                                                if (displayedMonth == 0) {
                                                    displayedMonth = 11
                                                    displayedYear -= 1
                                                } else {
                                                    displayedMonth -= 1
                                                }
                                            }
                                        },
                                        enabled = !isPrevMonthDisabled
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                            contentDescription = "Previous Month",
                                            tint = if (!isPrevMonthDisabled) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (!isNextMonthDisabled) {
                                                if (displayedMonth == 11) {
                                                    displayedMonth = 0
                                                    displayedYear += 1
                                                } else {
                                                    displayedMonth += 1
                                                }
                                            }
                                        },
                                        enabled = !isNextMonthDisabled
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Next Month",
                                            tint = if (!isNextMonthDisabled) Color(0xFF0F172A) else Color(0xFFCBD5E1)
                                        )
                                    }
                                }
                            }

                            // 7-Column Weekday Headings Row (Mon, Tue, Wed, Thu, Fri, Sat, Sun)
                            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                weekdays.forEach { dayName ->
                                    Text(
                                        text = dayName,
                                        modifier = Modifier.weight(1f),
                                        textAlign = TextAlign.Center,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            // 7-Column Date Grid Alignment
                            val firstDayOfWeek = monthCal.get(Calendar.DAY_OF_WEEK)
                            val offset = (firstDayOfWeek + 5) % 7 // Mon = 0, Tue = 1, ..., Sun = 6
                            val maxDaysInMonth = monthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
                            val totalCells = offset + maxDaysInMonth

                            val selectedCal = remember(selectedDateMs) {
                                if (selectedDateMs > 0L) {
                                    Calendar.getInstance(timeZone, Locale.ROOT).apply {
                                        timeInMillis = selectedDateMs
                                        set(Calendar.HOUR_OF_DAY, 0)
                                        set(Calendar.MINUTE, 0)
                                        set(Calendar.SECOND, 0)
                                        set(Calendar.MILLISECOND, 0)
                                    }
                                } else {
                                    null
                                }
                            }

                            val rowsCount = (totalCells + 6) / 7
                            Column(
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                for (rowIndex in 0 until rowsCount) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        for (colIndex in 0..6) {
                                            val cellIndex = rowIndex * 7 + colIndex
                                            val dayNumber = cellIndex - offset + 1

                                            if (cellIndex < offset || dayNumber > maxDaysInMonth) {
                                                Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                            } else {
                                                val cellCal = (monthCal.clone() as Calendar).apply {
                                                    set(Calendar.DAY_OF_MONTH, dayNumber)
                                                }
                                                val cellMs = cellCal.timeInMillis

                                                val isPast = cellCal.before(todayCal)
                                                val isBeyondMax = cellCal.after(maxFutureCal)
                                                val isOperatingDay = operatingDays.contains(cellCal.get(Calendar.DAY_OF_WEEK))
                                                val isBookable = !isPast && !isBeyondMax && isOperatingDay

                                                val isSelected = selectedCal != null &&
                                                        selectedCal.get(Calendar.YEAR) == cellCal.get(Calendar.YEAR) &&
                                                        selectedCal.get(Calendar.DAY_OF_YEAR) == cellCal.get(Calendar.DAY_OF_YEAR)

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .aspectRatio(1f)
                                                        .padding(2.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            when {
                                                                isSelected -> PrimaryBlue
                                                                isBookable -> Color(0xFFF1F5F9)
                                                                else -> Color.Transparent
                                                            }
                                                        )
                                                        .clickable(enabled = isBookable) {
                                                            selectedDateMs = cellMs
                                                            selectedSlotIndex = -1 // Clear previous time selection on new date pick
                                                            errorMessage = null
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Column(
                                                        horizontalAlignment = Alignment.CenterHorizontally,
                                                        verticalArrangement = Arrangement.Center
                                                    ) {
                                                        Text(
                                                            text = dayNumber.toString(),
                                                            fontSize = 13.sp,
                                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                            color = when {
                                                                isSelected -> Color.White
                                                                isBookable -> Color(0xFF0F172A)
                                                                else -> Color(0xFFCBD5E1)
                                                            }
                                                        )

                                                        if (isBookable && !isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(4.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color(0xFF16A34A))
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
                    }
                }

                // DYNAMIC SECTION: PROMPT BEFORE DATE SELECTION VS TIME SLOTS AFTER DATE SELECTION
                item {
                    if (selectedDateMs <= 0L) {
                        // State 1: INITIAL UNSELECTED PROMPT (No time slots displayed by default)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.DateRange,
                                    contentDescription = null,
                                    tint = PrimaryBlue,
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "Select a Booking Date",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    Text(
                                        text = "Tap a date on the calendar above to load available car wash time slots.",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    } else if (isLoadingAvailability) {
                        // State 2: LOADING AVAILABILITY INDICATOR
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PrimaryBlue,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Loading available time slots for $formattedSelectedDate...",
                                    fontSize = 13.sp,
                                    color = Color(0xFF475569),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        // State 3: DATE SELECTED - TIME SLOTS ORDERED IN TWO SEPARATED SECTIONS
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F7FF)),
                                border = BorderStroke(1.dp, PrimaryBlue.copy(alpha = 0.2f)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            text = "AVAILABLE TIMES FOR",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = PrimaryBlue,
                                            letterSpacing = 0.5.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = formattedSelectedDate,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0F172A)
                                        )
                                    }

                                    Text(
                                        text = "$durationMinutes min duration",
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B),
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }

                            // SECTION 1: AVAILABLE TIME SLOTS (GENUINELY BOOKABLE)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(
                                    text = "Available Time Slots",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF166534)
                                )

                                if (availableSlots.isEmpty()) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Text(
                                            text = "No available time slots for this date. Please select another date.",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF92400E),
                                            modifier = Modifier.padding(16.dp)
                                        )
                                    }
                                } else {
                                    availableSlots.forEach { slot ->
                                        val actualIndex = timeSlots.indexOf(slot)
                                        val isSelected = actualIndex == selectedSlotIndex

                                        Card(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable {
                                                    selectedSlotIndex = actualIndex
                                                    errorMessage = null
                                                },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isSelected) Color(0xFFE2EDFF) else Color.White
                                            ),
                                            border = if (isSelected) BorderStroke(2.dp, PrimaryBlue) else BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(if (isSelected) PrimaryBlue else Color(0xFFCBD5E1)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        if (isSelected) {
                                                            Box(
                                                                modifier = Modifier
                                                                    .size(8.dp)
                                                                    .clip(CircleShape)
                                                                    .background(Color.White)
                                                            )
                                                        }
                                                    }

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column {
                                                        Text(
                                                            text = slot.timeLabel,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF0F172A)
                                                        )
                                                        Text(
                                                            text = "Duration: $durationMinutes mins",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF64748B)
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFDCFCE7))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = "Available",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF166534)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }

                            // SECTION 2: UNAVAILABLE TIME SLOTS (GREYED OUT & DISABLED)
                            if (unavailableSlots.isNotEmpty()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(
                                        text = "Unavailable Time Slots",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )

                                    unavailableSlots.forEach { slot ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            shape = RoundedCornerShape(12.dp),
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F5F9)),
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                                        ) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(14.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFFCBD5E1))
                                                    )

                                                    Spacer(modifier = Modifier.width(12.dp))

                                                    Column {
                                                        Text(
                                                            text = slot.timeLabel,
                                                            fontSize = 14.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                        Text(
                                                            text = "Duration: $durationMinutes mins",
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(Color(0xFFFEE2E2))
                                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                                ) {
                                                    Text(
                                                        text = slot.reason,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF991B1B)
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

                // FINAL BOOKING SUMMARY BANNER ON MAIN SCREEN
                item {
                    AnimatedVisibility(
                        visible = selectedSlot != null && selectedSlot.isAvailable,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFF0FDF4)),
                            border = BorderStroke(1.dp, Color(0xFF86EFAC))
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF166534),
                                    modifier = Modifier.size(24.dp)
                                )

                                Spacer(modifier = Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = "READY FOR CONFIRMATION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF166534),
                                        letterSpacing = 0.5.sp
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = "${selectedBay?.bayNumber ?: "Bay 1"} • $formattedSelectedDate",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    Text(
                                        text = "Slot: ${selectedSlot?.timeLabel ?: ""} ($durationMinutes min)",
                                        fontSize = 12.sp,
                                        color = Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // CONFIRMATION POPUP DIALOG (OVERLAY)
            if (showConfirmationDialog && selectedSlot != null) {
                AlertDialog(
                    onDismissRequest = { showConfirmationDialog = false },
                    title = {
                        Text(
                            text = "Confirm Booking Details",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Station:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = facilityName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Washing Bay:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = selectedBay?.bayNumber ?: "Bay 01", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Package:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = tier.title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = PrimaryBlue)
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Booking Date:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = formattedSelectedDate, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Time Slot:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = selectedSlot.timeLabel, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(text = "Duration:", fontSize = 12.sp, color = Color(0xFF64748B))
                                        Text(text = "$durationMinutes minutes", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Total Price:", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        Text(text = tier.formattedPrice, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF0F172A))
                                    }
                                }
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { confirmAndProceedToPayment() },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue, contentColor = Color.White),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Confirm & Proceed", fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = { showConfirmationDialog = false },
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(text = "Edit Selection", color = Color(0xFF475569))
                        }
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BookingScreenPreview() {
    MyApplicationTheme {
        BookingScreen()
    }
}
