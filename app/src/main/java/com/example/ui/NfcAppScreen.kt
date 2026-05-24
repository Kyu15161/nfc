package com.example.ui

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SimCard
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CreditCard
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Nfc
import androidx.compose.material.icons.outlined.Pin
import androidx.compose.material.icons.outlined.Power
import androidx.compose.material.icons.outlined.QrCode
import androidx.compose.material.icons.outlined.TransitEnterexit
import androidx.compose.material.icons.outlined.VpnKey
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.ui.window.Dialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.NfcCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Theme aware colors
val DarkBackground @Composable get() = MaterialTheme.colorScheme.background
val CardSurface @Composable get() = MaterialTheme.colorScheme.surface
val OnBackground @Composable get() = MaterialTheme.colorScheme.onBackground
val GrayText @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val CyberCyan @Composable get() = MaterialTheme.colorScheme.primary
val CyberTeal @Composable get() = MaterialTheme.colorScheme.secondary
val CyberViolet @Composable get() = MaterialTheme.colorScheme.primaryContainer
val CyberAmber @Composable get() = MaterialTheme.colorScheme.tertiary
val CyberRed @Composable get() = MaterialTheme.colorScheme.error
val CodeBg @Composable get() = MaterialTheme.colorScheme.surfaceVariant


@Composable
fun NfcAppScreen(viewModel: NfcViewModel) {
    val context = LocalContext.current
    val allCards by viewModel.allCards.collectAsState()
    val activeCard by viewModel.activeCardScanned.collectAsState()
    val isScanning by viewModel.isScanningActive.collectAsState()
    val toastMessage by viewModel.showToastMessage.collectAsState()

    var selectedTab by remember { mutableStateOf(0) } // 0 = Scanner/Diagnostics, 1 = History Log

    // Manage standard notifications safely
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            viewModel.dismissToast()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Scaffold(
            bottomBar = {
                NavigationBar(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                ) {
                    NavigationBarItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Nfc, contentDescription = "Active Scanner") },
                        label = { Text("Scanner", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("tab_radar_scan")
                    )
                    NavigationBarItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Digital Wallet") },
                        label = { Text("Wallet", fontWeight = FontWeight.SemiBold) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                        ),
                        modifier = Modifier.testTag("tab_history")
                    )
                }
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                // Cyber Style Header Banner
                NfcHeaderBanner(viewModel)

                if (selectedTab == 0) {
                    ScannerAndDetailsLayout(
                        activeCard = activeCard,
                        isScanning = isScanning,
                        viewModel = viewModel,
                        onClearActiveCard = { viewModel.clearActiveCard() },
                        onSaveToWallet = { card, label -> viewModel.saveToWallet(card, label) },
                        onUpdateCardInfo = { card, label, details -> viewModel.updateCardInfo(card, label, details) },
                        onTapToPay = { card -> viewModel.tapToPay(card) }
                    )
                } else {
                    HistoryLogsLayout(
                        historyList = allCards,
                        onSelectCard = { card ->
                            viewModel.selectCardFromHistory(card)
                            selectedTab = 0  // switch tab to view tag immediately
                        },
                        onDelete = { id -> viewModel.deleteCard(id) },
                        onClearAll = { viewModel.clearAllScans() },
                        onAddManualCard = { label, type, details -> viewModel.addManualCard(label, type, details) }
                    )
                }
            }
        }
    }
}

@Composable
fun NfcHeaderBanner(viewModel: NfcViewModel) {
    val isDark by viewModel.isDarkMode.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkBackground)
            .padding(vertical = 16.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Nfc,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = "NFC Wallet & Lab",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = OnBackground
                )
                Text(
                    text = "Manage your cards intuitively",
                    fontSize = 11.sp,
                    color = GrayText,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            androidx.compose.material3.IconButton(
                onClick = { viewModel.toggleTheme() },
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape)
            ) {
                Icon(
                    imageVector = if (isDark) Icons.Outlined.LightMode else Icons.Outlined.DarkMode,
                    contentDescription = "Toggle Theme",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun ScannerAndDetailsLayout(
    activeCard: NfcCard?,
    isScanning: Boolean,
    viewModel: NfcViewModel,
    onClearActiveCard: () -> Unit,
    onSaveToWallet: (NfcCard, String) -> Unit,
    onUpdateCardInfo: (NfcCard, String, String) -> Unit,
    onTapToPay: (NfcCard) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Upper scanner status or active details
        if (activeCard == null) {
            // Displays spinning visual scan radar to capture card tap
            NfcRadarScannerArea(isScanning)
            
            Spacer(modifier = Modifier.height(30.dp))
            
            CardSimulatorSwiper(
                onSimulate = { cardIndex -> 
                    viewModel.onSimulateScan(cardIndex) 
                }
            )
        } else {
            // Displays beautiful detailed diagnostic breakdown
            ActiveCardDetailArea(card = activeCard, onDismiss = onClearActiveCard, onSaveToWallet = onSaveToWallet, onUpdateCardInfo = onUpdateCardInfo, onTapToPay = onTapToPay)
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun NfcRadarScannerArea(isScanning: Boolean) {
    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "NFC DISPATCH MONITOR",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                letterSpacing = 1.5.sp
            )

            Spacer(modifier = Modifier.height(30.dp))

            // Pulse Radar Graphic View
            AnimatedRadarPulse(isScanning)

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "Awaiting Physical Tag Contact",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Bring an NFC card near the back of your device to scan and inspect its contents.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                modifier = Modifier.padding(horizontal = 12.dp)
            )
        }
    }
}

