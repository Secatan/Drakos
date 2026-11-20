package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FlashlightOff
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.theme.DrakosBlack
import com.example.ui.theme.DrakosMutedInk
import com.example.ui.theme.DrakosRed
import com.example.ui.theme.DrakosSurfaceTint
import com.example.ui.theme.DrakosWhite
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.SpaceGroteskFontFamily
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

@OptIn(ExperimentalGetImage::class)
@Composable
fun BarcodeScannerDialog(
    title: String = "Escáner Óptico Drakos",
    subtitle: String = "Apunte la cámara trasera al código EAN-13 o QR del producto.",
    confirmLabel: String = "Confirmar Código",
    onBarcodeDetected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    var cameraError by remember { mutableStateOf<String?>(null) }
    var torchEnabled by remember { mutableStateOf(false) }
    var boundCamera by remember { mutableStateOf<Camera?>(null) }
    var manualBarcode by remember { mutableStateOf("") }
    val detectedLock = remember { AtomicBoolean(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val cameraExecutor = remember { Executors.newSingleThreadExecutor() }
    val barcodeScanner = remember {
        val options = BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_EAN_8,
                Barcode.FORMAT_UPC_A,
                Barcode.FORMAT_UPC_E,
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_QR_CODE
            )
            .build()
        BarcodeScanning.getClient(options)
    }

    DisposableEffect(Unit) {
        onDispose {
            val mainExecutor = ContextCompat.getMainExecutor(context)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            cameraProviderFuture.addListener({
                try {
                    cameraProviderFuture.get().unbindAll()
                } catch (_: Exception) {
                }
                barcodeScanner.close()
                cameraExecutor.shutdown()
            }, mainExecutor)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DrakosWhite,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DrakosBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.QrCodeScanner,
                            contentDescription = null,
                            tint = DrakosRed,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = title,
                        fontFamily = SpaceGroteskFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = DrakosBlack
                    )
                }
                if (hasCameraPermission && boundCamera?.cameraInfo?.hasFlashUnit() == true) {
                    IconButton(
                        onClick = {
                            torchEnabled = !torchEnabled
                            boundCamera?.cameraControl?.enableTorch(torchEnabled)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = if (torchEnabled) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                            contentDescription = "Linterna",
                            tint = if (torchEnabled) DrakosRed else DrakosMutedInk
                        )
                    }
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (hasCameraPermission && cameraError == null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DrakosBlack),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { ctx ->
                                val previewView = PreviewView(ctx)
                                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                                cameraProviderFuture.addListener({
                                    try {
                                        val cameraProvider = cameraProviderFuture.get()
                                        val preview = Preview.Builder().build().also {
                                            it.surfaceProvider = previewView.surfaceProvider
                                        }
                                        val analysis = ImageAnalysis.Builder()
                                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                            .build()
                                            .also { imageAnalysis ->
                                                imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                                                    val mediaImage = imageProxy.image
                                                    if (mediaImage != null && !detectedLock.get()) {
                                                        val image = InputImage.fromMediaImage(
                                                            mediaImage,
                                                            imageProxy.imageInfo.rotationDegrees
                                                        )
                                                        barcodeScanner.process(image)
                                                            .addOnSuccessListener { barcodes ->
                                                                val code = barcodes.firstOrNull { !it.rawValue.isNullOrBlank() }?.rawValue
                                                                if (code != null && detectedLock.compareAndSet(false, true)) {
                                                                    ContextCompat.getMainExecutor(ctx).execute {
                                                                        onBarcodeDetected(code)
                                                                    }
                                                                }
                                                            }
                                                            .addOnCompleteListener {
                                                                imageProxy.close()
                                                            }
                                                    } else {
                                                        imageProxy.close()
                                                    }
                                                }
                                            }
                                        cameraProvider.unbindAll()
                                        boundCamera = cameraProvider.bindToLifecycle(
                                            lifecycleOwner,
                                            CameraSelector.DEFAULT_BACK_CAMERA,
                                            preview,
                                            analysis
                                        )
                                    } catch (e: Exception) {
                                        cameraError = e.localizedMessage ?: "Cámara no disponible en este dispositivo."
                                    }
                                }, ContextCompat.getMainExecutor(ctx))
                                previewView
                            },
                            modifier = Modifier.fillMaxSize()
                        )

                        // Retícula de enfoque Carmesí Drakos
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.76f)
                                .height(92.dp)
                                .border(2.5.dp, DrakosRed, RoundedCornerShape(10.dp))
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = DrakosMutedInk
                    )
                } else {
                    Surface(
                        color = DrakosSurfaceTint,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.CameraAlt, contentDescription = null, tint = DrakosRed, modifier = Modifier.size(34.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = cameraError ?: "Se requiere permiso de cámara para escanear códigos de barras.",
                                fontSize = 12.sp,
                                color = DrakosBlack,
                                fontWeight = FontWeight.Medium
                            )
                            if (!hasCameraPermission) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed)
                                ) {
                                    Text("Conceder Permiso de Cámara", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = manualBarcode,
                    onValueChange = { manualBarcode = it },
                    label = { Text("Ingreso manual / Lector USB HID") },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(fontFamily = JetBrainsMonoFontFamily),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (manualBarcode.isNotBlank()) {
                                onBarcodeDetected(manualBarcode.trim())
                            }
                        }
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("scanner_manual_input")
                )
            }
        },
        confirmButton = {
            if (manualBarcode.isNotBlank()) {
                Button(
                    onClick = { onBarcodeDetected(manualBarcode.trim()) },
                    colors = ButtonDefaults.buttonColors(containerColor = DrakosRed),
                    modifier = Modifier.testTag("scanner_confirm_manual_button")
                ) {
                    Text(confirmLabel, color = DrakosWhite, fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cerrar", color = DrakosBlack)
            }
        }
    )
}
