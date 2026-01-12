package com.TOTOMOFYP.VTOAPP.auth

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.lifecycle.viewmodel.compose.viewModel
import com.TOTOMOFYP.VTOAPP.ui.theme.DarkGray
import com.TOTOMOFYP.VTOAPP.ui.theme.LightBackground
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPinkLight

private const val TAG = "RegisterScreen"

@Composable
fun RegisterScreen(
    navController: NavController,
    onRegisterSuccess: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val passwordVisible = remember { mutableStateOf(false) }
    val confirmPasswordVisible = remember { mutableStateOf(false) }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Define the color scheme
    val backgroundColor = LightBackground // White background
    val textColor = Color(0xFF333333) // Dark gray text for readability
    val buttonColor = Color(0xFFDAA520) // Dark yellow (Gold) for primary elements
    val borderColor = Color(0xFFAAAAAA) // Light gray for text field borders

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(top=60.dp,start=20.dp, end=20.dp),
        contentAlignment = Alignment.Center
    ) {
        IconButton(
            onClick = { navController.popBackStack() }, // Navigate back
            modifier = Modifier.align(Alignment.TopStart) // Position at top-left
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack, // Updated to AutoMirrored
                contentDescription = "Back",
                tint = SoftBlushPink // Dark Yellow color
            )
        }

        if (isLoading.value) {
            CircularProgressIndicator(
                color = buttonColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {

            // Header Text
            Text(
                text = "Create Your Account",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = DarkGray // Dark Yellow for Title
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Email Input
            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email", color = textColor) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    backgroundColor = Color.Transparent,
                    focusedBorderColor = SoftBlushPink,
                    unfocusedBorderColor = borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Password Input
            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password", color = textColor) },
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            imageVector = if (passwordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = SoftBlushPink
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    backgroundColor = Color.Transparent,
                    focusedBorderColor = SoftBlushPink,
                    unfocusedBorderColor = borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Confirm Password Input
            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Confirm Password", color = textColor) },
                visualTransformation = if (confirmPasswordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { confirmPasswordVisible.value = !confirmPasswordVisible.value }) {
                        Icon(
                            imageVector = if (confirmPasswordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Confirm Password",
                            tint = SoftBlushPink
                        )
                    }
                },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    backgroundColor = Color.Transparent,
                    focusedBorderColor = SoftBlushPink,
                    unfocusedBorderColor = borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Sign Up Button
            Button(
                onClick = { 
                    if (email.value.isBlank() || password.value.isBlank()) {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (password.value != confirmPassword.value) {
                        Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    if (password.value.length < 6) {
                        Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Show loading indicator
                    isLoading.value = true
                    
                    // Register with Firebase
                    Log.d(TAG, "Registering user with email: ${email.value}")
                    authViewModel.registerWithEmailAndPassword(
                        email = email.value,
                        password = password.value,
                        onSuccess = {
                            Log.d(TAG, "Registration successful")
                            isLoading.value = false
                            onRegisterSuccess()
                        },
                        onError = { errorMsg ->
                            Log.e(TAG, "Registration failed: $errorMsg")
                            isLoading.value = false
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = SoftBlushPink),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading.value
            ) {
                Text("Sign Up", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