@Composable
fun AnimatedRadarPulse(isScanning: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "RadarTransition")
    
    // Scale animation
    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse1"
    )

    // Secondary offset pulse scale
    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 2.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, delayMillis = 1100, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Pulse2"
    )

    // Rotating arc sweep animation
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "SweepArc"
    )

    val colorByState = if (isScanning) CyberCyan else GrayText

    Box(
        modifier = Modifier.size(160.dp),
        contentAlignment = Alignment.Center
    ) {
        // Draw Radar Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val centerOffset = this.center
            val baseRadius = size.minDimension / 4f

            // Ripple 1
            if (isScanning) {
                drawCircle(
                    color = colorByState,
                    radius = baseRadius * pulseScale1,
                    center = centerOffset,
                    alpha = (1.0f - (pulseScale1 / 2.0f)).coerceIn(0f, 1f),
                    style = Stroke(width = 2.dp.toPx())
                )
                // Ripple 2
                drawCircle(
                    color = colorByState,
                    radius = baseRadius * pulseScale2,
                    center = centerOffset,
                    alpha = (1.0f - (pulseScale2 / 2.0f)).coerceIn(0f, 1f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }

            // Central concentric static grid lines
            drawCircle(
                color = colorByState.copy(alpha = 0.2f),
                radius = baseRadius * 1.5f,
                style = Stroke(width = 1.dp.toPx())
            )
            drawCircle(
                color = colorByState.copy(alpha = 0.2f),
                radius = baseRadius * 0.8f,
                style = Stroke(width = 1.6.dp.toPx())
            )

            // Dynamic rotating sweeping radar wedge
            if (isScanning) {
                drawArc(
                    brush = Brush.sweepGradient(
                        listOf(Color.Transparent, colorByState.copy(alpha = 0.15f), colorByState.copy(alpha = 0.35f))
                    ),
                    startAngle = sweepAngle,
                    sweepAngle = 45f,
                    useCenter = true
                )
            }
        }

        // Animated Central Core Chip
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(
                    Brush.verticalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)),
                    CircleShape
                )
                .border(2.dp, Color.White, CircleShape)
                .shadow(
                    elevation = 6.dp,
                    shape = CircleShape,
                    spotColor = colorByState
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Nfc,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
fun CardSimulatorSwiper(onSimulate: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "NFC CHIP EMULATOR LAB",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF49454F),
                letterSpacing = 1.5.sp
            )
            Box(
                modifier = Modifier
                    .background(Color(0xFFEADDFF), RoundedCornerShape(12.dp))
                    .padding(horizontal = 10.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "5 PROFILES",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF21005D)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Horizontal Carousel of Cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Card 1: Visa Contactless
            SimulatedPocketCard(
                title = "VISA EMV Contactless",
                subTitle = "ISO/IEC 14443-4 Class",
                accentColor = Color(0xFF0F3EE0),
                techTag = "EMV SMART",
                icon = Icons.Outlined.CreditCard,
                onClick = { onSimulate(1) },
                testTag = "sim_btn_visa"
            )

            // Card 2: Transit Pass
            SimulatedPocketCard(
                title = "Suica Transit Pass",
                subTitle = "JIS X 6319-4 Sony FeliCa",
                accentColor = Color(0xFF0FBA53),
                techTag = "NFC-F PASS",
                icon = Icons.Outlined.TransitEnterexit,
                onClick = { onSimulate(2) },
                testTag = "sim_btn_suica"
            )

            // Card 3: Amiibo Token
            SimulatedPocketCard(
                title = "Nintendo Amiibo",
                subTitle = "NTAG215 Gaming Crypto",
                accentColor = Color(0xFFE52521),
                techTag = "NTAG215",
                icon = Icons.Default.SmartButton,
                onClick = { onSimulate(3) },
                testTag = "sim_btn_amiibo"
            )

            // Card 4: Mifare Classic Business
            SimulatedPocketCard(
                title = "MIFARE Classic 1K",
                subTitle = "ISO 14443-3 Type A Pass",
                accentColor = Color(0xFF4A148C),
                techTag = "1KB SECTOR",
                icon = Icons.Outlined.VpnKey,
                onClick = { onSimulate(4) },
                testTag = "sim_btn_mifare"
            )

            // Card 5: Smart Sticker Poster
            SimulatedPocketCard(
                title = "Smart Portal RFID",
                subTitle = "NTAG213 Smart URI Record",
                accentColor = Color(0xFF7C4DFF),
                techTag = "NDEF WIFI",
                icon = Icons.Outlined.QrCode,
                onClick = { onSimulate(5) },
                testTag = "sim_btn_ntag213"
            )
        }
    }
}

