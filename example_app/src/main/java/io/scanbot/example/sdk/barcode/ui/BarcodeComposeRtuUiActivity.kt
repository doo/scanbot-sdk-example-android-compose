package io.scanbot.example.sdk.barcode.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.ComposeView
import io.scanbot.sdk.barcode.textWithExtension
import io.scanbot.sdk.common.AspectRatio
import io.scanbot.sdk.ui_v2.barcode.BarcodeScannerView
import io.scanbot.sdk.ui_v2.barcode.configuration.BarcodeNativeConfiguration
import io.scanbot.sdk.ui_v2.barcode.configuration.BarcodeScannerScreenConfiguration
import io.scanbot.sdk.ui_v2.barcode.configuration.LocalBarcodeNativeConfiguration
import io.scanbot.sdk.ui_v2.barcode.configuration.MultipleScanningMode
import io.scanbot.sdk.ui_v2.common.ScanbotColor
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

        setContentView(ComposeView(this).apply {
            setContent {
                val configuration = remember {
                    BarcodeScannerScreenConfiguration().apply {
                        // Customize configuration of the screen here:
                        palette = palette.copy(
                            sbColorPrimary = ScanbotColor(Color.Black)
                        )
                        viewFinder.aspectRatio = AspectRatio(16.0, 9.0)
                        useCase = MultipleScanningMode()
                    }
                }

                CompositionLocalProvider(
                    LocalScanbotTopBarConfiguration provides ScanbotTopBarConfiguration(
                        addStatusBarPadding = true,
                        addNavigationBarPadding = true
                    ),
                    LocalBarcodeNativeConfiguration provides BarcodeNativeConfiguration(
                        // Enable if after the successful scan of
                        // barcodes the scanner should continue scanning
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
        })
    }
}
