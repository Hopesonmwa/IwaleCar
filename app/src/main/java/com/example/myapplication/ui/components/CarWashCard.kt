package com.example.myapplication.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.myapplication.R
import com.example.myapplication.data.BadgeType
import com.example.myapplication.data.CarWashFacility
import com.example.myapplication.ui.theme.PrimaryBlue

@Composable
fun CarWashCard(
    facility: CarWashFacility,
    onViewFacilityClick: (CarWashFacility) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onViewFacilityClick(facility) },

        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Image Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
            ) {
                CarWashBayPlaceholder(badgeType = facility.badgeType)

                // Overlay Badge at bottom right of image
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(12.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xE60F172A))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        val icon = when (facility.badgeType) {
                            BadgeType.HIGH_PRESSURE_JET -> CarWashIcons.HighPressureCar
                            BadgeType.SUPER_FOAM -> CarWashIcons.SuperFoamIcon
                        }

                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = facility.badgeText,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            // Card Content Area
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    // Left Column: Title + Subtitle
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = facility.name,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = CarWashIcons.LocationPin,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(14.dp)
                            )

                            Spacer(modifier = Modifier.width(2.dp))

                            Text(
                                text = facility.location,
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Right Column: Starts at + MWK 2,000 + / 30 min
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "Starts at",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )

                        Text(
                            text = facility.price,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0F172A)
                        )

                        Text(
                            text = facility.duration,
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // View Facility Button at bottom right
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (facility.isPrimaryAction) {
                        Button(
                            onClick = { onViewFacilityClick(facility) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue,
                                contentColor = Color.White
                            ),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(
                                text = "View Facility  >",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { onViewFacilityClick(facility) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDCE6F8),
                                contentColor = Color(0xFF0F172A)
                            ),
                            modifier = Modifier.height(42.dp)
                        ) {
                            Text(
                                text = "View Facility  >",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CarWashBayPlaceholder(badgeType: BadgeType) {
    if (badgeType == BadgeType.HIGH_PRESSURE_JET) {
        Image(
            painter = painterResource(id = R.drawable.capital_sparkle_car_wash),
            contentDescription = "Capital Sparkle Car Wash",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
    } else {
        val gradientColors = listOf(Color(0xFF0F766E), Color(0xFF14B8A6), Color(0xFF5EEAD4))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height

                // Steel bay arches / structure
                drawRect(
                    color = Color(0x33FFFFFF),
                    topLeft = Offset(width * 0.1f, height * 0.15f),
                    size = Size(width * 0.8f, height * 0.08f)
                )

                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(width * 0.15f, height * 0.23f),
                    size = Size(width * 0.04f, height * 0.77f)
                )

                drawRect(
                    color = Color(0x22FFFFFF),
                    topLeft = Offset(width * 0.81f, height * 0.23f),
                    size = Size(width * 0.04f, height * 0.77f)
                )

                // Spray line graphics
                for (i in 1..8) {
                    drawLine(
                        color = Color(0x44FFFFFF),
                        start = Offset(width * (0.2f + i * 0.08f), height * 0.23f),
                        end = Offset(width * (0.15f + i * 0.08f), height * 0.8f),
                        strokeWidth = 3f
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "AUTOWASH BAY",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
