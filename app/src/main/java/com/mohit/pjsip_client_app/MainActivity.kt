package com.mohit.pjsip_client_app

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.mizuvoip.jvoip.SIPNotification
import com.mizuvoip.jvoip.SIPNotificationListener
import com.mohit.pjsip_client_app.ui.theme.PjSip_client_appTheme
import kotlinx.coroutines.delay

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SipManager.init(
            context = this,
            server = "192.168.1.42",
            username = "8181",
            password = "mohit8181"
        )
        enableEdgeToEdge()
        setContent {
            PjSip_client_appTheme {
                Scaffold { innerPadding ->
                    SIPVoIPUI(Modifier.padding(innerPadding))
                }
            }
        }

        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.MODIFY_AUDIO_SETTINGS,
                android.Manifest.permission.ACCESS_WIFI_STATE,
                android.Manifest.permission.ACCESS_NETWORK_STATE,
            ),
            123
        )
    }
}

@Composable
fun SIPVoIPUI(modifier: Modifier = Modifier) {
    var destination by remember { mutableStateOf("") }
    val context = LocalContext.current
    var callDuration by remember { mutableStateOf(0) }

    // Listen for incoming calls
    IncomingCallListener()

    // Check if we're in an active call
    val isInCall = remember(SipManager.callStatus) {
        SipManager.callStatus.contains("Call", ignoreCase = true) &&
                !SipManager.callStatus.contains("Incoming", ignoreCase = true) &&
                !SipManager.callStatus.contains("Idle", ignoreCase = true) &&
                !SipManager.callStatus.contains("Ended", ignoreCase = true) &&
                !SipManager.callStatus.contains("Rejected", ignoreCase = true)
    }

    val isOnHold = remember(SipManager.callStatus) {
        SipManager.callStatus.contains("Hold", ignoreCase = true)
    }

    // Call duration timer
    LaunchedEffect(isInCall) {
        if (!isInCall) {
            callDuration = 0
        }
    }

    LaunchedEffect(isInCall, isOnHold) {
        if (isInCall && !isOnHold) {
            while (isInCall && !isOnHold) {
                delay(1000)
                callDuration++
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // Status Bar
            StatusBar(
                registrationStatus = SipManager.registrationStatus,
                callStatus = SipManager.callStatus
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Main Content
            if (SipManager.isIncomingCall) {
                IncomingCallScreen(
                    callerNumber = SipManager.incomingCallNumber,
                    onAnswer = { SipManager.answerIncomingCall() },
                    onReject = { SipManager.rejectIncomingCall() }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Display Area
                    DisplayArea(
                        destination = destination,
                        onDestinationChange = { destination = it },
                        isInCall = isInCall
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Call Duration (if in call)
                    if (isInCall) {
                        CallDurationDisplay(
                            duration = callDuration,
                            isOnHold = isOnHold
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Dial Pad (only when not in call)
                    if (!isInCall) {
                        DialPad(
                            onDigitClick = { digit -> destination += digit },
                            onDeleteClick = {
                                if (destination.isNotEmpty()) {
                                    destination = destination.dropLast(1)
                                }
                            }
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // Call Controls
                    if (isInCall) {
                        // Force recomposition when states change
                        val currentMuteState by remember { derivedStateOf { SipManager.isMuted } }
                        val currentSpeakerState by remember { derivedStateOf { SipManager.isSpeakerOn } }

                        InCallControls(
                            isMuted = currentMuteState,
                            isSpeakerOn = currentSpeakerState,
                            isOnHold = isOnHold,
                            onMuteToggle = {
                                SipManager.toggleMute()
                                Toast.makeText(context,
                                    if (SipManager.isMuted) "Muted" else "Unmuted",
                                    Toast.LENGTH_SHORT).show()
                            },
                            onSpeakerToggle = {
                                SipManager.toggleSpeaker(context)
                                Toast.makeText(context,
                                    if (SipManager.isSpeakerOn) "Speaker On" else "Speaker Off",
                                    Toast.LENGTH_SHORT).show()
                            },
                            onHoldToggle = {
                                if (isOnHold) {
                                    SipManager.releaseCall()
                                } else {
                                    SipManager.holdCall()
                                }
                            },
                            onHangup = { SipManager.hangUp() }
                        )
                    } else {
                        // Call Button
                        CallButton(
                            onClick = {
                                if (destination.isNotBlank()) {
                                    SipManager.makeCall(destination)
                                } else {
                                    Toast.makeText(context, "Enter destination", Toast.LENGTH_SHORT).show()
                                }
                            },
                            enabled = destination.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBar(registrationStatus: String, callStatus: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(
                            if (registrationStatus == "Registered")
                                Color(0xFF4CAF50)
                            else
                                Color(0xFFF44336)
                        )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = registrationStatus,
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
            Text(
                text = callStatus,
                color = Color.White.copy(alpha = 0.6f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun DisplayArea(
    destination: String,
    onDestinationChange: (String) -> Unit,
    isInCall: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (isInCall) {
                Text(
                    text = destination,
                    color = Color.White,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                )
            } else {
                OutlinedTextField(
                    value = destination,
                    onValueChange = onDestinationChange,
                    placeholder = {
                        Text(
                            "Enter number",
                            color = Color.White.copy(alpha = 0.4f),
                            fontSize = 24.sp
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    textStyle = LocalTextStyle.current.copy(
                        fontSize = 28.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        }
    }
}

@Composable
fun CallDurationDisplay(duration: Int, isOnHold: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Call Duration",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = formatDuration(duration),
            color = Color.White,
            fontSize = 40.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
        )
        if (isOnHold) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "On Hold",
                color = Color(0xFFFFC107),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DialPad(onDigitClick: (String) -> Unit, onDeleteClick: () -> Unit) {
    val digits = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("*", "0", "#")
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        digits.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { digit ->
                    Button(
                        onClick = { onDigitClick(digit) },
                        modifier = Modifier
                            .weight(1f)
                            .height(70.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Text(
                            text = digit,
                            color = Color.White,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InCallControls(
    isMuted: Boolean,
    isSpeakerOn: Boolean,
    isOnHold: Boolean,
    onMuteToggle: () -> Unit,
    onSpeakerToggle: () -> Unit,
    onHoldToggle: () -> Unit,
    onHangup: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mute Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isMuted)
                        Color.White.copy(alpha = 0.2f)
                    else
                        Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                onClick = onMuteToggle
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🎤",
                        fontSize = 32.sp,
                        color = if (isMuted) Color.Red else Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isMuted) {
                        Text(
                            text = "❌",
                            fontSize = 16.sp,
                            color = Color.Red
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isMuted) "Unmute" else "Mute",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            // Hold Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isOnHold)
                        Color.White.copy(alpha = 0.2f)
                    else
                        Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                onClick = onHoldToggle
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isOnHold) "▶️" else "⏸️",
                        fontSize = 32.sp,
                        color = if (isOnHold) Color(0xFFFFC107) else Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isOnHold) "Resume" else "Hold",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }

            // Speaker Button
            Card(
                modifier = Modifier
                    .weight(1f)
                    .height(100.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSpeakerOn)
                        Color.White.copy(alpha = 0.2f)
                    else
                        Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                onClick = onSpeakerToggle
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🔊",
                        fontSize = 32.sp,
                        color = if (isSpeakerOn) Color(0xFF2196F3) else Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (isSpeakerOn) {
                        Text(
                            text = "❌",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (!isSpeakerOn) "Speaker On" else "Speaker Off",
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp
                    )
                }
            }
        }

        // Hangup Button
        Button(
            onClick = onHangup,
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFF44336)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(
                text = "📞",
                fontSize = 24.sp
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("End Call", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ControlButton(
    icon: ImageVector,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.height(100.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isActive)
                Color.White.copy(alpha = 0.2f)
            else
                Color.White.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = if (isActive) Color(0xFF2196F3) else Color.White,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = label,
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
fun CallButton(onClick: () -> Unit, enabled: Boolean) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF4CAF50),
            disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Text(
            text = "📞",
            fontSize = 28.sp
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            "Call",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun IncomingCallScreen(
    callerNumber: String,
    onAnswer: () -> Unit,
    onReject: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF2196F3),
                            Color(0xFF1976D2)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "📞",
                fontSize = 60.sp
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Incoming Call",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = callerNumber,
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 22.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // Reject Button
            FloatingActionButton(
                onClick = onReject,
                containerColor = Color(0xFFF44336),
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = "Reject",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }

            // Answer Button
            FloatingActionButton(
                onClick = onAnswer,
                containerColor = Color(0xFF4CAF50),
                modifier = Modifier.size(80.dp)
            ) {
                Icon(
                    Icons.Filled.Call,
                    contentDescription = "Answer",
                    tint = Color.White,
                    modifier = Modifier.size(36.dp)
                )
            }
        }
    }
}

@Composable
fun IncomingCallListener() {
    LaunchedEffect(Unit) {
        while (true) {
            val incoming = SipManager.getIncomingCaller()
            if (incoming.isNotEmpty() && !SipManager.callStatus.contains("In Call")) {
                SipManager.isIncomingCall = true
                SipManager.incomingCallNumber = incoming
                SipManager.callStatus = "Incoming call from $incoming"
            }
            delay(500)
        }
    }
}

// Helper function to format call duration
fun formatDuration(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%02d:%02d".format(mins, secs)
}