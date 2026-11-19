package ui.auth

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.OutlinedButton
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import data.auth.AuthRepository
import data.auth.AuthState
import data.config.CloudSessionManager
import kotlinx.coroutines.launch

@Composable
fun SignInDialog(
    onDismissRequest: () -> Unit,
    onGoogleSignInClick: () -> Unit = {},
    onAppleSignInClick: () -> Unit = {},
) {
    val coroutineScope = rememberCoroutineScope()
    var emailInput by remember { mutableStateOf("") }
    var oobCodeInput by remember { mutableStateOf("") }
    var isEmailSent by remember { mutableStateOf(false) }
    var isSendingEmail by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val authState = AuthRepository.authState
    val isLoading = authState is AuthState.Loading || isSendingEmail

    Dialog(onDismissRequest = { if (!isLoading) onDismissRequest() }) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colors.surface,
            elevation = 16.dp,
            modifier = Modifier.fillMaxWidth().padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                // Top close button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                // Header icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(MaterialTheme.colors.primary.copy(alpha = 0.12f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Sign in to BibleBible",
                    style = MaterialTheme.typography.h6.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colors.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Sync reading history, saved positions, and preferences across your devices. Sign-in is always optional.",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(24.dp))

                if (errorMessage != null) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colors.error,
                        style = MaterialTheme.typography.caption,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colors.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Apple Sign-In
                Button(
                    onClick = {
                        errorMessage = null
                        onAppleSignInClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Color.Black,
                        contentColor = Color.White
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Continue with Apple",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Google Sign-In
                OutlinedButton(
                    onClick = {
                        errorMessage = null
                        onGoogleSignInClick()
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.25f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        backgroundColor = MaterialTheme.colors.surface,
                        contentColor = MaterialTheme.colors.onSurface
                    ),
                    enabled = !isLoading
                ) {
                    Text(
                        text = "Continue with Google",
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f)))
                    Text(
                        text = "or",
                        style = MaterialTheme.typography.caption,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f),
                        modifier = Modifier.padding(horizontal = 12.dp)
                    )
                    Spacer(modifier = Modifier.weight(1f).height(1.dp).background(MaterialTheme.colors.onSurface.copy(alpha = 0.15f)))
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Email Magic Link Input
                if (!isEmailSent) {
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = { emailInput = it },
                        label = { Text("Email address") },
                        placeholder = { Text("name@example.com") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = MaterialTheme.colors.primary,
                            unfocusedBorderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                        ),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (emailInput.isNotBlank() && "@" in emailInput) {
                                coroutineScope.launch {
                                    isSendingEmail = true
                                    errorMessage = null
                                    try {
                                        AuthRepository.sendEmailSignInLink(emailInput.trim())
                                        isEmailSent = true
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Failed to send link"
                                    } finally {
                                        isSendingEmail = false
                                    }
                                }
                            } else {
                                errorMessage = "Please enter a valid email address."
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = MaterialTheme.colors.primary,
                            contentColor = MaterialTheme.colors.onPrimary
                        ),
                        enabled = !isLoading && emailInput.isNotBlank()
                    ) {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Send Magic Link", fontSize = 14.sp)
                    }
                } else {
                    // Confirmation when link is dispatched
                    Text(
                        text = "Sign-in link sent to $emailInput. Check your inbox and tap the link to complete sign-in.",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Code input for desktop / manual pasting
                    OutlinedTextField(
                        value = oobCodeInput,
                        onValueChange = { oobCodeInput = it },
                        label = { Text("Or paste sign-in code") },
                        placeholder = { Text("Enter code from link") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isLoading
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            if (oobCodeInput.isNotBlank()) {
                                coroutineScope.launch {
                                    try {
                                        AuthRepository.completeEmailSignInLink(emailInput.trim(), oobCodeInput.trim())
                                        onDismissRequest()
                                    } catch (e: Exception) {
                                        errorMessage = e.message ?: "Verification failed"
                                    }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(40.dp),
                        enabled = !isLoading && oobCodeInput.isNotBlank()
                    ) {
                        Text("Verify Code")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Continue as Guest",
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        }
    }
}
