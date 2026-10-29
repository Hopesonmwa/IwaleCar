package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import com.example.myapplication.data.BookingRepository
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.components.OwnerBottomNavigationBar
import com.example.myapplication.ui.components.OwnerNavItem
import com.example.myapplication.ui.theme.MyApplicationTheme
import java.text.NumberFormat

data class BayConfig(
    val id: String,
    val bayNumber: String,
    val serviceTitle: String,
    val featuresDescription: String,
    val priceText: String,
    val priceValue: Int,
    val durationText: String,
    val status: String // "Available", "Occupied", "Maintenance"
)

@Composable
fun ManageBaysScreen(
    onBackClick: () -> Unit = {},
    onDashboardClick: () -> Unit = {},
    onFacilitiesClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onRevenueClick: () -> Unit = {},
    onNotificationsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {}
) {
    val currentUser = UserRepository.getCurrentUser()
    val ownerFacilities = FacilityRepository.getFacilitiesForOwner(currentUser.email)
    val selectedFacility = FacilityRepository.getSelectedFacilityForOwner(currentUser.email)

    var isFacilityDropdownExpanded by remember { mutableStateOf(false) }
    var editingBay by remember { mutableStateOf<BayConfig?>(null) }
    var showAddModal by remember { mutableStateOf(false) }
    var bayToRemove by remember { mutableStateOf<BayConfig?>(null) }
    var removalErrorMessage by remember { mutableStateOf<String?>(null) }

    val configuredBays = selectedFacility?.bays ?: emptyList()

    val totalCount = configuredBays.size
    val availableCount = configuredBays.count { it.status == "Available" }
    val occupiedCount = configuredBays.count { it.status == "Occupied" }
    val maintenanceCount = configuredBays.count { it.status == "Maintenance" }

    // Add New Bay Modal
    if (showAddModal) {
        val nextNumber = String.format("%02d", configuredBays.size + 1)
        val defaultNewBay = BayConfig(
            id = "${System.currentTimeMillis()}",
            bayNumber = "BAY $nextNumber",
            serviceTitle = "Standard Wash Bay",
            featuresDescription = "High-pressure nozzle & foam lance",
            priceText = "MK 5,000",
            priceValue = 5000,
            durationText = "30 mins",
            status = "Available"
        )

        EditBayModal(
            titleText = "Add New Bay",
            bay = defaultNewBay,
            onDismiss = { showAddModal = false },
            onSave = { newBay ->
                if (selectedFacility != null) {
                    val updatedList = configuredBays + newBay
                    FacilityRepository.updateBaysForFacility(selectedFacility.id, updatedList)
                }
                showAddModal = false
            }
        )
    }

    // Edit Bay Modal
    editingBay?.let { bayToEdit ->
        EditBayModal(
            titleText = "Edit ${bayToEdit.bayNumber}",
            bay = bayToEdit,
            onDismiss = { editingBay = null },
            onSave = { updatedBay ->
                if (selectedFacility != null) {
                    val newBays = configuredBays.map { if (it.id == updatedBay.id) updatedBay else it }
                    FacilityRepository.updateBaysForFacility(selectedFacility.id, newBays)
                }
                editingBay = null
            }
        )
    }

    // Requirement 7: Confirmation Dialog Before Removing Bay
    bayToRemove?.let { bay ->
        AlertDialog(
            onDismissRequest = { bayToRemove = null },
            title = {
                Text(
                    text = "Remove ${bay.bayNumber}?",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove ${bay.bayNumber} (${bay.serviceTitle})? This bay will no longer be available for new bookings.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val facId = selectedFacility?.id ?: ""
                        if (BookingRepository.hasActiveOrUpcomingBookingForBay(facId, bay.id)) {
                            removalErrorMessage = "Cannot remove ${bay.bayNumber} because it has an active or upcoming booking."
                        } else {
                            if (selectedFacility != null) {
                                val newBays = configuredBays.filter { it.id != bay.id }
                                FacilityRepository.updateBaysForFacility(selectedFacility.id, newBays)
                            }
                        }
                        bayToRemove = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFDC2626),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(text = "Remove Bay", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bayToRemove = null }) {
                    Text(text = "Cancel", color = Color(0xFF64748B), fontWeight = FontWeight.SemiBold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
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
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Manage Bays & Pricing",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
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
                        OwnerNavItem.FACILITIES -> onFacilitiesClick()
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
            // Removal Error Banner
            item {
                AnimatedVisibility(visible = removalErrorMessage != null) {
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
                                text = removalErrorMessage ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B),
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { removalErrorMessage = null }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = Color(0xFF991B1B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }

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
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Selected Facility",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = selectedFacility?.name ?: "Select Facility",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A)
                                )
                            }
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

            // Stats Summary Bar
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F5FA)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Total: $totalCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Text(text = "|", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(text = "Available: $availableCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF16A34A))
                        Text(text = "|", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(text = "Occupied: $occupiedCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        Text(text = "|", fontSize = 12.sp, color = Color(0xFFCBD5E1))
                        Text(text = "Maint: $maintenanceCount", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }
            }

            // Header: Washing Bays + Add Bay Button
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Washing Bays ",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "(${configuredBays.size} configured)",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                    }

                    Button(
                        onClick = { showAddModal = true },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0B132B),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.height(38.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "Add Bay", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Empty State if no bays added yet
            if (configuredBays.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Settings,
                                contentDescription = null,
                                tint = Color(0xFF0284C7),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "No bays added yet",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Add washing bays to configure service tiers, pricing, and availability for ${selectedFacility?.name ?: "this facility"}.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Button(
                                onClick = { showAddModal = true },
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF006699),
                                    contentColor = Color.White
                                )
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(text = "Add Bay", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                // Configured Bays List
                items(configuredBays, key = { it.id }) { bay ->
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
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = bay.bayNumber,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0284C7)
                                    )

                                    Spacer(modifier = Modifier.width(8.dp))

                                    // Status Pill
                                    val (bgCol, textCol) = when (bay.status) {
                                        "Available" -> Color(0xFFDCFCE7) to Color(0xFF16A34A)
                                        "Occupied" -> Color(0xFFE0F2FE) to Color(0xFF0284C7)
                                        else -> Color(0xFFF1F5F9) to Color(0xFF64748B)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(bgCol)
                                            .padding(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = bay.status,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textCol
                                        )
                                    }
                                }

                                // Action Buttons Row: Remove & Edit Buttons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFFEF2F2))
                                            .clickable {
                                                removalErrorMessage = null
                                                bayToRemove = bay
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Remove",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFDC2626)
                                        )
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(Color(0xFFE2EDFF))
                                            .clickable { editingBay = bay }
                                            .padding(horizontal = 14.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            text = "Edit",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF0284C7)
                                        )
                                    }
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bay.serviceTitle,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )

                                    Spacer(modifier = Modifier.height(2.dp))

                                    Text(
                                        text = bay.featuresDescription,
                                        fontSize = 12.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = bay.priceText,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF0F172A)
                                    )

                                    Text(
                                        text = "/${bay.durationText.replace(" mins", "m")}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
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

/**
 * Edit & Add Bay Pop-up Modal Window with Validation (Requirement 6)
 */
@Composable
fun EditBayModal(
    titleText: String = "Edit Bay",
    bay: BayConfig,
    onDismiss: () -> Unit,
    onSave: (BayConfig) -> Unit
) {
    var bayName by remember { mutableStateOf(bay.bayNumber) }
    var serviceTitle by remember { mutableStateOf(bay.serviceTitle) }
    var features by remember { mutableStateOf(bay.featuresDescription) }
    var priceInput by remember { mutableStateOf("${bay.priceValue}") }
    var durationText by remember { mutableStateOf(bay.durationText) }
    var statusChoice by remember { mutableStateOf(if (bay.status == "Maintenance") "Maintenance" else "Available") }
    var isDurationDropdownExpanded by remember { mutableStateOf(false) }

    var validationError by remember { mutableStateOf<String?>(null) }

    val durations = listOf("10 mins", "15 mins", "20 mins", "30 mins", "40 mins", "45 mins", "60 mins")

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = titleText,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                    }

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

                // Inline Validation Error Banner
                AnimatedVisibility(visible = validationError != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = validationError ?: "",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                        }
                    }
                }

                // Bay Name / Number
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Bay Name / Number", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = bayName,
                            onValueChange = {
                                bayName = it
                                validationError = null
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Service / Package Description
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Service / Package Description", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = serviceTitle,
                            onValueChange = {
                                serviceTitle = it
                                validationError = null
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Equipment / Lance Features
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(text = "Equipment / Lance Features", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 12.dp, vertical = 12.dp)
                    ) {
                        BasicTextField(
                            value = features,
                            onValueChange = {
                                features = it
                                validationError = null
                            },
                            singleLine = true,
                            textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Price (MWK) & Duration (Requirement 6 Validation)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Price (MWK)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = "MK ", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                                BasicTextField(
                                    value = priceInput,
                                    onValueChange = {
                                        priceInput = it
                                        validationError = null
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A)),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = "Duration", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F5FA))
                                .clickable { isDurationDropdownExpanded = true }
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = durationText, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Icon(imageVector = Icons.Outlined.ArrowDropDown, contentDescription = null, tint = Color(0xFF64748B))
                            }

                            DropdownMenu(
                                expanded = isDurationDropdownExpanded,
                                onDismissRequest = { isDurationDropdownExpanded = false }
                            ) {
                                durations.forEach { d ->
                                    DropdownMenuItem(
                                        text = { Text(text = d, fontSize = 13.sp) },
                                        onClick = {
                                            durationText = d
                                            validationError = null
                                            isDurationDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Bay Status Radio Options
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(text = "Bay Status", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Option 1: Available
                        val isAvail = statusChoice == "Available"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isAvail) Color(0xFFE2EDFF) else Color.White)
                                .border(1.5.dp, if (isAvail) Color(0xFF0284C7) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { statusChoice = "Available" }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isAvail,
                                    onClick = { statusChoice = "Available" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF0284C7))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(text = "Available", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text(text = "Ready for wash", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }

                        // Option 2: Maintenance
                        val isMaint = statusChoice == "Maintenance"
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isMaint) Color(0xFFFEF3C7) else Color.White)
                                .border(1.5.dp, if (isMaint) Color(0xFFD97706) else Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                                .clickable { statusChoice = "Maintenance" }
                                .padding(10.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(
                                    selected = isMaint,
                                    onClick = { statusChoice = "Maintenance" },
                                    colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFD97706))
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Column {
                                    Text(text = "Maintenance", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    Text(text = "Under service", fontSize = 10.sp, color = Color(0xFF64748B))
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                // Save Changes Button (Requirement 6 Validation Enforced)
                Button(
                    onClick = {
                        val parsedPrice = priceInput.trim().toIntOrNull()
                        val durationMins = durationText.removeSuffix(" mins").removeSuffix("m").trim().toIntOrNull()

                        if (bayName.trim().isEmpty() || serviceTitle.trim().isEmpty()) {
                            validationError = "Bay name and service title are required."
                            return@Button
                        }

                        if (parsedPrice == null || parsedPrice <= 0) {
                            validationError = "Price must be a valid positive MWK amount (> 0)."
                            return@Button
                        }

                        if (durationMins == null || durationMins <= 0) {
                            validationError = "Duration must be greater than 0 minutes."
                            return@Button
                        }

                        val formattedP = "MK " + NumberFormat.getNumberInstance().format(parsedPrice)
                        val updated = bay.copy(
                            bayNumber = bayName.trim(),
                            serviceTitle = serviceTitle.trim(),
                            featuresDescription = features.trim(),
                            priceText = formattedP,
                            priceValue = parsedPrice,
                            durationText = durationText,
                            status = statusChoice
                        )
                        onSave(updated)
                    },
                    shape = RoundedCornerShape(12.dp),
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
                        Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = "Save Bay Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Cancel Link
                Text(
                    text = "Cancel",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B),
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

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ManageBaysScreenPreview() {
    MyApplicationTheme {
        ManageBaysScreen()
    }
}
