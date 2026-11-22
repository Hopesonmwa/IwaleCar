package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.myapplication.data.ApplicationStatus
import com.example.myapplication.data.AuthRepository
import com.example.myapplication.data.CarWashFacility
import com.example.myapplication.data.CarWashTier
import com.example.myapplication.data.OwnerApplicationRepository
import com.example.myapplication.data.UserAccount
import com.example.myapplication.data.UserRepository
import com.example.myapplication.ui.AdminDashboardScreen
import com.example.myapplication.ui.AdminFacilitiesScreen
import com.example.myapplication.ui.AdminNotificationsScreen
import com.example.myapplication.ui.AdminOwnerApplicationsScreen
import com.example.myapplication.ui.AdminProfileScreen
import com.example.myapplication.ui.BookedSessionsScreen
import com.example.myapplication.ui.BookingScreen
import com.example.myapplication.ui.DetailsScreen
import com.example.myapplication.ui.ForgotPasswordScreen
import com.example.myapplication.ui.HistoryScreen
import com.example.myapplication.ui.HomeScreen
import com.example.myapplication.ui.LoginScreen
import com.example.myapplication.ui.ManageBaysScreen
import com.example.myapplication.ui.NotificationsScreen
import com.example.myapplication.ui.OwnerBookingsScreen
import com.example.myapplication.ui.OwnerDashboardScreen
import com.example.myapplication.ui.OwnerApplicationScreen
import com.example.myapplication.ui.OwnerFacilitiesScreen
import com.example.myapplication.ui.OwnerOnboardingScreen
import com.example.myapplication.ui.OwnerRevenueScreen
import com.example.myapplication.ui.PaymentScreen
import com.example.myapplication.ui.ProfileScreen
import com.example.myapplication.ui.SignUpScreen
import com.example.myapplication.ui.SplashScreen
import com.example.myapplication.ui.theme.MyApplicationTheme
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.launch

enum class Screen {
    SPLASH,
    LOGIN,
    FORGOT_PASSWORD,
    SIGNUP,
    HOME,
    DETAILS,
    BOOKING,
    PAYMENT,
    PROFILE,
    HISTORY,
    BOOKED_SESSIONS,
    OWNER_DASHBOARD,
    OWNER_FACILITIES,
    MANAGE_BAYS,
    OWNER_BOOKINGS,
    OWNER_REVENUE,
    NOTIFICATIONS,
    ADMIN_DASHBOARD,
    ADMIN_FACILITIES,
    ADMIN_NOTIFICATIONS,
    ADMIN_PROFILE,
    ADMIN_APPLICATIONS,
    OWNER_ONBOARDING,
    OWNER_APPLICATION
}

/**
 * Decides where a freshly authenticated user lands:
 * administrators -> admin dashboard, approved owners -> owner dashboard,
 * owner applicants (registered as Car Wash Owner but not yet approved) -> owner application screen,
 * everyone else -> customer home.
 */
fun routeForUser(user: UserAccount?): Screen = when {
    user == null -> Screen.HOME
    user.email.equals("admin@gmail.com", ignoreCase = true) ||
            user.email.equals("chisomo.banda@iwalecar.mw", ignoreCase = true) ||
            user.role == "Administrator" ||
            user.role == "SuperAdmin" -> Screen.ADMIN_DASHBOARD
    user.role == "Car Wash Owner" ||
            OwnerApplicationRepository.getApplicationFor(user.email)?.status == ApplicationStatus.APPROVED -> Screen.OWNER_DASHBOARD
    user.requestedRole == "Car Wash Owner" -> Screen.OWNER_ONBOARDING
    else -> Screen.HOME
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val firebaseApp = FirebaseApp.initializeApp(this)

        if (firebaseApp == null) {
            android.util.Log.e(
                "IWALE_FIREBASE",
                "Firebase initialization failed"
            )
        } else {
            android.util.Log.d(
                "IWALE_FIREBASE",
                "Firebase initialized successfully"
            )
        }
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                MainAppLayout()
            }
        }
    }
}

