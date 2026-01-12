package com.TOTOMOFYP.VTOAPP.auth

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.TOTOMOFYP.VTOAPP.R
import com.TOTOMOFYP.VTOAPP.ui.theme.DarkGray
import com.TOTOMOFYP.VTOAPP.ui.theme.LightBackground
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPink
import com.TOTOMOFYP.VTOAPP.ui.theme.SoftBlushPinkLight
import com.google.firebase.auth.FirebaseAuth

private const val TAG = "LoginScreen"

@Composable
fun LoginScreen(
    navController: NavController,
    onLoginSuccess: () -> Unit = {},
    authViewModel: AuthViewModel = viewModel()
) {
    val passwordVisible = remember { mutableStateOf(false) }
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val isLoading = remember { mutableStateOf(false) }
    val context = LocalContext.current
    
    // State for forgot password dialog
    val showForgotPasswordDialog = remember { mutableStateOf(false) }
    val resetEmail = remember { mutableStateOf("") }
    val isResetLoading = remember { mutableStateOf(false) }

    // Updated Theme Colors to match app's Soft Blush Pink theme
    val backgroundColor = LightBackground // Light pink background
    val textColor = DarkGray // Dark gray text
    val buttonColor = SoftBlushPink // Soft blush pink for buttons
    val borderColor = SoftBlushPinkLight // Light pink border for input fields

    // Forgot password dialog
    if (showForgotPasswordDialog.value) {
        AlertDialog(
            onDismissRequest = { 
                showForgotPasswordDialog.value = false 
                resetEmail.value = ""
            },
            title = { Text("Reset Password", color = textColor) },
            text = {
                Column {
                    Text("Please enter your email address to receive a password reset link.", color = textColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail.value,
                        onValueChange = { resetEmail.value = it },
                        label = { Text("Email", color = textColor) },
                        keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            textColor = textColor,
                            backgroundColor = backgroundColor,
                            focusedBorderColor = buttonColor,
                            unfocusedBorderColor = borderColor
                        ),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isResetLoading.value
                    )
                    if (isResetLoading.value) {
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = buttonColor
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (resetEmail.value.isBlank()) {
                            Toast.makeText(context, "Please enter your email address", Toast.LENGTH_SHORT).show()
                        } else {
                            isResetLoading.value = true
                            FirebaseAuth.getInstance().sendPasswordResetEmail(resetEmail.value)
                                .addOnCompleteListener { task ->
                                    isResetLoading.value = false
                                    if (task.isSuccessful) {
                                        Toast.makeText(context, "Password reset email sent to ${resetEmail.value}", Toast.LENGTH_LONG).show()
                                        Log.d(TAG, "Password reset email sent to: ${resetEmail.value}")
                                        showForgotPasswordDialog.value = false
                                        resetEmail.value = ""
                                    } else {
                                        val errorMessage = task.exception?.message ?: "Failed to send reset email"
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                                        Log.e(TAG, "Error sending reset email: $errorMessage")
                                    }
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                    enabled = !isResetLoading.value
                ) {
                    Text("Send Reset Link", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showForgotPasswordDialog.value = false
                        resetEmail.value = ""
                    },
                    enabled = !isResetLoading.value
                ) {
                    Text("Cancel", color = buttonColor)
                }
            },
            backgroundColor = backgroundColor,
            shape = RoundedCornerShape(16.dp)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor) // White background
            .padding(20.dp),
        contentAlignment = Alignment.Center
    ) {
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
            Text(
                text = "Welcome Back",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = textColor // Dark gray for text
            )

            Spacer(modifier = Modifier.height(20.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email", color = textColor) },
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Email),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    focusedBorderColor = buttonColor,
                    unfocusedBorderColor = borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Password", color = textColor) },
                visualTransformation = if (passwordVisible.value) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Password),
                trailingIcon = {
                    IconButton(onClick = { passwordVisible.value = !passwordVisible.value }) {
                        Icon(
                            imageVector = if (passwordVisible.value) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle Password",
                            tint = buttonColor
                        )
                    }
                },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    textColor = textColor,
                    backgroundColor = backgroundColor,
                    focusedBorderColor = buttonColor,
                    unfocusedBorderColor = borderColor
                ),
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading.value
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = { 
                    showForgotPasswordDialog.value = true
                    resetEmail.value = email.value // Pre-fill with email if already entered
                },
                enabled = !isLoading.value
            ) {
                Text("Forgot your password?", color = buttonColor, fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = {
                    if (email.value.isBlank() || password.value.isBlank()) {
                        Toast.makeText(context, "Please enter email and password", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    
                    // Show loading indicator
                    isLoading.value = true
                    
                    // Login with Firebase
                    Log.d(TAG, "Attempting login with email: ${email.value}")
                    authViewModel.signInWithEmailAndPassword(
                        email = email.value,
                        password = password.value,
                        onSuccess = {
                            Log.d(TAG, "Login successful")
                            isLoading.value = false
                            
                            // Check if we need to go to onboarding
                            if (authViewModel.hasCompletedOnboarding.value == true) {
                                Log.d(TAG, "User has already completed onboarding, proceeding to main app")
                                onLoginSuccess()
                            } else {
                                Log.d(TAG, "User needs to complete onboarding, navigating to userInfo1")
                                navController.navigate("userInfo1")
                            }
                        },
                        onError = { errorMsg ->
                            Log.e(TAG, "Login failed: $errorMsg")
                            isLoading.value = false
                            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show()
                        }
                    )
                },
                colors = ButtonDefaults.buttonColors(backgroundColor = buttonColor),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = !isLoading.value
            ) {
                Text("Log In", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))

//            Text("Or sign in with", color = textColor, fontSize = 14.sp)
//
//            Spacer(modifier = Modifier.height(10.dp))

//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceEvenly
//            ) {
//                SocialLoginButton(
//                    icon = R.drawable.ic_google,
//                    text = "Google",
//                    onClick = { /* Google Sign-in */ },
//                    enabled = !isLoading.value
//                )
//                SocialLoginButton(
//                    icon = R.drawable.ic_facebook,
//                    text = "Facebook",
//                    onClick = { /* Facebook Sign-in */ },
//                    enabled = !isLoading.value
//                )
//            }
//
//            Spacer(modifier = Modifier.height(20.dp))

            // Sign-up option
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(text = "Don't have an account?", color = textColor)
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "Sign Up",
                    color = buttonColor,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable(enabled = !isLoading.value) { 
                        if (!isLoading.value) {
                            navController.navigate("register") 
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun SocialLoginButton(
    icon: Int, 
    text: String, 
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(backgroundColor = Color.White),
        border = BorderStroke(2.dp, SoftBlushPink), // Updated to match theme
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
            .height(50.dp)
            .padding(horizontal = 8.dp),
        enabled = enabled
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = text,
                modifier = Modifier.size(24.dp),
                tint = Color.Unspecified
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = text, color = Color.Black, fontSize = 16.sp)
        }
    }
}

