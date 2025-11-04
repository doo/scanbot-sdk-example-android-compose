package io.scanbot.example.sdk.barcode.ui

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import io.scanbot.example.sdk.barcode.R
import io.scanbot.sdk.barcode.textWithExtension
import io.scanbot.sdk.ui_v2.barcode.BarcodeScannerView
import io.scanbot.sdk.ui_v2.barcode.configuration.BarcodeNativeConfiguration
import io.scanbot.sdk.ui_v2.barcode.configuration.BarcodeScannerScreenConfiguration
import io.scanbot.sdk.ui_v2.barcode.configuration.LocalBarcodeNativeConfiguration
import io.scanbot.sdk.ui_v2.common.StatusBarMode
import io.scanbot.sdk.ui_v2.common.activity.AutoCancelTimeout
import io.scanbot.sdk.ui_v2.common.activity.CanceledByUser
import io.scanbot.sdk.ui_v2.common.activity.ForceClose
import io.scanbot.sdk.ui_v2.common.activity.LicenseInvalid
import io.scanbot.sdk.ui_v2.common.activity.SystemError
import io.scanbot.sdk.ui_v2.common.components.LocalScanbotTopBarConfiguration
import io.scanbot.sdk.ui_v2.common.components.ScanbotTopBarConfiguration

class BarcodeComposeRtuUiActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.compose_barcode_scanner)

        // If you use "traditional" Android XML-driven UI - integrate ComposeView into your layout
        // and use code below to render our BarcodeScannerView in it.
        val cameraContainerView: ComposeView = findViewById(R.id.compose_container)
        cameraContainerView.apply {
            setContent {

                //In case if you already migrated to Compose UI - just use
                // the code below in your Composable function.
                val configuration = remember {
                    BarcodeScannerScreenConfiguration().apply {
                        // TODO: configure as needed
                    }
                }

                // This `LaunchedEffect` will allow view to react on
                // BarcodeScannerConfiguration's `statusBarMode` correctly.
                val statusBarHidden = configuration.topBar.statusBarMode == StatusBarMode.HIDDEN
                LaunchedEffect(key1 = true, block = {
                    if (statusBarHidden) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                            window.attributes.layoutInDisplayCutoutMode =
                                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }

                        WindowCompat.setDecorFitsSystemWindows(window, false)
                    }
                })

                CompositionLocalProvider(
                    LocalScanbotTopBarConfiguration provides ScanbotTopBarConfiguration(
                        addStatusBarPadding = true,
                        addNavigationBarPadding = true
                    ),
                    LocalBarcodeNativeConfiguration provides BarcodeNativeConfiguration(
                        enableContinuousScanning = false
                    )
                ) {
                    BarcodeScannerView(
                        configuration = configuration,
                        onBarcodeScanned = {
                            // Handle scanned barcodes here
                            Toast.makeText(
                                this@BarcodeComposeRtuUiActivity,
                                "Scanned barcodes:\n" + it.items.joinToString("\n") { barcodeItem ->
                                    val barcode = barcodeItem.barcode
                                    "${barcode.format.name}: ${barcode.textWithExtension}"
                                },
                                Toast.LENGTH_LONG
                            ).show()
                            finish()
                        },
                        onBarcodeScannerClosed = {
                            when (it) {
                                LicenseInvalid -> Toast.makeText(
                                    context,
                                    "License has expired!",
                                    Toast.LENGTH_LONG
                                ).show()

                                AutoCancelTimeout -> Unit // just close screen (below)
                                CanceledByUser -> Unit // just close screen (below)
                                is SystemError -> Unit // handle system error here
                                ForceClose -> Unit // just close screen (below)
                            }
                            finish()
                        }
                    )
                }
            }
        }
    }
}
