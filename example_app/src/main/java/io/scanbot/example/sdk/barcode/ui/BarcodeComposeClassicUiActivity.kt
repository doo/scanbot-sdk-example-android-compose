package io.scanbot.example.sdk.barcode.ui

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.scanbot.example.sdk.barcode.ui.internal.BarcodeScannerCustomUI
import io.scanbot.sdk.barcode.BarcodeItem
import io.scanbot.sdk.barcode.BarcodeScannerResult
import io.scanbot.sdk.barcode.textWithExtension
import io.scanbot.sdk.camera.FrameHandler
import io.scanbot.sdk.common.AspectRatio
import io.scanbot.sdk.ui_v2.barcode.components.ar_tracking.ScanbotBarcodesArOverlay
import io.scanbot.sdk.ui_v2.common.CameraPermissionScreen
import io.scanbot.sdk.ui_v2.common.camera.FinderConfiguration
import io.scanbot.sdk.ui_v2.common.components.ScanbotCameraPermissionView
import kotlinx.coroutines.flow.SharedFlow
import kotlin.random.Random

class BarcodeComposeClassicUiActivity : ComponentActivity() {

    @androidx.annotation.OptIn(ExperimentalCamera2Interop::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(modifier = Modifier.systemBarsPadding()) {
                // Use these states to control camera, torch and zoom
                val zoom = remember { mutableFloatStateOf(1.0f) }
                val torchEnabled = remember { mutableStateOf(false) }
                val cameraEnabled = remember { mutableStateOf(true) }

                // Unused in this example, but you may use it to
                // enable/disable barcode scanning dynamically
                val barcodeScanningEnabled = remember { mutableStateOf(true) }

                BarcodeScannerCustomUI(
                    // Modify Size here:
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1.0f),
                    finderConfiguration = FinderConfiguration(
                        // Modify aspect ratio of the viewfinder here:
                        aspectRatio = AspectRatio(1.0, 1.0),
                        // Change view finder overlay color here:
                        overlayColor = Color.Transparent,
                        // Change view finder stroke color here:
                        strokeColor = Color.Transparent,

                        // Alternatively, it is possible to provide a completely custom finder content:
                        finderContent = {
                            // Box with border stroke color as an example of custom finder content
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Transparent)
                                    // Same but with rounded corners
                                    .border(
                                        4.dp,
                                        Color.Cyan,
                                        shape = androidx.compose.foundation.shape.RoundedCornerShape(
                                            16.dp
                                        )
                                    )
                            ) {

                            }
                        },
                        topContent = {
                            Text(
                                "Custom Top Content",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        },
                        bottomContent = {
                            // You may add custom buttons and other elements here:
                            Text(
                                "Custom Bottom Content",
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                            )
                        }
                    ),
                    cameraEnabled = cameraEnabled.value,
                    barcodeScanningEnabled = barcodeScanningEnabled.value,
                    torchEnabled = torchEnabled.value,
                    zoomLevel = zoom.floatValue,
                    permissionView = {
                        // View that will be shown while camera permission is not granted
                        ScanbotCameraPermissionView(
                            modifier = Modifier.fillMaxSize(),
                            bottomContentPadding = 0.dp,
                            permissionConfig = CameraPermissionScreen(),
                            onClose = {
                                // Handle permission screen close if needed
                            })
                    },
                    arPolygonView = { barcodesFlow, framesFlow ->
                        // Configure AR overlay polygon appearance inside CustomBarcodesArView if needed
                        CustomBarcodesArView(
                            barcodesFlow = barcodesFlow, framesFlow = framesFlow,
                            onBarcodeClick = {
                                // Handle barcode click on barcode from AR overlay if needed
                            }, density = LocalDensity.current
                        )
                    },
                    onBarcodesScanned = { barcodeResult ->
                        // Apply feedback, sound, vibration here if needed
                        // ...

                        // Handle scanned barcodes here (for example, show a dialog)
                        Log.d(
                            "BarcodeComposeClassic", "Scanned barcodes: ${barcodeResult.barcodes}"
                        )
                    },
                )
                Row {
                    Button(modifier = Modifier.weight(1f), onClick = {
                        zoom.floatValue = 1.0f + Random.nextFloat()
                    }) {
                        Text("Zoom")
                    }

                    Button(modifier = Modifier.weight(1f), onClick = {
                        torchEnabled.value = !torchEnabled.value
                    }) {
                        Text("Flash")
                    }

                    Button(modifier = Modifier.weight(1f), onClick = {
                        cameraEnabled.value = !cameraEnabled.value
                    }) {
                        Text("Visibility")
                    }
                }

            }
        }
    }
}


@Composable
fun CustomBarcodesArView(
    // Flows providing barcode results and camera frames. Currently its not possible to use them without the SharedFlow we will investigate further
    barcodesFlow: SharedFlow<BarcodeScannerResult?>,
    framesFlow: SharedFlow<FrameHandler.Frame>,
    onBarcodeClick: (BarcodeItem) -> Unit = {},
    density: Density,
) {

    ScanbotBarcodesArOverlay(
        barcodesFlow,
        getData = { barcodeItem -> barcodeItem.textWithExtension },
        getPolygonStyle = { defaultStyle, barcodeItem ->
            // Customize polygon style here.
            // You may use barcodeItem to apply different styles for different barcode types, etc.
            defaultStyle.copy(
                drawPolygon = true,
                useFill = true,
                useFillHighlighted = true,
                cornerRadius = density.run { 20.dp.toPx() },
                cornerHighlightedRadius = density.run { 20.dp.toPx() },
                strokeWidth = density.run { 5.dp.toPx() },
                strokeHighlightedWidth = density.run { 5.dp.toPx() },
                strokeColor = Color.Green,
                strokeHighlightedColor = Color.Red,
                fillColor = Color.Green.copy(alpha = 0.3f),
                fillHighlightedColor = Color.Red.copy(alpha = 0.3f),
                shouldDrawShadows = false
            )
        },
        shouldHighlight = { barcodeItem ->
            // Here you can implement any custom logic.
            false
        },
        view = { path, barcodeItem, data, shouldHighlight ->
            // If only polygon is needed without any additional UI, leave this block empty
        },

// Uncomment and  Customize AR view for barcode polygon here if needed
//        view = { path, barcodeItem, data, shouldHighlight ->
//            // Implement custom view for barcode polygon if needed
//            Box(modifier = Modifier.layout { measurable, constraints ->
//                val placeable = measurable.measure(constraints);
//
//                var rectF: Rect
//                path.getBounds().also { rectF = it }
//
//                val width = placeable.width
//                val height = placeable.height
//                val x = rectF.center.x - width / 2
//                val y = rectF.center.y + rectF.height / 2 + 10.dp.toPx() // place below the polygon
//                layout(width, height) {
//                    placeable.placeRelative(x.toInt(), y.toInt())
//                }
//            }) {
//                Text(
//                    text = data,
//                    color = if (shouldHighlight) Color.Red else Color.Green,
//                    style = MaterialTheme.typography.body2,
//                    modifier = Modifier
//                        .background(Color.Black.copy(alpha = 0.5f))
//                        .padding(4.dp)
//                )
//            }
//        },
        frameFlow = framesFlow,
        onClick = onBarcodeClick,
    )
}