package com.example.myapplication.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholderText: String = "Search express bays...",
    onFilterClick: () -> Unit = {}
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        placeholder = {
            Text(
                text = placeholderText,
                color = Color(0xFF94A3B8),
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = CarWashIcons.Search,
                contentDescription = "Search",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(20.dp)
            )
        },
        trailingIcon = {
            Icon(
                imageVector = CarWashIcons.FilterTune,
                contentDescription = "Filter",
                tint = Color(0xFF334155),
                modifier = Modifier
                    .size(20.dp)
                    .clickable { onFilterClick() }
            )
        },
        textStyle = TextStyle(
            fontSize = 14.sp,
            color = Color(0xFF0F172A)
        ),
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White,
            focusedBorderColor = Color(0xFFE2E8F0),
            unfocusedBorderColor = Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
    )
}
