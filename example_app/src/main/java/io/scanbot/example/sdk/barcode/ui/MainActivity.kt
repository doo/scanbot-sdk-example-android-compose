package io.scanbot.example.sdk.barcode.ui

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import io.scanbot.example.sdk.barcode.R
import io.scanbot.example.sdk.barcode.databinding.ActivityMainBinding
import io.scanbot.example.sdk.barcode.ui.util.applyEdgeToEdge
import io.scanbot.sap.Status
import io.scanbot.sdk.barcode.BarcodeScanner
import io.scanbot.sdk.barcode_scanner.ScanbotBarcodeScannerSDK

class MainActivity : AppCompatActivity() {

    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        applyEdgeToEdge(this.findViewById(R.id.root_view))

        barcodeScanner = ScanbotBarcodeScannerSDK(this).createBarcodeScanner()

        binding.warningView.isVisible =
            ScanbotBarcodeScannerSDK(this).licenseInfo.status == Status.StatusTrial

        binding.artuDemo.setOnClickListener {
            val intent = Intent(applicationContext, BarcodeComposeRtuUiActivity::class.java)
            startActivity(intent)
        }

        binding.composeDemo.setOnClickListener {
            val intent = Intent(this@MainActivity, BarcodeComposeClassicUiActivity::class.java)
            startActivity(intent)
        }
    }
}
