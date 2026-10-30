package com.example.myapplication.ui

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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.rememberCoroutineScope
import com.example.myapplication.R
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.ui.theme.MyApplicationTheme
import kotlinx.coroutines.launch

@Composable
fun ForgotPasswordScreen(
    onBackClick: () -> Unit = {},
    onSignInClick: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val coroutineScope = rememberCoroutineScope()

    if (showDialog) {
        AlertDialog(
            onDismissRequest = {
                showDialog = false
                onSignInClick()
            },
            title = {
                Text(
                    text = "Reset Link Sent",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "A password reset link has been sent to ${if (email.isNotEmpty()) email else "your email address"}. Please check your inbox and spam folder.",
                    fontSize = 14.sp,
                    color = Color(0xFF475569)
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDialog = false
                        onSignInClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF0B132B),
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = "Back to Sign In",
                        fontWeight = FontWeight.Bold
                    )
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
                            contentDescription = "Back to Sign In",
                            tint = Color(0xFF0F172A)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Forgot Password",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A)
                    )
                }
            }
        },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top Card Banner
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEBF2FF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_iwale_wash_logo),
                            contentDescription = "IWALE Logo",
                            modifier = Modifier.size(90.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Forgot Password?",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "Enter your registered email address and we will send you a secure password reset link.",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Normal,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            // Email Address Input Field
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Email Address",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = "Account verification",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                    }

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
                                        text = "e.g example@gmail.com",
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
                }
            }

            item {
                errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = Color.Red,
                        fontSize = 13.sp,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Delivery Check Box
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEBF2FF))
                        .padding(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = Color(0xFF0284C7),
                            modifier = Modifier.size(20.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "Delivery Check",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = "Check your spam if you don't receive an email within 2 minutes. Links expire in 15 minutes.",
                                fontSize = 12.sp,
                                color = Color(0xFF475569),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Send Reset Link Button
            item {
                Button(
                    onClick = {
                        if (isLoading) return@Button
                        errorMessage = null
                        isLoading = true
                        coroutineScope.launch {
                            val result = AuthRepository.resetPassword(email)
                            isLoading = false
                            if (result.isSuccess) {
                                showDialog = true
                            } else {
                                errorMessage = result.exceptionOrNull()?.message
                                    ?: "Unable to send password reset email."
                            }
                        }
                    },
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
                            text = "Send Reset Link  →",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Bottom Link: Remember your password? Sign In
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Remember your password? ",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B)
                    )

                    Text(
                        text = "Sign In",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0284C7),
                        textDecoration = TextDecoration.Underline,
                        modifier = Modifier.clickable { onSignInClick() }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun ForgotPasswordScreenPreview() {
    MyApplicationTheme {
        ForgotPasswordScreen()
    }
}
