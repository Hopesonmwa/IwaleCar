package com.example.myapplication.ui

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Nightlight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.myapplication.R
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.toFacilityItemData
import com.example.myapplication.ui.components.OwnerBottomNavigationBar
import com.example.myapplication.ui.components.OwnerNavItem
import com.example.myapplication.ui.theme.MyApplicationTheme

data class FacilityItemData(
    val id: String,
    val name: String,
    val address: String,
    val city: String,
    val phone: String,
    val bayCount: Int,
    val openingTime: String,
    val closingTime: String,
    val description: String,
    val status: String // "Approved", "Pending Approval", "Rejected"
)

@Composable
fun OwnerFacilitiesScreen(
    onDashboardClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onRevenueClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
    onManageBaysClick: () -> Unit = {}
) {
    var showModal by remember { mutableStateOf(false) }
    var facilityToEdit by remember { mutableStateOf<FacilityItemData?>(null) }

    val currentUser = UserRepository.getCurrentUser()
    val facilitiesList = FacilityRepository.getFacilitiesForOwner(currentUser.email).map { it.toFacilityItemData() }

    val activeCount = facilitiesList.count { it.status == "Approved" }
    val inReviewCount = facilitiesList.count { it.status != "Approved" }

    // Modal Window for Adding / Editing Facility
    if (showModal) {
        RegisterFacilityModal(
            facilityToEdit = facilityToEdit,
            onDismiss = {
                showModal = false
                facilityToEdit = null
            },
            onSubmitSuccess = { updatedData ->
                FacilityRepository.saveFacility(updatedData, currentUser.email)
                showModal = false
                facilityToEdit = null
            }
        )
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
                            text = "Owner Facilities",
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
                selectedTab = OwnerNavItem.FACILITIES,
                onTabSelected = { tab ->
                    when (tab) {
                        OwnerNavItem.DASHBOARD -> onDashboardClick()
                        OwnerNavItem.FACILITIES -> {}
                        OwnerNavItem.BOOKINGS -> onBookingsClick()
                        OwnerNavItem.REVENUE -> onRevenueClick()
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
            // Header Row: My Facilities + Add Facility Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Facilities",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Manage your car wash locations",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = {
                            facilityToEdit = null
                            showModal = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006699),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(42.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Add Facility",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // Summary Stats Line
            item {
                Text(
                    text = "Total: ${facilitiesList.size}   •   Active: $activeCount   •   In Review: $inReviewCount",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }

            // Facilities List
            items(facilitiesList, key = { it.id }) { facility ->
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
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val (badgeBg, badgeFg, badgeText) = when (facility.status) {
                                "Approved" -> Triple(Color(0xFFDCFCE7), Color(0xFF16A34A), "• Approved")
                                "Pending Approval" -> Triple(Color(0xFFE0F2FE), Color(0xFF0284C7), "Pending Approval")
                                else -> Triple(Color(0xFFFEE2E2), Color(0xFFDC2626), "Rejected")
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(badgeBg)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badgeFg
                                )
                            }

                            if (facility.status == "Rejected") {
                                Text(
                                    text = "Requires Corrections",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFB45309)
                                )
                            } else {
                                Text(
                                    text = facility.address,
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Text(
                            text = facility.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = if (facility.status == "Rejected") facility.address else "${facility.bayCount} Washing Bays • ${facility.openingTime} – ${facility.closingTime} CAT",
                            fontSize = 12.sp,
                            color = Color(0xFF475569)
                        )

                        if (facility.status == "Pending Approval") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Application & municipal drainage clearance under verification. Bays unavailable for public booking until approved.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 16.sp
                                )
                            }
                        } else if (facility.status == "Rejected") {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFFEF2F2))
                                    .border(1.dp, Color(0xFFFCA5A5), RoundedCornerShape(10.dp))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = "Note: ${facility.description}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF991B1B),
                                    lineHeight = 16.sp
                                )
                            }
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "+265 ${facility.phone}",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                if (facility.status == "Pending Approval") {
                                    Text(
                                        text = "Review Submission",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.clickable { }
                                    )
                                }

                                // Edit Button -> Opens Modal with prefilled facility details
                                Text(
                                    text = "Edit",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    modifier = Modifier.clickable {
                                        facilityToEdit = facility
                                        showModal = true
                                    }
                                )

                                if (facility.status == "Approved") {
                                    Button(
                                        onClick = {
                                            FacilityRepository.setSelectedFacilityId(facility.id)
                                            onManageBaysClick()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFF006699),
                                            contentColor = Color.White
                                        ),
                                        modifier = Modifier.height(36.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Outlined.Settings,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "Manage Bays",
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
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

/**
 * Register & Edit Facility Pop-up Modal Window
 */
@Composable
fun RegisterFacilityModal(
    facilityToEdit: FacilityItemData? = null,
    onDismiss: () -> Unit,
    onSubmitSuccess: (FacilityItemData) -> Unit
) {
    var facilityName by remember { mutableStateOf(facilityToEdit?.name ?: "") }
    var address by remember { mutableStateOf(facilityToEdit?.address ?: "") }
    var selectedCity by remember { mutableStateOf(facilityToEdit?.city ?: "Lilongwe") }
    var cityDropdownExpanded by remember { mutableStateOf(false) }
    var operatorPhone by remember { mutableStateOf(facilityToEdit?.phone ?: "") }
    var bayCount by remember { mutableIntStateOf(facilityToEdit?.bayCount ?: 0) }

    val openingTimeOptions = listOf(
        "06:00 AM", "06:30 AM", "07:00 AM", "07:30 AM", "08:00 AM", "08:30 AM", "09:00 AM"
    )
    val closingTimeOptions = listOf(
        "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM", "08:00 PM", "08:30 PM", "09:00 PM", "09:30 PM", "10:00 PM", "10:30 PM", "11:00 PM", "11:30 PM", "12:00 AM (Midnight)"
    )

    var openingTime by remember { mutableStateOf(facilityToEdit?.openingTime?.ifBlank { "06:30 AM" } ?: "06:30 AM") }
    var closingTime by remember { mutableStateOf(facilityToEdit?.closingTime?.ifBlank { "08:00 PM" } ?: "08:00 PM") }
    var openingTimeDropdownExpanded by remember { mutableStateOf(false) }
    var closingTimeDropdownExpanded by remember { mutableStateOf(false) }

    var featuresText by remember {
        mutableStateOf(facilityToEdit?.description ?: "High-pressure automated foam bays with dedicated drying slots, waiting lounge, and solar-backed power...")
    }

    val cities = listOf("Lilongwe", "Blantyre", "Mzuzu", "Zomba")
    val isEditing = facilityToEdit != null

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            LazyColumn(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Modal Header
                item {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEditing) "• EDIT FACILITY DETAILS" else "• FACILITY ONBOARDING",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0284C7),
                                letterSpacing = 0.5.sp
                            )

                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Close",
                                    tint = Color(0xFF475569),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Text(
                            text = if (isEditing) "Edit Facility" else "Register New Facility",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        Text(
                            text = if (isEditing) "Update your car wash details below and save changes." else "Submit your car wash location for administrative approval.",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Section 1: FACILITY DETAILS
                item {
                    Text(
                        text = "FACILITY DETAILS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                }

                // Facility Name
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Facility Name *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Store,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (facilityName.isEmpty()) {
                                        Text(text = "e.g. Area 10 AutoSpa", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    }
                                    BasicTextField(
                                        value = facilityName,
                                        onValueChange = { facilityName = it },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Physical Location / Address
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Physical Location / Address *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.LocationOn,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (address.isEmpty()) {
                                        Text(text = "e.g. Plot 42, Off M1 Road, Lilongwe", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    }
                                    BasicTextField(
                                        value = address,
                                        onValueChange = { address = it },
                                        singleLine = true,
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // City / District Dropdown Picker
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "City / District *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .clickable { cityDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Store,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = selectedCity,
                                        fontSize = 13.sp,
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
                                expanded = cityDropdownExpanded,
                                onDismissRequest = { cityDropdownExpanded = false }
                            ) {
                                cities.forEach { city ->
                                    DropdownMenuItem(
                                        text = { Text(text = city, fontSize = 13.sp) },
                                        onClick = {
                                            selectedCity = city
                                            cityDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Operator Phone (Malawi)
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Operator Phone (Malawi) *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Outlined.Call,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(text = "+265", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F5FA))
                                    .padding(horizontal = 12.dp, vertical = 12.dp)
                            ) {
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (operatorPhone.isEmpty()) {
                                        Text(text = "999 000 000", fontSize = 13.sp, color = Color(0xFF94A3B8))
                                    }
                                    BasicTextField(
                                        value = operatorPhone,
                                        onValueChange = { if (it.length <= 9 && it.all { char -> char.isDigit() }) operatorPhone = it },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                        textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                // Section 2: CAPACITY & OPERATIONS
                item {
                    Text(
                        text = "CAPACITY & OPERATIONS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        letterSpacing = 0.5.sp
                    )
                }

                // Opening & Closing Times Dropdown Selectors
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Opening Time Dropdown Menu
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "Opening Time *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF0F5FA))
                                        .clickable { openingTimeDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Outlined.AccessTime, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = openingTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = openingTimeDropdownExpanded,
                                    onDismissRequest = { openingTimeDropdownExpanded = false }
                                ) {
                                    openingTimeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                openingTime = option
                                                openingTimeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Closing Time Dropdown Menu
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(text = "Closing Time *", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFF0F5FA))
                                        .clickable { closingTimeDropdownExpanded = true }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(imageVector = Icons.Outlined.Nightlight, contentDescription = null, tint = Color(0xFF0284C7), modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(text = closingTime, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                        }
                                        Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                    }
                                }

                                DropdownMenu(
                                    expanded = closingTimeDropdownExpanded,
                                    onDismissRequest = { closingTimeDropdownExpanded = false }
                                ) {
                                    closingTimeOptions.forEach { option ->
                                        DropdownMenuItem(
                                            text = { Text(text = option, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                            onClick = {
                                                closingTime = option
                                                closingTimeDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Station Features / Description
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(text = "Station Features / Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(70.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(10.dp)
                        ) {
                            BasicTextField(
                                value = featuresText,
                                onValueChange = { featuresText = it },
                                textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF334155), lineHeight = 16.sp),
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }

                // Admin Review Required Box
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEBF2FF))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Outlined.VerifiedUser,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Admin Review Required",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "New facilities require admin verification (business registration & drainage clearance) before becoming publicly bookable on Iwale Car.",
                                    fontSize = 11.sp,
                                    color = Color(0xFF475569),
                                    lineHeight = 15.sp
                                )
                            }
                        }
                    }
                }

                // Submit / Save Button
                item {
                    Button(
                        onClick = {
                            val updatedItem = FacilityItemData(
                                id = facilityToEdit?.id ?: "${System.currentTimeMillis()}",
                                name = facilityName.ifEmpty { "New Car Wash Facility" },
                                address = address.ifEmpty { "Plot 1, Main St" },
                                city = selectedCity,
                                phone = operatorPhone.ifEmpty { "999000111" },
                                bayCount = bayCount,
                                openingTime = openingTime,
                                closingTime = closingTime,
                                description = featuresText,
                                status = facilityToEdit?.status ?: "Pending Approval"
                            )
                            onSubmitSuccess(updatedItem)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF006699),
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
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (isEditing) "Save Facility Changes" else "Submit for Approval",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Cancel Link
                item {
                    Text(
                        text = "Cancel",
                        fontSize = 13.sp,
                        color = Color(0xFF334155),
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismiss() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun OwnerFacilitiesScreenPreview() {
    MyApplicationTheme {
        OwnerFacilitiesScreen()
    }
}