@Composable
fun SimulatedPocketCard(
    title: String,
    subTitle: String,
    accentColor: Color,
    techTag: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    testTag: String
) {
    Box(
        modifier = Modifier
            .width(220.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        accentColor.copy(alpha = 0.85f),
                        accentColor.copy(alpha = 0.5f),
                        Color(0xFF13171C)
                    )
                )
            )
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .shadow(4.dp)
            .testTag(testTag)
    ) {
        // Hologram visual accent
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = 70.dp.toPx(),
                center = androidx.compose.ui.geometry.Offset(size.width, 0f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = techTag,
                        color = Color(0xFF00E5FF),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }

            Column {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subTitle,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "TAP TO SWIPE SCAN",
                    color = Color(0xFF00E5FF),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

@Composable
fun ActiveCardDetailArea(
    card: NfcCard, 
    onDismiss: () -> Unit, 
    onSaveToWallet: (NfcCard, String) -> Unit,
    onUpdateCardInfo: (NfcCard, String, String) -> Unit,
    onTapToPay: (NfcCard) -> Unit
) {
    var showWalletDialog by remember { mutableStateOf(false) }
    var walletLabel by remember { mutableStateOf("") }
    
    var showEditDialog by remember { mutableStateOf(false) }
    var editLabel by remember(card) { mutableStateOf(card.walletLabel ?: "") }
    var editDetails by remember(card) { mutableStateOf(card.payloadText) }

    if (showWalletDialog) {
        Dialog(onDismissRequest = { showWalletDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "SAVE TO WALLET",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(text = "Enter a label for this card:", color = GrayText, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = walletLabel,
                        onValueChange = { walletLabel = it },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        placeholder = { Text("e.g., Office Keycard") }
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showWalletDialog = false }) {
                            Text("CANCEL", color = GrayText, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onSaveToWallet(card, walletLabel)
                                showWalletDialog = false
                                walletLabel = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    if (showEditDialog) {
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "EDIT WALLET INFORMATION",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = editLabel,
                        onValueChange = { editLabel = it },
                        label = { Text("Wallet Label") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = editDetails,
                        onValueChange = { editDetails = it },
                        label = { Text("Card Details / Payload") },
                        modifier = Modifier.fillMaxWidth().height(140.dp),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showEditDialog = false }) {
                            Text("CANCEL", color = GrayText, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                onUpdateCardInfo(card, editLabel, editDetails)
                                showEditDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("UPDATE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(CyberTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = CyberTeal,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "DECODED HARDWARE ACQUISITION",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CyberTeal,
                    letterSpacing = 1.5.sp
                )
            }
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_active_btn")
            ) {
                Text("DISMISS", color = GrayText, fontWeight = FontWeight.Bold, fontSize = 11.sp)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Main M3 Summary Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, Color(0xFFCAC4D0), RoundedCornerShape(24.dp)),
            colors = CardDefaults.cardColors(containerColor = CardSurface),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp)
            ) {
                // Main Header Title Info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(MaterialTheme.colorScheme.secondaryContainer, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SimCard,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = card.cardType.uppercase(Locale.getDefault()),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnBackground
                        )
                        Text(
                            text = "UID: ${card.uid}",
                            fontSize = 12.sp,
                            color = GrayText,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.tertiaryContainer, RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = if (card.isWritable) "SECURE RW" else "SECURE",
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(modifier = Modifier.height(14.dp))

                // Detail Items Grid in light grey container backgrounds
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    DetailRowItem(label = "Hardware standard", value = card.standard, icon = Icons.Outlined.Info)
                    DetailRowItem(label = "Manufacturer", value = card.manufacturer, icon = Icons.Outlined.Pin)
                    DetailRowItem(label = "Technologies registered", value = card.technologies, icon = Icons.Outlined.Power)
                    DetailRowItem(label = "Storage capacity footprint", value = "${card.capacity} Bytes (${card.blockCount} blocks)", icon = Icons.Outlined.Memory)
                    DetailRowItem(label = "Write status permission", value = if (card.isWritable) "WRITABLE (Open Access)" else "READ-ONLY (Secure Write Protected)", icon = Icons.Outlined.VpnKey, isAccent = card.isWritable)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (!card.isSavedToWallet) {
                Button(
                    onClick = { showWalletDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.AccountBalanceWallet, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("SAVE", fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { onTapToPay(card) },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Nfc, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("TAP TO PAY", fontWeight = FontWeight.Bold)
                }
            }

            Button(
                onClick = { showEditDialog = true },
                colors = ButtonDefaults.buttonColors(containerColor = CyberTeal),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("EDIT DETAILS", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(modifier = Modifier.height(20.dp))

        // Decoded Payload section depending on type
        PayloadBreakdownSection(card)

        Spacer(modifier = Modifier.height(20.dp))

        // Raw Memory Sector Matrix Visualizer
        RawMemoryMatrixVisualizer(card)
    }
}

@Composable
fun DetailRowItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isAccent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(MaterialTheme.colorScheme.surface, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isAccent) CyberAmber else CyberCyan,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = label.uppercase(Locale.getDefault()),
                fontSize = 9.sp,
                color = GrayText,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.5.sp
            )
            Text(
                text = value,
                fontSize = 13.sp,
                color = if (isAccent) CyberAmber else OnBackground,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
fun PayloadBreakdownSection(card: NfcCard) {
    val headerColor = when (card.payloadType) {
        "PAYMENT" -> Color(0xFF0F3EE0)
        "TRANSIT" -> Color(0xFF0FBA53)
        "GAMING" -> CyberRed
        "URI" -> CyberViolet
        else -> CyberCyan
    }

    Text(
        text = "INTERPRETED PAYLOAD CONTENTS",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFF49454F),
        letterSpacing = 1.5.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color = Color(0xFFCAC4D0))
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val pIcon = when (card.payloadType) {
                    "PAYMENT" -> Icons.Outlined.CreditCard
                    "TRANSIT" -> Icons.Outlined.TransitEnterexit
                    "GAMING" -> Icons.Default.SmartButton
                    "URI" -> Icons.Outlined.Wifi
                    else -> Icons.Outlined.Info
                }
                Icon(
                    imageVector = pIcon,
                    contentDescription = null,
                    tint = headerColor,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "TYPE: ${card.payloadType}",
                    color = headerColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = card.payloadText,
                fontSize = 14.sp,
                color = OnBackground,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp
            )

            // Dynamic Action button inside parsed payload if URL
            if (card.payloadType == "URI") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = { /* Simulated redirect secure action */ },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberViolet),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Outlined.Wifi, contentDescription = null, modifier = Modifier.size(16.dp))
                        Text(
                            text = "LAUNCH PORTAL DIRECT",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RawMemoryMatrixVisualizer(card: NfcCard) {
    Text(
        text = "RAW MEMORY REGISTER MATRIX (HEX)",
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        color = GrayText,
        letterSpacing = 1.5.sp
    )
    Spacer(modifier = Modifier.height(10.dp))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(CodeBg)
            .border(1.dp, Color(0xFF263238), RoundedCornerShape(8.dp))
            .padding(14.dp)
    ) {
        Text(
            text = card.rawMemory,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = Color(0xFFA5D6A7),
            lineHeight = 16.sp
        )
    }
}

@Composable
fun HistoryLogsLayout(
    historyList: List<NfcCard>,
    onSelectCard: (NfcCard) -> Unit,
    onDelete: (Long) -> Unit,
    onClearAll: () -> Unit,
    onAddManualCard: (String, String, String) -> Unit
) {
    var showManualAddDialog by remember { mutableStateOf(false) }
    var addLabel by remember { mutableStateOf("") }
    var addType by remember { mutableStateOf("") }
    var cardNumber by remember { mutableStateOf("") }
    var cardHolder by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var expirationDate by remember { mutableStateOf("") }

    if (showManualAddDialog) {
        Dialog(onDismissRequest = { showManualAddDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp)
                ) {
                    Text(
                        text = "ADD MANUAL ENTRY",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = CyberTeal,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 450.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = addLabel,
                            onValueChange = { addLabel = it },
                            label = { Text("Wallet Label") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = addType,
                            onValueChange = { addType = it },
                            label = { Text("Card Type (e.g., Payment Card)") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cardHolder,
                            onValueChange = { cardHolder = it },
                            label = { Text("Card Holder Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = cardNumber,
                            onValueChange = { cardNumber = it },
                            label = { Text("Card Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedTextField(
                                value = expirationDate,
                                onValueChange = { expirationDate = it },
                                label = { Text("Exp Date") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = cvv,
                                onValueChange = { cvv = it },
                                label = { Text("CVV") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showManualAddDialog = false }) {
                            Text("CANCEL", color = GrayText, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (addLabel.isNotBlank()) {
                                    val details = """
                                        Card Holder: $cardHolder
                                        Card Number: $cardNumber
                                        Exp Date: $expirationDate
                                        CVV: $cvv
                                    """.trimIndent()
                                    onAddManualCard(addLabel, if (addType.isBlank()) "Payment Card" else addType, details)
                                }
                                showManualAddDialog = false
                                addLabel = ""
                                addType = ""
                                cardNumber = ""
                                cardHolder = ""
                                cvv = ""
                                expirationDate = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("SAVE SECURE", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    val walletCards = historyList.filter { it.isSavedToWallet }
    val regularHistory = historyList.filter { !it.isSavedToWallet }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "DIGITAL WALLET & LOGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF49454F),
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "${walletCards.size} Saved • ${regularHistory.size} Logged",
                    fontSize = 11.sp,
                    color = GrayText
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = { showManualAddDialog = true },
                    colors = ButtonDefaults.textButtonColors(contentColor = CyberTeal)
                ) {
                    Text("+ ADD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }

                if (historyList.isNotEmpty()) {
                    TextButton(
                        onClick = onClearAll,
                        colors = ButtonDefaults.textButtonColors(contentColor = CyberRed),
                        modifier = Modifier.testTag("clear_history_btn")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                            Text("CLEAR", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (historyList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Outlined.Nfc,
                        contentDescription = null,
                        tint = GrayText,
                        modifier = Modifier.size(60.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No Scanned Registers Found",
                        color = OnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Simulated scans or hardware reads will appear here.",
                        color = GrayText,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (walletCards.isNotEmpty()) {
                    Text(
                        text = "SECURE DIGITAL WALLET", 
                        fontWeight = FontWeight.Black, 
                        fontSize = 12.sp, 
                        color = CyberTeal,
                        letterSpacing = 1.sp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        walletCards.forEach { card ->
                            HistoryCardRow(
                                card = card,
                                onSelect = { onSelectCard(card) },
                                onDelete = { onDelete(card.id) },
                                isWalletView = true
                            )
                        }
                    }
                    HorizontalDivider(color = Color(0xFFCAC4D0).copy(alpha = 0.5f))
                }

                if (regularHistory.isNotEmpty()) {
                    Text(
                        text = "RECENT SCANS", 
                        fontWeight = FontWeight.Bold, 
                        fontSize = 12.sp, 
                        color = GrayText
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        regularHistory.forEach { card ->
                            HistoryCardRow(
                                card = card,
                                onSelect = { onSelectCard(card) },
                                onDelete = { onDelete(card.id) },
                                isWalletView = false
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryCardRow(
    card: NfcCard,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    isWalletView: Boolean = false
) {
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val sdf = remember(configuration) { SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()) }
    val timeLabel = remember(card.timestamp, configuration) { sdf.format(Date(card.timestamp)) }

    val accentBorder = when (card.payloadType) {
        "PAYMENT" -> Color(0xFF0F3EE0)
        "TRANSIT" -> Color(0xFF0FBA53)
        "GAMING" -> CyberRed
        "URI" -> CyberViolet
        else -> CyberCyan
    }

    OutlinedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("history_item_${card.id}"),
        colors = CardDefaults.outlinedCardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentBorder.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(accentBorder, CircleShape)
                    )
                    Text(
                        text = if (isWalletView && !card.walletLabel.isNullOrBlank()) card.walletLabel.toString() else card.cardType,
                        color = OnBackground,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                
                if (isWalletView) {
                    Text(
                        text = card.cardType,
                        color = accentBorder,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Text(
                    text = "UID: ${card.uid}",
                    color = GrayText,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Text(
                    text = "Scanned: $timeLabel",
                    color = GrayText,
                    fontSize = 11.sp
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("delete_btn_${card.id}")
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove record",
                    tint = CyberRed.copy(alpha = 0.8f)
                )
            }
        }
    }
}
