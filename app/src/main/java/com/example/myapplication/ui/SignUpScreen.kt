package com.example.myapplication.ui

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.ValidationUtils
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

enum class UserRole(val title: String) {
    CUSTOMER("Customer"),
    OWNER("Car Wash Owner")
}

private enum class LegalDocumentType {
    TERMS_OF_SERVICE,
    PRIVACY_POLICY
}

@Composable
fun SignUpScreen(
    onSignUpClick: (String) -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verifyPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifyPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf<UserRole?>(UserRole.CUSTOMER) }
    var selectedLocation by remember { mutableStateOf("Lilongwe") }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var activeLegalDocument by remember { mutableStateOf<LegalDocumentType?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Validation Flags
    val isFirstNameValid = firstName.trim().isNotEmpty()
    val isLastNameValid = lastName.trim().isNotEmpty()
    val isPhoneValid = ValidationUtils.isPhoneValid(phoneNumber)
    val isEmailValid = ValidationUtils.isEmailValid(email)
    val isPasswordFormatValid = ValidationUtils.isPasswordComplex(password)
    val isPasswordMatching = password.isNotEmpty() && password == verifyPassword

    val isFormValid = isFirstNameValid &&
            isLastNameValid &&
            isPhoneValid &&
            isEmailValid &&
            isPasswordFormatValid &&
            isPasswordMatching &&
            selectedRole != null &&
            selectedLocation.isNotEmpty()

    fun attemptRegistration() {
        if (isLoading) return

        if (!isTermsAccepted) {
            errorMessage = "You must accept the Terms of Service and Privacy Policy to register."
            return
        }
        if (!isFormValid) {
            errorMessage = "Please complete all required fields correctly."
            return
        }

        errorMessage = null
        val roleTitle = selectedRole?.title ?: "Customer"
        val fullNameCombined = "${firstName.trim()} ${lastName.trim()}"
        val newUser = UserAccount(
            fullName = fullNameCombined,
            phoneNumber = phoneNumber.trim(),
            email = email.trim(),
            password = password,
            role = roleTitle,
            primaryLocation = selectedLocation,
            requestedRole = roleTitle,
            firstName = firstName.trim(),
            lastName = lastName.trim()
        )

        isLoading = true
        scope.launch {
            val result = AuthRepository.signUp(newUser, password)
            isLoading = false
            if (result.isSuccess) {
                errorMessage = null
                onSignUpClick(newUser.email)
            } else {
                errorMessage = result.exceptionOrNull()?.message ?: "Registration failed. Please try again."
            }
        }
    }

    Scaffold(
        containerColor = Color.White
    ) { innerPadding ->
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
            // Header Logo & Title
            item {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
                        text = "Create Account",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
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

            // Role Selection
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Account Type",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        UserRole.entries.forEach { role ->
                            val isSelected = role == selectedRole
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) Color(0xFF0B132B) else Color(0xFFF0F5FA))
                                    .clickable { selectedRole = role }
                                    .padding(vertical = 12.dp, horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = if (role == UserRole.CUSTOMER) Icons.Outlined.Person else Icons.Outlined.Storefront,
                                        contentDescription = null,
                                        tint = if (isSelected) Color.White else Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = role.title,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // First Name & Last Name
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // First Name
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "First Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (firstName.isEmpty()) {
                                        Text(
                                            text = "e.g. Kondwani",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    BasicTextField(
                                        value = firstName,
                                        onValueChange = {
                                            firstName = it
                                            errorMessage = null
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 14.sp,
                                            color = Color(0xFF0F172A)
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // Last Name
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = "Last Name",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                Box(
                                    modifier = Modifier.weight(1f),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    if (lastName.isEmpty()) {
                                        Text(
                                            text = "e.g. Phiri",
                                            fontSize = 13.sp,
                                            color = Color(0xFF94A3B8)
                                        )
                                    }

                                    BasicTextField(
                                        value = lastName,
                                        onValueChange = {
                                            lastName = it
                                            errorMessage = null
                                        },
                                        singleLine = true,
                                        textStyle = TextStyle(
                                            fontSize = 14.sp,
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

            // Mobile Phone Number
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Phone Number (+265)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+265",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF0F5FA))
                                .padding(horizontal = 14.dp, vertical = 14.dp)
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                if (phoneNumber.isEmpty()) {
                                    Text(
                                        text = "992929988",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = phoneNumber,
                                    onValueChange = { input ->
                                        if (input.length <= 9 && input.all { it.isDigit() }) {
                                            phoneNumber = input
                                            errorMessage = null
                                        }
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (phoneNumber.isNotEmpty() && phoneNumber.length < 9) {
                        Text(
                            text = "Phone number must be exactly 9 digits (${phoneNumber.length}/9)",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626)
                        )
                    } else if (phoneNumber.length == 9) {
                        Text(
                            text = "✓ Exactly 9 digits entered",
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Email Address
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Email Address",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 14.dp, vertical = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Email,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (email.isEmpty()) {
                                    Text(
                                        text = "hopesonmwangonde@gmail.com",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = email,
                                    onValueChange = {
                                        email = it
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    if (email.isNotEmpty() && !isEmailValid) {
                        Text(
                            text = "Email address must contain '@'",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Set Password
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Password",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Lock,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (password.isEmpty()) {
                                    Text(
                                        text = "Create a secure password",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = password,
                                    onValueChange = {
                                        password = it
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                                Icon(
                                    imageVector = if (isPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (password.isNotEmpty() && !isPasswordFormatValid) {
                        Text(
                            text = "Must be ≥ 8 chars and include a letter, number & symbol",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626)
                        )
                    }
                }
            }

            // Verify Password
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Confirm Password",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF0F5FA))
                            .padding(horizontal = 14.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (verifyPassword.isEmpty()) {
                                    Text(
                                        text = "Re-enter your password",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = verifyPassword,
                                    onValueChange = {
                                        verifyPassword = it
                                        errorMessage = null
                                    },
                                    singleLine = true,
                                    visualTransformation = if (isVerifyPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                    textStyle = TextStyle(
                                        fontSize = 14.sp,
                                        color = Color(0xFF0F172A)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            IconButton(onClick = {
                                isVerifyPasswordVisible = !isVerifyPasswordVisible
                            }) {
                                Icon(
                                    imageVector = if (isVerifyPasswordVisible) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = "Toggle Password Visibility",
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }

                    if (verifyPassword.isNotEmpty() && !isPasswordMatching) {
                        Text(
                            text = "Passwords do not match",
                            fontSize = 11.sp,
                            color = Color(0xFFDC2626)
                        )
                    } else if (isPasswordMatching) {
                        Text(
                            text = "✓ Passwords match",
                            fontSize = 11.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Primary Wash Location
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Primary Wash Location",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )

                    // 2x2 Grid of Locations
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LocationChip(
                                name = "Lilongwe",
                                isSelected = selectedLocation == "Lilongwe",
                                onClick = { selectedLocation = "Lilongwe" },
                                modifier = Modifier.weight(1f)
                            )

                            LocationChip(
                                name = "Blantyre",
                                isSelected = selectedLocation == "Blantyre",
                                onClick = { selectedLocation = "Blantyre" },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            LocationChip(
                                name = "Mzuzu",
                                isSelected = selectedLocation == "Mzuzu",
                                onClick = { selectedLocation = "Mzuzu" },
                                modifier = Modifier.weight(1f)
                            )

                            LocationChip(
                                name = "Zomba",
                                isSelected = selectedLocation == "Zomba",
                                onClick = { selectedLocation = "Zomba" },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            // Custom Styled Checkbox: Terms & Privacy Policy (White Box, Black Boundary Stroke, Blue Tick Inside, Empty Initially)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isTermsAccepted) Color(0xFFEBF2FF) else Color(0xFFF8FAFC)
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isTermsAccepted) Color(0xFF0284C7) else Color(0xFFCBD5E1)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // White box with black boundary stroke and blue tick inside
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color.White)
                                .border(1.5.dp, Color(0xFF0F172A), RoundedCornerShape(6.dp))
                                .clickable {
                                    isTermsAccepted = !isTermsAccepted
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isTermsAccepted) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = Color(0xFF0284C7),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "I agree to IWALE Car's ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Terms of Service",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        activeLegalDocument = LegalDocumentType.TERMS_OF_SERVICE
                                    }
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "and ",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                                Text(
                                    text = "Privacy Policy",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0284C7),
                                    textDecoration = TextDecoration.Underline,
                                    modifier = Modifier.clickable {
                                        activeLegalDocument = LegalDocumentType.PRIVACY_POLICY
                                    }
                                )
                                Text(
                                    text = ".",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0F172A)
                                )
                            }
                        }
                    }
                }
            }

            // Submit Button
            item {
                Button(
                    onClick = { attemptRegistration() },
                    enabled = !isLoading,
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
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Create Account & Verify Phone  →",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Link: Already have an account? Sign In
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Already have an account? ",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )

                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF006699),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onSignInClick() }
                    )
                }
            }
        }
    }

    // Modal Popup for Terms of Service & Privacy Policy
    if (activeLegalDocument != null) {
        val docType = activeLegalDocument!!
        val isTerms = docType == LegalDocumentType.TERMS_OF_SERVICE
        val title = if (isTerms) "Terms of Service" else "Privacy Policy"
        val bodyText = if (isTerms) {
            """IWALE CAR - TERMS OF SERVICE

1. REGISTRATION RESPONSIBILITIES
Users must provide accurate, current, and complete information during registration (including first name, last name, phone number, and email address). Users are responsible for maintaining the confidentiality of their account credentials and for all activities under their account.

2. CUSTOMER BOOKINGS & CANCELLATIONS
Customers may select available car wash facilities, service bays, dates, and times. Bookings are subject to availability and facility confirmation. Cancellations must be performed within the applicable facility's notice period.

3. PAYMENTS & TRANSACTIONS
All payments made through mobile money or supported payment methods are processed securely. Users agree to pay all applicable fees for selected car wash services.

4. FACILITY RESPONSIBILITIES
Registered car wash facilities are responsible for providing services safely, professionally, and in accordance with scheduled booking times and listed service specifications.

5. USER CONDUCT
Users agree not to misuse the platform, attempt unauthorized access, upload harmful content, submit fraudulent bookings, or engage in abusive behavior towards facility staff or administrators.

6. ACCOUNT SUSPENSION & TERMINATION
IWALE Car reserves the right to suspend or terminate accounts that violate these Terms, engage in fraudulent activities, or disrupt service operations.

7. DISPUTE RESOLUTION
Any disputes arising from service bookings or platform usage shall be resolved through good-faith negotiation between the affected parties or through IWALE Car customer support mediation."""
        } else {
            """IWALE CAR - PRIVACY POLICY

1. PERSONAL INFORMATION COLLECTION
We collect information you provide directly during registration and account usage, including your first name, last name, phone number, email address, primary wash location, and booking preferences.

2. FIREBASE STORAGE & INFRASTRUCTURE
Your account data, authentication credentials, and user profile information are securely stored using Google Firebase Authentication and Cloud Firestore infrastructure.

3. USE OF INFORMATION
Collected information is used to manage your account, process car wash reservations, communicate booking updates, personalize location preferences, and improve application functionality.

4. SHARING WITH CAR WASH FACILITIES
When you place a booking, necessary information (such as your name, contact phone number, and booking time) is shared with the selected car wash facility to fulfill your service.

5. DATA SECURITY
We implement industry-standard security measures and role-based access controls to safeguard your personal data against unauthorized access, alteration, or disclosure.

6. DATA RETENTION
Your personal information is retained for as long as your account remains active or as needed to provide booking services, resolve disputes, and comply with operational requirements.

7. PRIVACY RIGHTS
You have the right to view, update, or correct your personal profile information at any time through the application settings."""
        }

        AlertDialog(
            onDismissRequest = { activeLegalDocument = null },
            title = {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        Text(
                            text = bodyText,
                            fontSize = 12.sp,
                            color = Color(0xFF334155),
                            lineHeight = 18.sp
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isTermsAccepted = true
                        errorMessage = null
                        activeLegalDocument = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "I Agree",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            dismissButton = {
                Button(
                    onClick = {
                        isTermsAccepted = false
                        activeLegalDocument = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "I Disagree",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
private fun LocationChip(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0xFF0B132B) else Color(0xFFF0F5FA))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isSelected) Color.White else Color(0xFF0F172A)
            )

            if (isSelected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SignUpScreenPreview() {
    MyApplicationTheme {
        SignUpScreen()
    }
}
