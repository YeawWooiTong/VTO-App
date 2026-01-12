package com.TOTOMOFYP.VTOAPP.auth

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material3.Checkbox
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import com.TOTOMOFYP.VTOAPP.ui.theme.LightBackground
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import java.text.SimpleDateFormat
import java.util.*


@Composable
fun UserInfoScreen1(
    navController: NavController,
    onComplete: () -> Unit = {}
) {
    val context = LocalContext.current
    val auth = FirebaseAuth.getInstance()
    val firestore = FirebaseFirestore.getInstance()

    val fullName = remember { mutableStateOf("") }
    val selectedDate = remember { mutableStateOf<Date?>(null) }
    val showDatePicker = remember { mutableStateOf(false) }
    val selectedStyles = remember { mutableStateOf(setOf<String>()) }
    val isDropdownExpanded = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }

    // Format date for display
    val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val formattedDate = remember {
        derivedStateOf {
            selectedDate.value?.let { dateFormatter.format(it) } ?: ""
        }
    }

    val styleOptions = listOf(
        "Casual", "Formal", "Business", "Party", "Wedding",
        "Sports", "Travel", "Loungewear", "Traditional", "Seasonal"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LightBackground)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Back Button (Top Left Corner)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 30.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = SoftBlushPink
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Complete Your Profile",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            Spacer(modifier = Modifier.height(50.dp))

            Text(
                text = "Tell Us About You",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Full Name Input
            OutlinedTextField(
                value = fullName.value,
                onValueChange = { fullName.value = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = SoftBlushPink,
                    unfocusedBorderColor = Color.Gray
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Date of Birth Input with Date Picker
            OutlinedTextField(
                value = formattedDate.value,
                onValueChange = { },
                label = { Text("Date of Birth") },
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showDatePicker.value = true }) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Select Date",
                            tint = SoftBlushPink
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker.value = true },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = SoftBlushPink,
                    unfocusedBorderColor = Color.Gray,
                    textColor = Color.Black
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Style Preferences (Multiple Selection)
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = if (selectedStyles.value.isEmpty()) "" else selectedStyles.value.joinToString(", "),
                    onValueChange = { },
                    label = { Text("Style Preferences (Select multiple)") },
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { isDropdownExpanded.value = !isDropdownExpanded.value }) {
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = SoftBlushPink
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = SoftBlushPink,
                        unfocusedBorderColor = Color.Gray,
                        textColor = Color.Black
                    )
                )

                DropdownMenu(
                    expanded = isDropdownExpanded.value,
                    onDismissRequest = { isDropdownExpanded.value = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    styleOptions.forEach { style ->
                        DropdownMenuItem(
                            onClick = {
                                val currentSelection = selectedStyles.value.toMutableSet()
                                if (currentSelection.contains(style)) {
                                    currentSelection.remove(style)
                                } else {
                                    currentSelection.add(style)
                                }
                                selectedStyles.value = currentSelection
                                // Don't close dropdown for multiple selection
                            }
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                androidx.compose.material3.Checkbox(
                                    checked = selectedStyles.value.contains(style),
                                    onCheckedChange = null // Handled by DropdownMenuItem onClick
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = style,
                                    color = Color.Black,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))

            // Submit Button
            Button(
                onClick = {
                    if (fullName.value.isBlank() || selectedDate.value == null || selectedStyles.value.isEmpty()) {
                        Toast.makeText(context, "Please fill in all fields and select at least one style preference", Toast.LENGTH_SHORT).show()
                    } else {
                        isLoading.value = true
                        saveUserProfile(
                            fullName = fullName.value,
                            dateOfBirth = formattedDate.value,
                            stylePreferences = selectedStyles.value.toList(),
                            auth = auth,
                            firestore = firestore,
                            context = context,
                            navController = navController,
                            onComplete = {
                                isLoading.value = false
                                onComplete() // Call the completion callback to navigate to main app
                            }
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = SoftBlushPink),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = !isLoading.value
            ) {
                if (isLoading.value) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White
                    )
                } else {
                    Text("Complete Profile", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Date Picker Dialog
        if (showDatePicker.value) {
            DatePickerDialog(
                selectedDate = selectedDate.value,
                onDateSelected = { date ->
                    selectedDate.value = date
                    showDatePicker.value = false
                },
                onDismiss = { showDatePicker.value = false }
            )
        }
    }
}

private fun saveUserProfile(
    fullName: String,
    dateOfBirth: String,
    stylePreferences: List<String>,
    auth: FirebaseAuth,
    firestore: FirebaseFirestore,
    context: android.content.Context,
    navController: NavController,
    onComplete: () -> Unit
) {
    val user = auth.currentUser
    if (user != null) {
        val userData = hashMapOf<String, Any>(
            "fullName" to fullName,
            "dateOfBirth" to dateOfBirth,
            "stylePreference" to (stylePreferences.firstOrNull() ?: ""), // Keep first selection for backward compatibility
            "stylePreferences" to stylePreferences, // New multiple preferences field
            "hasCompletedOnboarding" to true,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(user.uid)
            .set(userData, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Profile saved successfully!", Toast.LENGTH_SHORT).show()
                // Navigate to main app - this will be handled by AuthActivity's onboarding observer
                onComplete()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Error saving profile: ${e.message}", Toast.LENGTH_LONG).show()
                onComplete()
            }
    } else {
        Toast.makeText(context, "User not authenticated", Toast.LENGTH_SHORT).show()
        onComplete()
    }
}

@Composable
fun DatePickerDialog(
    selectedDate: Date?,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val calendar = remember { Calendar.getInstance() }

    // Initialize with selected date or default to a reasonable birth year
    val initCalendar = remember {
        Calendar.getInstance().apply {
            selectedDate?.let { time = it } ?: run {
                set(Calendar.YEAR, 2000)
                set(Calendar.MONTH, 0)
                set(Calendar.DAY_OF_MONTH, 1)
            }
        }
    }

    val currentYear = remember { mutableStateOf(initCalendar.get(Calendar.YEAR)) }
    val currentMonth = remember { mutableStateOf(initCalendar.get(Calendar.MONTH)) }
    val selectedDay = remember { mutableStateOf(initCalendar.get(Calendar.DAY_OF_MONTH)) }

    // Month names for better UX
    val monthNames = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    // Calculate calendar grid
    val calendarData = remember(currentYear.value, currentMonth.value) {
        generateCalendarData(currentYear.value, currentMonth.value)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Select Date of Birth",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // Year and Month Navigation
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Year selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = { if (currentYear.value > 1950) currentYear.value-- }
                        ) {
                            Text("◀", color = SoftBlushPink, fontSize = 16.sp)
                        }
                        Box(
                            modifier = Modifier.width(60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${currentYear.value}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        TextButton(
                            onClick = { if (currentYear.value < 2010) currentYear.value++ }
                        ) {
                            Text("▶", color = SoftBlushPink, fontSize = 16.sp)
                        }
                    }

                    // Month selector
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        TextButton(
                            onClick = {
                                if (currentMonth.value > 0) {
                                    currentMonth.value--
                                } else {
                                    currentMonth.value = 11
                                    if (currentYear.value > 1950) currentYear.value--
                                }
                            }
                        ) {
                            Text("◀", color = SoftBlushPink, fontSize = 16.sp)
                        }
                        Box(
                            modifier = Modifier.width(65.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = monthNames[currentMonth.value].take(3),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                        TextButton(
                            onClick = {
                                if (currentMonth.value < 11) {
                                    currentMonth.value++
                                } else {
                                    currentMonth.value = 0
                                    if (currentYear.value < 2010) currentYear.value++
                                }
                            }
                        ) {
                            Text("▶", color = SoftBlushPink, fontSize = 16.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Calendar Grid
                Column {
                    // Week day headers
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                            Box(
                                modifier = Modifier.width(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SoftBlushPink
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar days
                    calendarData.chunked(7).forEach { week ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            week.forEach { dayData ->
                                val isSelected = dayData.day == selectedDay.value &&
                                        dayData.isCurrentMonth &&
                                        currentYear.value == currentYear.value &&
                                        currentMonth.value == currentMonth.value

                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            if (isSelected) SoftBlushPink else Color.Transparent,
                                            RoundedCornerShape(16.dp)
                                        )
                                        .clickable(enabled = dayData.isCurrentMonth) {
                                            if (dayData.isCurrentMonth) {
                                                selectedDay.value = dayData.day
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (dayData.day > 0) dayData.day.toString() else "",
                                        fontSize = 14.sp,
                                        color = when {
                                            isSelected -> Color.White
                                            dayData.isCurrentMonth -> Color.Black
                                            else -> Color.Gray
                                        },
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Selected: ${selectedDay.value} ${monthNames[currentMonth.value]} ${currentYear.value}",
                    fontSize = 14.sp,
                    color = SoftBlushPink,
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    calendar.set(currentYear.value, currentMonth.value, selectedDay.value, 0, 0, 0)
                    calendar.set(Calendar.MILLISECOND, 0)
                    onDateSelected(calendar.time)
                }
            ) {
                Text("OK", color = SoftBlushPink, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        backgroundColor = LightBackground,
        shape = RoundedCornerShape(16.dp)
    )
}

data class CalendarDay(
    val day: Int,
    val isCurrentMonth: Boolean
)

fun generateCalendarData(year: Int, month: Int): List<CalendarDay> {
    val calendar = Calendar.getInstance()
    calendar.set(year, month, 1)

    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val firstDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1 // Sunday = 0

    val calendarDays = mutableListOf<CalendarDay>()

    // Add empty days for the start of the month
    repeat(firstDayOfWeek) {
        calendarDays.add(CalendarDay(0, false))
    }

    // Add days of the current month
    for (day in 1..daysInMonth) {
        calendarDays.add(CalendarDay(day, true))
    }

    // Fill the rest to complete the grid (6 weeks * 7 days = 42 cells)
    while (calendarDays.size < 42) {
        calendarDays.add(CalendarDay(0, false))
    }

    return calendarDays
}