@Composable
fun MainAppLayout() {
    val scope = rememberCoroutineScope()
    var currentScreen by remember { mutableStateOf(Screen.SPLASH) }
    var loginSuccessMessage by remember { mutableStateOf<String?>(null) }
    var selectedFacility by remember { mutableStateOf<CarWashFacility?>(null) }
    var selectedTier by remember {
        mutableStateOf(
            CarWashTier(
                id = "3",
                title = "40 Minutes- Deep Clean",
                description = "Full exterior wax, high-suction vacuum & mats blast",
                price = 15000,
                formattedPrice = "MWK 15,000"
            )
        )
    }
    var selectedBayId by remember { mutableStateOf("1_1") }
    var selectedStartTimeMs by remember { mutableStateOf(System.currentTimeMillis() + 3600_000L) }
    var selectedEndTimeMs by remember { mutableStateOf(System.currentTimeMillis() + 5400_000L) }

    Crossfade(targetState = currentScreen, label = "ScreenNavigation") { screen ->
        when (screen) {
            Screen.SPLASH -> {
                SplashScreen {
                    scope.launch {
                        val restored = AuthRepository.restoreSession().getOrNull()
                        currentScreen = if (restored != null && restored.email.isNotBlank()) {
                            routeForUser(restored)
                        } else {
                            Screen.LOGIN
                        }
                    }
                }
            }
            Screen.LOGIN -> {
                LoginScreen(
                    onSignInClick = { user ->
                        loginSuccessMessage = null
                        currentScreen = routeForUser(user)
                    },
                    onCreateAccountClick = {
                        loginSuccessMessage = null
                        currentScreen = Screen.SIGNUP
                    },
                    onForgotPasswordClick = {
                        loginSuccessMessage = null
                        currentScreen = Screen.FORGOT_PASSWORD
                    },
                    successMessage = loginSuccessMessage
                )
            }
            Screen.FORGOT_PASSWORD -> {
                ForgotPasswordScreen(
                    onBackClick = {
                        currentScreen = Screen.LOGIN
                    },
                    onSignInClick = {
                        currentScreen = Screen.LOGIN
                    }
                )
            }
            Screen.SIGNUP -> {
                SignUpScreen(
                    onSignUpClick = { email ->
                        val registeredUser = UserRepository.findUser(email)
                        AuthRepository.logout()
                        loginSuccessMessage =
                            if (registeredUser?.requestedRole == "Car Wash Owner") {
                                "Account created! Sign in to complete your Car Wash Owner application."
                            } else {
                                "Account created successfully! Please sign in with your credentials."
                            }
                        currentScreen = Screen.LOGIN
                    },
                    onSignInClick = {
                        loginSuccessMessage = null
                        currentScreen = Screen.LOGIN
                    }
                )
            }
            Screen.HOME -> {
                HomeScreen(
                    modifier = Modifier.fillMaxSize(),
                    onFacilityClick = { facility ->
                        selectedFacility = facility
                        currentScreen = Screen.DETAILS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onHistoryClick = {
                        currentScreen = Screen.HISTORY
                    },
                    onBookingsClick = {
                        currentScreen = Screen.BOOKED_SESSIONS
                    }
                )
            }
            Screen.DETAILS -> {
                DetailsScreen(
                    facility = selectedFacility,
                    onBackClick = {
                        currentScreen = Screen.HOME
                    },
                    onBookClick = { tier ->
                        selectedTier = tier
                        currentScreen = Screen.BOOKING
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.BOOKING -> {
                BookingScreen(
                    facility = selectedFacility,
                    tier = selectedTier,
                    onBackClick = {
                        currentScreen = Screen.DETAILS
                    },
                    onProceedToPayment = { bayId, startMs, endMs ->
                        selectedBayId = bayId
                        selectedStartTimeMs = startMs
                        selectedEndTimeMs = endMs
                        currentScreen = Screen.PAYMENT
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.PAYMENT -> {
                PaymentScreen(
                    tier = selectedTier,
                    bayId = selectedBayId,
                    startTimeMs = selectedStartTimeMs,
                    endTimeMs = selectedEndTimeMs,
                    onBackClick = {
                        currentScreen = Screen.BOOKING
                    },
                    onPaymentSuccess = {
                        currentScreen = Screen.BOOKED_SESSIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onBaysClick = {
                        currentScreen = Screen.HOME
                    },
                    onHistoryClick = {
                        currentScreen = Screen.HISTORY
                    },
                    onBookingsClick = {
                        currentScreen = Screen.BOOKED_SESSIONS
                    }
                )
            }
            Screen.PROFILE -> {
                ProfileScreen(
                    onLogoutClick = {
                        AuthRepository.logout()
                        currentScreen = Screen.LOGIN
                    },
                    onBaysClick = {
                        currentScreen = Screen.HOME
                    },
                    onHistoryClick = {
                        currentScreen = Screen.HISTORY
                    },
                    onBookingsClick = {
                        currentScreen = Screen.BOOKED_SESSIONS
                    }
                )
            }
            Screen.HISTORY -> {
                HistoryScreen(
                    onBaysClick = {
                        currentScreen = Screen.HOME
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onBookingsClick = {
                        currentScreen = Screen.BOOKED_SESSIONS
                    }
                )
            }
            Screen.BOOKED_SESSIONS -> {
                BookedSessionsScreen(
                    onBaysClick = {
                        currentScreen = Screen.HOME
                    },
                    onHistoryClick = {
                        currentScreen = Screen.HISTORY
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onExtendPaymentClick = { extensionTier ->
                        selectedTier = extensionTier
                        currentScreen = Screen.PAYMENT
                    }
                )
            }
            Screen.OWNER_DASHBOARD -> {
                OwnerDashboardScreen(
                    onFacilitiesClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onBookingsClick = {
                        currentScreen = Screen.OWNER_BOOKINGS
                    },
                    onRevenueClick = {
                        currentScreen = Screen.OWNER_REVENUE
                    },
                    onNotificationsClick = {
                        currentScreen = Screen.NOTIFICATIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onManageBaysClick = {
                        currentScreen = Screen.MANAGE_BAYS
                    }
                )
            }
            Screen.OWNER_FACILITIES -> {
                OwnerFacilitiesScreen(
                    onDashboardClick = {
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onBookingsClick = {
                        currentScreen = Screen.OWNER_BOOKINGS
                    },
                    onRevenueClick = {
                        currentScreen = Screen.OWNER_REVENUE
                    },
                    onNotificationsClick = {
                        currentScreen = Screen.NOTIFICATIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    },
                    onManageBaysClick = {
                        currentScreen = Screen.MANAGE_BAYS
                    }
                )
            }
            Screen.MANAGE_BAYS -> {
                ManageBaysScreen(
                    onBackClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onDashboardClick = {
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onBookingsClick = {
                        currentScreen = Screen.OWNER_BOOKINGS
                    },
                    onRevenueClick = {
                        currentScreen = Screen.OWNER_REVENUE
                    },
                    onNotificationsClick = {
                        currentScreen = Screen.NOTIFICATIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.OWNER_BOOKINGS -> {
                OwnerBookingsScreen(
                    onDashboardClick = {
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onRevenueClick = {
                        currentScreen = Screen.OWNER_REVENUE
                    },
                    onNotificationsClick = {
                        currentScreen = Screen.NOTIFICATIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.OWNER_REVENUE -> {
                OwnerRevenueScreen(
                    onDashboardClick = {
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onBookingsClick = {
                        currentScreen = Screen.OWNER_BOOKINGS
                    },
                    onNotificationsClick = {
                        currentScreen = Screen.NOTIFICATIONS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.NOTIFICATIONS -> {
                NotificationsScreen(
                    onDashboardClick = {
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.OWNER_FACILITIES
                    },
                    onBookingsClick = {
                        currentScreen = Screen.OWNER_BOOKINGS
                    },
                    onRevenueClick = {
                        currentScreen = Screen.OWNER_REVENUE
                    },
                    onManageBaysClick = {
                        currentScreen = Screen.MANAGE_BAYS
                    },
                    onProfileClick = {
                        currentScreen = Screen.PROFILE
                    }
                )
            }
            Screen.ADMIN_DASHBOARD -> {
                AdminDashboardScreen(
                    onApplicationsClick = {
                        currentScreen = Screen.ADMIN_APPLICATIONS
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.ADMIN_FACILITIES
                    },
                    onAlertsClick = {
                        currentScreen = Screen.ADMIN_NOTIFICATIONS
                    },
                    onAccountClick = {
                        currentScreen = Screen.ADMIN_PROFILE
                    }
                )
            }
            Screen.ADMIN_APPLICATIONS -> {
                AdminOwnerApplicationsScreen(
                    onBackClick = { currentScreen = Screen.ADMIN_DASHBOARD },
                    onDashboardClick = { currentScreen = Screen.ADMIN_DASHBOARD },
                    onFacilitiesClick = { currentScreen = Screen.ADMIN_FACILITIES },
                    onAlertsClick = { currentScreen = Screen.ADMIN_NOTIFICATIONS },
                    onAccountClick = { currentScreen = Screen.ADMIN_PROFILE }
                )
            }
            Screen.ADMIN_FACILITIES -> {
                AdminFacilitiesScreen(
                    onDashboardClick = {
                        currentScreen = Screen.ADMIN_DASHBOARD
                    },
                    onAlertsClick = {
                        currentScreen = Screen.ADMIN_NOTIFICATIONS
                    },
                    onAccountClick = {
                        currentScreen = Screen.ADMIN_PROFILE
                    }
                )
            }
            Screen.ADMIN_NOTIFICATIONS -> {
                AdminNotificationsScreen(
                    onDashboardClick = {
                        currentScreen = Screen.ADMIN_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.ADMIN_FACILITIES
                    },
                    onAccountClick = {
                        currentScreen = Screen.ADMIN_PROFILE
                    }
                )
            }
            Screen.ADMIN_PROFILE -> {
                AdminProfileScreen(
                    onLogoutClick = {
                        AuthRepository.logout()
                        currentScreen = Screen.LOGIN
                    },
                    onDashboardClick = {
                        currentScreen = Screen.ADMIN_DASHBOARD
                    },
                    onFacilitiesClick = {
                        currentScreen = Screen.ADMIN_FACILITIES
                    },
                    onAlertsClick = {
                        currentScreen = Screen.ADMIN_NOTIFICATIONS
                    }
                )
            }
            Screen.OWNER_ONBOARDING -> {
                OwnerOnboardingScreen(
                    onContinueToCustomerClick = {
                        currentScreen = Screen.HOME
                    },
                    onApprovedContinueClick = {
                        UserRepository.setCurrentUser(
                            UserRepository.getCurrentUser().copy(role = "Car Wash Owner")
                        )
                        currentScreen = Screen.OWNER_DASHBOARD
                    },
                    onSignOutClick = {
                        AuthRepository.logout()
                        loginSuccessMessage = null
                        currentScreen = Screen.LOGIN
                    },
                    onOpenApplicationFormClick = {
                        currentScreen = Screen.OWNER_APPLICATION
                    }
                )
            }
            Screen.OWNER_APPLICATION -> {
                OwnerApplicationScreen(
                    onBackClick = {
                        currentScreen = Screen.OWNER_ONBOARDING
                    },
                    onSubmitted = {
                        currentScreen = Screen.OWNER_ONBOARDING
                    }
                )
            }
        }
    }
}
