package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Call
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

@Composable
fun OwnerApplicationScreen(
    onBackClick: () -> Unit = {},
    onSubmitted: () -> Unit = {}
) {
    val currentUser = UserRepository.getCurrentUser()
    val scope = rememberCoroutineScope()

    // 1. Facility Details
    var facilityName by remember { mutableStateOf("") }
    var facilityContactNumber by remember { mutableStateOf(currentUser.phoneNumber) }
    var shortDescription by remember { mutableStateOf("") }

    // 2. Location Details
    var city by remember { mutableStateOf(currentUser.primaryLocation.ifBlank { "Lilongwe" }) }
    var district by remember { mutableStateOf("${currentUser.primaryLocation.ifBlank { "Lilongwe" }} Urban") }
    var physicalAddress by remember { mutableStateOf("") }
    var landmark by remember { mutableStateOf("") }

    // 3. Business Verification
    var registrationNumber by remember { mutableStateOf("") }
    var operatingLicense by remember { mutableStateOf("") }

    // 4. Operating Information (Dropdown Selections)
    val operatingDaysOptions = listOf(
        "Monday - Saturday",
        "Monday - Friday",
        "Monday - Sunday (Except Saturday)",
        "Monday - Sunday"
    )
    val openingTimeOptions = listOf(
        "06:00 AM", "06:30 AM", "07:00 AM", "07:30 AM", "08:00 AM", "08:30 AM", "09:00 AM"
    )
    val closingTimeOptions = listOf(
        "05:00 PM", "05:30 PM", "06:00 PM", "06:30 PM", "07:00 PM", "07:30 PM", "08:00 PM", "08:30 PM", "09:00 PM", "09:30 PM", "10:00 PM", "10:30 PM", "11:00 PM", "11:30 PM", "12:00 AM (Midnight)"
    )

    var operatingDays by remember { mutableStateOf(operatingDaysOptions.first()) }
    var openingTime by remember { mutableStateOf(openingTimeOptions[1]) } // "06:30 AM"
    var closingTime by remember { mutableStateOf(closingTimeOptions[6]) } // "08:00 PM"

    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSubmittedSuccess by remember { mutableStateOf(false) }

    fun submitForm() {
        if (isSubmitting) return

        if (facilityName.isBlank()) {
            errorMessage = "Facility name is required."
            return
        }
        if (facilityContactNumber.isBlank()) {
            errorMessage = "Facility contact number is required."
            return
        }
        if (city.isBlank()) {
            errorMessage = "City is required."
            return
        }
        if (district.isBlank()) {
            errorMessage = "District is required."
            return
        }
        if (physicalAddress.isBlank()) {
            errorMessage = "Physical address / area is required."
            return
        }
        if (operatingDays.isBlank()) {
            errorMessage = "Operating days are required."
            return
        }
        if (openingTime.isBlank()) {
            errorMessage = "Opening time is required."
            return
        }
        if (closingTime.isBlank()) {
            errorMessage = "Closing time is required."
            return
        }

        errorMessage = null
        isSubmitting = true

        scope.launch {
            val result = OwnerApplicationRepository.submitFullApplication(
                applicant = currentUser,
                facilityName = facilityName,
                facilityContactNumber = facilityContactNumber,
                shortDescription = shortDescription,
                city = city,
                district = district,
                physicalAddress = physicalAddress,
                landmark = landmark,
                registrationNumber = registrationNumber,
                operatingLicense = operatingLicense,
                operatingDays = operatingDays,
                openingTime = openingTime,
                closingTime = closingTime
            )
            isSubmitting = false
            if (result.isSuccess) {
                isSubmittedSuccess = true
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Application submission failed. Please try again."
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
        if (isSubmittedSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
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
                            text = "Application Submitted!",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Your car wash facility application for \"$facilityName\" has been received. " +
                                    "Our system administrator will review your submitted verification details. Once approved, you will gain full access to your Car Wash Owner Dashboard.",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )

                        Button(
                            onClick = onSubmitted,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                        ) {
                            Text(text = "Go to Owner Onboarding", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .background(Color.White),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Navigation & Branding Logo (Matching SignUpScreen layout)
                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onBackClick) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color(0xFF0F172A)
                                )
                            }
                        }

                        Image(
                            painter = painterResource(id = R.drawable.ic_iwale_wash_logo),
                            contentDescription = "IWALE Car Logo",
                            modifier = Modifier.size(140.dp)
                        )

                        Spacer(modifier = Modifier.height(1.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "IWALE ",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Car",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF0284C7)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Car Wash Owner Application",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = "Complete your business & facility verification details below.",
                            fontSize = 13.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Error Message Banner
                item {
                    AnimatedVisibility(visible = errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                            border = BorderStroke(1.dp, Color(0xFFFCA5A5))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ErrorOutline,
                                    contentDescription = null,
                                    tint = Color(0xFFDC2626),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
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

                // 1. Facility Details Section Header
                item {
                    FormSectionHeader("1. Facility Details")
                }

                // Facility Name
                item {
                    FormInputField(
                        label = "Facility Name *",
                        value = facilityName,
                        onValueChange = { facilityName = it; errorMessage = null },
                        placeholder = "e.g. Area 4 Express Wash Station",
                        icon = Icons.Outlined.Storefront
                    )
                }

                // Facility Contact Number
                item {
                    FormInputField(
                        label = "Facility Contact Number *",
                        value = facilityContactNumber,
                        onValueChange = { facilityContactNumber = it; errorMessage = null },
                        placeholder = "999123456",
                        icon = Icons.Outlined.Call,
                        keyboardType = KeyboardType.Phone
                    )
                }

                // Short Description
                item {
                    FormInputField(
                        label = "Short Description (Optional)",
                        value = shortDescription,
                        onValueChange = { if (it.length <= 400) shortDescription = it; errorMessage = null },
                        placeholder = "e.g. 4 High-pressure wash bays with active foam & ceramic rinse",
                        icon = Icons.Outlined.Description,
                        singleLine = false
                    )
                }

                // 2. Location Details Section Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    FormSectionHeader("2. Location Details")
                }

                // City & District Side by Side
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormInputField(
                                label = "City *",
                                value = city,
                                onValueChange = { city = it; errorMessage = null },
                                placeholder = "Lilongwe",
                                icon = Icons.Outlined.LocationOn
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            FormInputField(
                                label = "District *",
                                value = district,
                                onValueChange = { district = it; errorMessage = null },
                                placeholder = "Lilongwe Urban",
                                icon = Icons.Outlined.Map
                            )
                        }
                    }
                }

                // Physical Address / Area
                item {
                    FormInputField(
                        label = "Physical Address / Area *",
                        value = physicalAddress,
                        onValueChange = { physicalAddress = it; errorMessage = null },
                        placeholder = "Plot 18, Off Chilambula Road, Area 4",
                        icon = Icons.Outlined.Place
                    )
                }

                // Nearby Landmark
                item {
                    FormInputField(
                        label = "Nearby Landmark (Optional)",
                        value = landmark,
                        onValueChange = { landmark = it; errorMessage = null },
                        placeholder = "e.g. Opposite Pacific Mall / Behind Puma Filling Station",
                        icon = Icons.Outlined.Place
                    )
                }

                // 3. Business Verification Section Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    FormSectionHeader("3. Business Verification")
                }

                // Business Registration Certificate
                item {
                    FormInputField(
                        label = "Business Registration Certificate (Optional)",
                        value = registrationNumber,
                        onValueChange = { registrationNumber = it; errorMessage = null },
                        placeholder = "MBRS-Reg-2024-8849",
                        icon = Icons.Outlined.Badge
                    )
                }

                // Operating Licence
                item {
                    FormInputField(
                        label = "Operating Licence (Optional)",
                        value = operatingLicense,
                        onValueChange = { operatingLicense = it; errorMessage = null },
                        placeholder = "LCC-Licence-2024-4412",
                        icon = Icons.Outlined.Badge
                    )
                }

                // 4. Operating Information Section Header
                item {
                    Spacer(modifier = Modifier.height(6.dp))
                    FormSectionHeader("4. Operating Information")
                }

                // Operating Days Dropdown Menu
                item {
                    FormDropdownSelectField(
                        label = "Operating Days *",
                        selectedValue = operatingDays,
                        options = operatingDaysOptions,
                        onOptionSelected = { operatingDays = it; errorMessage = null },
                        icon = Icons.Outlined.AccessTime
                    )
                }

                // Opening & Closing Time Side by Side Dropdown Selectors
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            FormDropdownSelectField(
                                label = "Opening Time *",
                                selectedValue = openingTime,
                                options = openingTimeOptions,
                                onOptionSelected = { openingTime = it; errorMessage = null },
                                icon = Icons.Outlined.AccessTime
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            FormDropdownSelectField(
                                label = "Closing Time *",
                                selectedValue = closingTime,
                                options = closingTimeOptions,
                                onOptionSelected = { closingTime = it; errorMessage = null },
                                icon = Icons.Outlined.AccessTime
                            )
                        }
                    }
                }

                // Submit Button (Matching SignUpScreen button layout)
                item {
                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = { submitForm() },
                        enabled = !isSubmitting,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = PrimaryBlue,
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFCBD5E1),
                            disabledContentColor = Color(0xFF64748B)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(22.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Submit Application for Verification  →",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Bottom Padding
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun FormSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 16.sp,
        fontWeight = FontWeight.ExtraBold,
        color = Color(0xFF0F172A),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun FormInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    singleLine: Boolean = true
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFFF0F5FA))
                .padding(horizontal = 14.dp, vertical = if (singleLine) 14.dp else 10.dp)
        ) {
            Row(verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(20.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        singleLine = singleLine,
                        minLines = if (singleLine) 1 else 2,
                        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF0F172A)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun FormDropdownSelectField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onOptionSelected: (String) -> Unit,
    icon: ImageVector
) {
    var isExpanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF0F172A)
        )

        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F5FA))
                    .clickable { isExpanded = true }
                    .padding(horizontal = 14.dp, vertical = 14.dp)
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
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(10.dp))

                        Text(
                            text = selectedValue.ifBlank { "Select option..." },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A)
                        )
                    }

                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = "Dropdown",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = isExpanded,
                onDismissRequest = { isExpanded = false },
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .background(Color.White)
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                fontSize = 13.sp,
                                fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Medium,
                                color = if (option == selectedValue) Color(0xFF0284C7) else Color(0xFF0F172A)
                            )
                        },
                        onClick = {
                            onOptionSelected(option)
                            isExpanded = false
                        }
                    )
                }
            }
        }
    }
}
