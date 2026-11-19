package ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import data.auth.AuthRepository
import data.config.CloudSessionManager

@Composable
fun AccountTopBarButton(
    modifier: Modifier = Modifier,
    onGoogleSignInRequest: () -> Unit = {},
    onAppleSignInRequest: () -> Unit = {},
) {
    var showDialog by remember { mutableStateOf(false) }
    val user = AuthRepository.currentUser

    if (user != null) {
        val initial = user.displayName?.firstOrNull()?.uppercase()
            ?: user.email?.firstOrNull()?.uppercase()
            ?: "B"
        Box(
            modifier = modifier
                .size(32.dp)
                .background(Color.White.copy(alpha = 0.25f), CircleShape)
                .clickable { showDialog = true },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    } else {
        IconButton(
            onClick = { showDialog = true },
            modifier = modifier.size(36.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Account",
                tint = Color.White
            )
        }
    }

    if (showDialog) {
        if (user != null) {
            AccountDialog(
                onDismissRequest = { showDialog = false }
            )
        } else {
            SignInDialog(
                onDismissRequest = { showDialog = false },
                onGoogleSignInClick = {
                    showDialog = false
                    onGoogleSignInRequest()
                },
                onAppleSignInClick = {
                    showDialog = false
                    onAppleSignInRequest()
                }
            )
        }
    }
}
