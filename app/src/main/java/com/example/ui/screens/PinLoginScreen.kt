package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.automirrored.filled.Login
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.cloud.authStateFlow
import com.example.data.cloud.onGoogleSignInClicked
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosBorder
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosRedContainer
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningAmber
import com.example.ui.viewmodel.KioscoViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay

@Composable
fun PinLoginScreen(
    viewModel: KioscoViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val firebaseAuth = remember { Firebase.auth }
    val firebaseUser by remember { firebaseAuth.authStateFlow() }
        .collectAsStateWithLifecycle(initialValue = firebaseAuth.currentUser)

    val currentBranch by viewModel.selectedBranch.collectAsStateWithLifecycle()
    val failedAttempts by viewModel.failedPinAttempts.collectAsStateWithLifecycle()
    val lockoutUntil by viewModel.lockoutUntilMillis.collectAsStateWithLifecycle()
    val errorMessage by viewModel.errorMessage.collectAsStateWithLifecycle()
    val currentDeviceId by viewModel.currentDeviceId.collectAsStateWithLifecycle()
    val currentDeviceRecord by viewModel.currentDeviceRecord.collectAsStateWithLifecycle()
    val companySettings by viewModel.companySettings.collectAsStateWithLifecycle()
    val allUsers by viewModel.allUsers.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudAccessSyncStatus.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudAccessSyncing.collectAsStateWithLifecycle()

    var pinInput by remember { mutableStateOf("") }
    var remainingSeconds by remember { mutableStateOf(0L) }
    var googleAuthBusy by remember { mutableStateOf(false) }
    var showPairingDialog by remember { mutableStateOf(false) }
    var pairingCodeInput by remember { mutableStateOf("") }

    LaunchedEffect(lockoutUntil) {
        while (true) {
            val left = (lockoutUntil - System.currentTimeMillis()) / 1000L
            if (left > 0) {
                remainingSeconds = left
                delay(500L)
            } else {
                remainingSeconds = 0L
                break
            }
        }
    }

    val isLockedOut = remainingSeconds > 0
    val isDeviceAuthorized = !companySettings.strictDeviceWhitelist ||
        currentDeviceRecord?.status == "AUTHORIZED"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.ui.theme.DrakosLightCanvas)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp)
                .verticalScroll(rememberScrollState()),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = DrakosWhite),
            border = BorderStroke(1.dp, DrakosBorder),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Cabecera corporativa oscura DRAKO'S STORE con el logo oficial enviado
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(DrakosBlack, Color(0xFF14161B), Color(0xFF28050A))
                            )
                        )
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_drakos_bulldog),
                            contentDescription = "Logo Oficial Drako's Store",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.Black)
                                .border(1.5.dp, DrakosRed, RoundedCornerShape(14.dp))
                                .padding(4.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "DRAKOS",
                                    fontFamily = SpaceGroteskFontFamily,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 2.sp,
                                    color = DrakosWhite
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = DrakosRed,
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text(
                                        text = "24HS",
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = DrakosWhite,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Text(
                                text = "MANAGEMENT • 365 DÍAS",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                color = DrakosWhite.copy(alpha = 0.72f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = currentBranch?.name ?: "Terminal Punto de Venta",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DrakosWhite.copy(alpha = 0.92f)
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp)
                        .background(DrakosRed)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    // Estado de autorización del dispositivo (Seguridad Nivel 3)
                    Surface(
                        color = if (isDeviceAuthorized) SuccessGreen.copy(alpha = 0.12f) else WarningAmber.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, if (isDeviceAuthorized) SuccessGreen.copy(alpha = 0.5f) else WarningAmber),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Icon(
                                    imageVector = if (isDeviceAuthorized) Icons.Default.VerifiedUser else Icons.Default.Devices,
                                    contentDescription = null,
                                    tint = if (isDeviceAuthorized) SuccessGreen else WarningAmber,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = if (isDeviceAuthorized) "Terminal Autorizada" else "Terminal Pendiente de Permiso Nivel 3",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosBlack
                                    )
                                    Text(
                                        text = "ID: ${currentDeviceId.ifBlank { "DRK-LOCAL" }}",
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 10.sp,
                                        color = DrakosMutedInk
                                    )
                                }
                            }

                            TextButton(
                                onClick = { showPairingDialog = true },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                modifier = Modifier.testTag("open_device_pairing_button")
                            ) {
                                Icon(Icons.Default.Key, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isDeviceAuthorized) "Importar PINs" else "Vincular",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosRed
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Indicador enmascarado de PIN (4 dígitos)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(4) { index ->
                            val filled = index < pinInput.length
                            Box(
                                modifier = Modifier
                                    .size(18.dp)
                                    .clip(CircleShape)
                                    .background(if (filled) DrakosRed else DrakosSurfaceTint)
                                    .border(1.5.dp, if (filled) DrakosRed else DrakosBorder, CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isLockedOut) {
                        Surface(
                            color = DrakosRedContainer,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Terminal bloqueada. Reintente en ${remainingSeconds}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DangerRed
                                )
                            }
                        }
                    } else if (!errorMessage.isNullOrBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = errorMessage ?: "",
                                fontSize = 11.sp,
                                color = DangerRed,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Text(
                            text = if (failedAttempts > 0) {
                                "Intentos fallidos: $failedAttempts/5"
                            } else if (!isDeviceAuthorized) {
                                "Solo Dueño (N3) o con código de vinculación"
                            } else {
                                "Ingrese su PIN de operador (4 dígitos)"
                            },
                            fontSize = 12.sp,
                            color = DrakosMutedInk,
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9")
                    )

                    rows.forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            row.forEach { digit ->
                                OutlinedButton(
                                    onClick = {
                                        if (!isLockedOut && pinInput.length < 4) {
                                            val next = pinInput + digit
                                            pinInput = next
                                            if (next.length == 4) {
                                                viewModel.loginWithPin(next) { ok ->
                                                    if (!ok) pinInput = ""
                                                }
                                            }
                                        }
                                    },
                                    enabled = !isLockedOut,
                                    border = BorderStroke(1.dp, DrakosBorder),
                                    colors = ButtonDefaults.outlinedButtonColors(containerColor = DrakosSurfaceTint.copy(alpha = 0.45f)),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(52.dp)
                                        .testTag("pin_key_$digit")
                                ) {
                                    Text(
                                        text = digit,
                                        fontFamily = SpaceGroteskFontFamily,
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = DrakosBlack
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { pinInput = "" },
                            enabled = !isLockedOut && pinInput.isNotEmpty(),
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Text("Limpiar", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DrakosMutedInk)
                        }

                        OutlinedButton(
                            onClick = {
                                if (!isLockedOut && pinInput.length < 4) {
                                    val next = pinInput + "0"
                                    pinInput = next
                                    if (next.length == 4) {
                                        viewModel.loginWithPin(next) { ok ->
                                            if (!ok) pinInput = ""
                                        }
                                    }
                                }
                            },
                            enabled = !isLockedOut,
                            border = BorderStroke(1.dp, DrakosBorder),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = DrakosSurfaceTint.copy(alpha = 0.45f)),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .testTag("pin_key_0")
                        ) {
                            Text(
                                text = "0",
                                fontFamily = SpaceGroteskFontFamily,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = DrakosBlack
                            )
                        }

                        OutlinedButton(
                            onClick = { if (pinInput.isNotEmpty()) pinInput = pinInput.dropLast(1) },
                            enabled = !isLockedOut && pinInput.isNotEmpty(),
                            border = BorderStroke(1.dp, DrakosBorder),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Backspace, contentDescription = "Borrar dígito", tint = DrakosBlack)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            viewModel.loginWithPin(pinInput) { ok ->
                                if (!ok) pinInput = ""
                            }
                        },
                        enabled = !isLockedOut && pinInput.length == 4,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DrakosRed,
                            contentColor = DrakosWhite
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("pin_submit_button")
                    ) {
                        Text(
                            text = "DESBLOQUEAR TERMINAL",
                            fontFamily = SpaceGroteskFontFamily,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = DrakosBorder)
                    Spacer(modifier = Modifier.height(6.dp))

                    val activeGoogleUser = firebaseUser
                    if (activeGoogleUser != null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Default.CloudDone, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Nube Drakos: ${activeGoogleUser.email ?: activeGoogleUser.uid.take(8)}",
                                    fontSize = 11.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Text(
                                text = cloudSyncStatus ?: "${allUsers.size} perfiles activos en este equipo",
                                fontSize = 10.sp,
                                color = DrakosMutedInk,
                                textAlign = TextAlign.Center
                            )
                            TextButton(
                                onClick = { viewModel.syncAccessProfilesFromCloud(silent = false) },
                                enabled = !isCloudSyncing,
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("sync_pins_from_cloud_button")
                            ) {
                                Icon(Icons.Default.CloudSync, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (isCloudSyncing) "Sincronizando con nube..." else "Descargar PINs y Terminales de Nube",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = DrakosBlack
                                )
                            }
                        }
                    } else {
                        TextButton(
                            onClick = {
                                googleAuthBusy = true
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        googleAuthBusy = false
                                        viewModel.syncAccessProfilesFromCloud(silent = false)
                                    },
                                    onAuthError = { errMsg ->
                                        googleAuthBusy = false
                                        viewModel.notifyError(errMsg)
                                    },
                                    scope = scope,
                                    onAuthCancelled = { googleAuthBusy = false }
                                )
                            },
                            enabled = !googleAuthBusy,
                            modifier = Modifier.testTag("pin_screen_google_signin")
                        ) {
                            Icon(Icons.AutoMirrored.Filled.Login, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (googleAuthBusy) "Conectando Google..." else "Conectar Nube Empresarial (Google)",
                                fontSize = 11.sp,
                                color = DrakosBlack,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Drakos Store • Acceso protegido con PIN SHA-256 y lista blanca.",
                        fontSize = 10.sp,
                        color = DrakosMutedInk,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }

    if (showPairingDialog) {
        AlertDialog(
            onDismissRequest = { showPairingDialog = false },
            containerColor = DrakosWhite,
            title = {
                Text(
                    text = "Vincular Terminal e Importar PINs",
                    fontFamily = SpaceGroteskFontFamily,
                    fontWeight = FontWeight.Bold,
                    color = DrakosBlack
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Pegue la Llave Portátil (DRKPRO1:...) enviada desde 'Accesos N3' del teléfono del Dueño, o el código corto (DRK-XXXX) si comparte la misma cuenta Google en la nube.",
                        fontSize = 12.sp,
                        color = DrakosMutedInk
                    )
                    Text(
                        text = "ID de este dispositivo: ${currentDeviceId.ifBlank { "DRK-LOCAL" }}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = DrakosBlack
                    )
                    OutlinedTextField(
                        value = pairingCodeInput,
                        onValueChange = { pairingCodeInput = it },
                        label = { Text("Llave portátil (DRKPRO1:...) o código (DRK-XXXX)") },
                        minLines = 2,
                        maxLines = 5,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("device_pairing_code_input")
                    )
                    if (pairingCodeInput.isNotBlank()) {
                        val isPortable = com.example.domain.security.DeviceProvisioningCodec.isPortableBundle(pairingCodeInput)
                        Text(
                            text = if (isPortable) {
                                "Llave portátil DRKPRO1 detectada (importa PINs sin depender de Google)."
                            } else {
                                "Código corto detectado (requiere haber subido los PINs en el otro teléfono con la misma cuenta Google)."
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isPortable) SuccessGreen else WarningAmber
                        )
                    }
                    OutlinedButton(
                        onClick = {
                            val cm = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val pasted = cm.primaryClip?.let { clip ->
                                if (clip.itemCount > 0) clip.getItemAt(0).coerceToText(context)?.toString()?.trim().orEmpty() else ""
                            } ?: ""
                            if (pasted.isNotBlank()) {
                                pairingCodeInput = pasted
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.ContentPaste, contentDescription = null, tint = DrakosBlack, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Pegar desde Portapapeles", fontSize = 12.sp, color = DrakosBlack)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.redeemPairingCode(pairingCodeInput)
                        showPairingDialog = false
                        pairingCodeInput = ""
                    },
                    enabled = pairingCodeInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                    modifier = Modifier.testTag("confirm_device_pairing_button")
                ) {
                    Text("Vincular e Importar")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showPairingDialog = false }) {
                    Text("Cancelar", color = DrakosBlack)
                }
            }
        )
    }
}
