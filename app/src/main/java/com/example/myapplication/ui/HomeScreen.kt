package com.example.myapplication.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.data.AreaFilter
import com.example.myapplication.data.CarWashFacility
import com.example.myapplication.data.FacilityRepository
import com.example.myapplication.data.UserRepository
import com.example.myapplication.data.greetingName
import com.example.myapplication.ui.components.AreaFilterSection
import com.example.myapplication.ui.components.BottomNavigationBar
import com.example.myapplication.ui.components.CarWashCard
import com.example.myapplication.ui.components.GreetingSection
import com.example.myapplication.ui.components.HomeHeader
import com.example.myapplication.ui.components.NavItem
import com.example.myapplication.ui.components.SearchBar
import com.example.myapplication.ui.components.SelfServiceBanner
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.example.myapplication.ui.theme.PrimaryBlue

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    onFacilityClick: (CarWashFacility) -> Unit = {},
    onProfileClick: () -> Unit = {},
    onHistoryClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedAreaId by remember { mutableStateOf("0") } // Default "All"
    var currentTab by remember { mutableStateOf(NavItem.BAYS) }

    val currentUser = UserRepository.getCurrentUser()

    val sampleAreas = remember {
        listOf(
            AreaFilter(id = "0", name = "All", hasIcon = false),
            AreaFilter(id = "1", name = "Lilongwe", hasIcon = false),
            AreaFilter(id = "2", name = "Blantyre", hasIcon = false),
            AreaFilter(id = "3", name = "Zomba", hasIcon = false),
            AreaFilter(id = "4", name = "Mzuzu", hasIcon = false)
        )
    }

    val approvedFacilities = FacilityRepository.getApprovedCarWashFacilities()
    val selectedAreaName = sampleAreas.find { it.id == selectedAreaId }?.name ?: "All"

    // Facilities filtered by selected city & search query
    val filteredFacilities = approvedFacilities.filter { facility ->
        val matchesCity = selectedAreaName.equals("All", ignoreCase = true) ||
                facility.area.contains(selectedAreaName, ignoreCase = true) ||
                facility.location.contains(selectedAreaName, ignoreCase = true)
        val matchesSearch = searchQuery.trim().isEmpty() ||
                facility.name.contains(searchQuery.trim(), ignoreCase = true) ||
                facility.location.contains(searchQuery.trim(), ignoreCase = true) ||
                facility.area.contains(searchQuery.trim(), ignoreCase = true)
        matchesCity && matchesSearch
    }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = currentTab,
                onTabSelected = { tab ->
                    currentTab = tab
                    if (tab == NavItem.PROFILE) {
                        onProfileClick()
                    } else if (tab == NavItem.HISTORY) {
                        onHistoryClick()
                    } else if (tab == NavItem.BOOKINGS) {
                        onBookingsClick()
                    }
                }
            )
        },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8FAFC)),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header: "Home" title + Profile icon
            item {
                HomeHeader(onProfileClick = onProfileClick)
            }

            // User Greeting & Location
            item {
                GreetingSection(
                    userName = currentUser.greetingName,
                    locationName = currentUser.primaryLocation
                )
            }

            // Self Service Info Banner Card
            item {
                SelfServiceBanner()
            }

            // Search Bar
            item {
                SearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it }
                )
            }

            // Area Filter Section
            item {
                AreaFilterSection(
                    areas = sampleAreas,
                    selectedAreaId = selectedAreaId,
                    onAreaSelected = { selectedAreaId = it }
                )
            }

            // Section Header: "Nearby Car Washes"
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Nearby Car Washes",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0F172A)
                )
            }

            if (filteredFacilities.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Storefront,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(40.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))

                            if (searchQuery.trim().isNotEmpty()) {
                                Text(
                                    text = "No car wash facilities found.",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "No car wash stations matched '$searchQuery'. Try searching for a different name or location.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { searchQuery = "" },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = PrimaryBlue,
                                        contentColor = Color.White
                                    )
                                ) {
                                    Text(text = "Clear Search", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                            } else {
                                Text(
                                    text = "No car wash site available yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "There are currently no active express bays listed in $selectedAreaName.",
                                    fontSize = 13.sp,
                                    color = Color(0xFF64748B),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            } else {
                items(filteredFacilities, key = { it.id }) { facility ->
                    CarWashCard(
                        facility = facility,
                        onViewFacilityClick = { onFacilityClick(it) }
                    )
                }
            }

            // Bottom spacing
            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    MyApplicationTheme {
        HomeScreen()
    }
}
