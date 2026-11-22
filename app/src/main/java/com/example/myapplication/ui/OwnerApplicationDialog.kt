package com.example.myapplication.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.myapplication.data.OwnerApplication
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.OwnerApplicationValidator
import com.example.myapplication.data.UserAccount
import com.example.myapplication.ui.theme.PrimaryBlue
import kotlinx.coroutines.launch

/**
 * Pop-up shown to a car wash owner applicant after signing in, to apply for owner verification.
 * Text styling uses solid black color (0xFF0F172A) and Medium boldness across all labels/content.
 */
@Composable
fun OwnerApplicationDialog(
    applicant: UserAccount,
    previous: OwnerApplication? = null,
    onDismiss: () -> Unit,
    onSubmitted: () -> Unit
) {
    var businessName by remember { mutableStateOf(previous?.businessName ?: "") }
    var registrationNumber by remember { mutableStateOf(previous?.registrationNumber ?: "") }
    var businessAddress by remember { mutableStateOf(previous?.businessAddress ?: "") }
    var city by remember { mutableStateOf(previous?.city ?: applicant.primaryLocation) }
    var numberOfBays by remember { mutableStateOf(previous?.numberOfBays?.toString() ?: "") }
    var experienceYears by remember { mutableStateOf(previous?.experienceYears?.takeIf { it > 0 }?.toString() ?: "") }
    var description by remember { mutableStateOf(previous?.description ?: "") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val fieldTextStyle = TextStyle(
        color = Color(0xFF0F172A),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp
    )

    fun submit() {
        if (isSubmitting) return
        val validationError = OwnerApplicationValidator.validate(
            businessName, businessAddress, city, numberOfBays, experienceYears
        )
        if (validationError != null) {
            errorMessage = validationError
            return
        }
        errorMessage = null
        isSubmitting = true
        scope.launch {
            val result = OwnerApplicationRepository.submit(
                applicant = applicant,
                businessName = businessName,
                registrationNumber = registrationNumber,
                businessAddress = businessAddress,
                city = city,
                numberOfBays = numberOfBays,
                experienceYears = experienceYears,
                description = description
            )
            isSubmitting = false
            if (result.isSuccess) onSubmitted()
            else errorMessage = result.exceptionOrNull()?.message ?: "Could not submit your application."
        }
    }

    Dialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        properties = DialogProperties(dismissOnClickOutside = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color.White,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 640.dp)
                .testTag("owner_application_dialog")
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Car Wash Owner Application",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
                Text(
                    text = "Tell us about your business. An administrator will review your application and, once approved, you will get access to the owner dashboard.",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF0F172A),
                    lineHeight = 18.sp
                )

                OutlinedTextField(
                    value = businessName,
                    onValueChange = { businessName = it },
                    label = { Text("Business name *", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                    singleLine = true,
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth().testTag("app_business_name")
                )
                OutlinedTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it },
                    label = { Text("Business registration no. (optional)", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                    singleLine = true,
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth().testTag("app_registration")
                )
                OutlinedTextField(
                    value = businessAddress,
                    onValueChange = { businessAddress = it },
                    label = { Text("Business address *", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth().testTag("app_address")
                )
                OutlinedTextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("City *", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                    singleLine = true,
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth().testTag("app_city")
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = numberOfBays,
                        onValueChange = { numberOfBays = it.filter(Char::isDigit).take(2) },
                        label = { Text("No. of bays *", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                        singleLine = true,
                        textStyle = fieldTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("app_bays")
                    )
                    OutlinedTextField(
                        value = experienceYears,
                        onValueChange = { experienceYears = it.filter(Char::isDigit).take(2) },
                        label = { Text("Years in business", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                        singleLine = true,
                        textStyle = fieldTextStyle,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f).testTag("app_experience")
                    )
                }
                OutlinedTextField(
                    value = description,
                    onValueChange = { if (it.length <= 500) description = it },
                    label = { Text("Services & description (optional)", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium) },
                    minLines = 3,
                    textStyle = fieldTextStyle,
                    modifier = Modifier.fillMaxWidth().testTag("app_description")
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.testTag("app_error")
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { submit() },
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("app_submit")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Submit Application", fontWeight = FontWeight.Medium, color = Color.White)
                    }
                }
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !isSubmitting,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp).testTag("app_later")
                ) {
                    Text("Fill in later", color = Color(0xFF0F172A), fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
