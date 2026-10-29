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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import kotlinx.coroutines.launch

enum class UserRole(val title: String) {
    USER("User"),
    OWNER("Car Wash Owner")
}

@Composable
fun SignUpScreen(
    onSignUpClick: (String) -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var fullName by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var verifyPassword by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }
    var isVerifyPasswordVisible by remember { mutableStateOf(false) }
    var selectedRole by remember { mutableStateOf<UserRole?>(UserRole.USER) }
    var selectedLocation by remember { mutableStateOf("Lilongwe") }
    var isTermsAccepted by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Validation Flags
    val isPhoneValid = ValidationUtils.isPhoneValid(phoneNumber)
    val isEmailValid = ValidationUtils.isEmailValid(email)
    val isPasswordFormatValid = ValidationUtils.isPasswordComplex(password)
    val isPasswordMatching = password.isNotEmpty() && password == verifyPassword

    val isFormValid = fullName.trim().isNotEmpty() &&
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
        val newUser = UserAccount(
            fullName = fullName.trim(),
            phoneNumber = phoneNumber.trim(),
            email = email.trim(),
            password = password,
            role = selectedRole?.title ?: "User",
            primaryLocation = selectedLocation
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
                                        imageVector = if (role == UserRole.USER) Icons.Outlined.Person else Icons.Outlined.Storefront,
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

            // Full Name
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Full Name",
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
                                modifier = Modifier.size(20.dp)
                            )

                            Spacer(modifier = Modifier.width(10.dp))

                            Box(
                                modifier = Modifier.weight(1f),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (fullName.isEmpty()) {
                                    Text(
                                        text = "e.g. Kondwani Phiri",
                                        fontSize = 14.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }

                                BasicTextField(
                                    value = fullName,
                                    onValueChange = {
                                        fullName = it
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
                                        text = "hopesonmwangongonde@gmail.com",
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
                            text = "Email address must contain '@gmail.com'",
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

            // Checkbox: Terms & Privacy Policy
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
                        Checkbox(
                            checked = isTermsAccepted,
                            onCheckedChange = {
                                isTermsAccepted = it
                                errorMessage = null
                            },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF0B132B),
                                uncheckedColor = Color(0xFF64748B)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "I agree to Iwale Car's Terms of Service and Privacy Policy.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF0F172A),
                            lineHeight = 18.sp
                        )
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
                        containerColor = Color(0xFF0B132B),
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
