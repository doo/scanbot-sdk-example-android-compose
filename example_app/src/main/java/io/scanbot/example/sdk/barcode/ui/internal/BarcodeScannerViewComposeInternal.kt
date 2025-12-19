package io.scanbot.example.sdk.barcode.ui.internal

import android.Manifest
import android.content.pm.PackageManager
import androidx.annotation.FloatRange
import androidx.camera.camera2.interop.ExperimentalCamera2Interop
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.PermissionState
import com.google.accompanist.permissions.PermissionStatus
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import io.scanbot.sdk.barcode.BarcodeScanner
import io.scanbot.sdk.barcode.BarcodeScannerConfiguration
import io.scanbot.sdk.barcode.BarcodeScannerFrameHandler
import io.scanbot.sdk.barcode.BarcodeScannerResult
import io.scanbot.sdk.barcode_scanner.ScanbotBarcodeScannerSDK
import io.scanbot.sdk.camera.FrameHandler
import io.scanbot.sdk.camera.FrameHandlerResult
import io.scanbot.sdk.ui_v2.common.CameraConfiguration
import io.scanbot.sdk.ui_v2.common.camera.FinderConfiguration
import io.scanbot.sdk.ui_v2.common.camera.ScanbotComposeCamera
import io.scanbot.sdk.ui_v2.common.camera.ScanbotComposeCameraViewModel
import io.scanbot.sdk.ui_v2.common.camera.ZoomCameraValue
import io.scanbot.sdk.ui_v2.common.components.ArComposeView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.runBlocking


@Composable
@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class)
@ExperimentalCamera2Interop
fun BarcodeScannerViewClassic(
    modifier: Modifier = Modifier,
    cameraEnabled: Boolean = true, // not working yet, will be added in feature release
    torchEnabled: Boolean = false, // torch control, depends on device capabilities
    @FloatRange(
        from = 1.0, to = 5.0
    ) zoom: Float = 1.0f,   // not all the values might be supported depending on device capabilities
    barcodeScanningEnabled: Boolean = true,
    barcodeScannerConfiguration: BarcodeScannerConfiguration = BarcodeScannerConfiguration(),
    finderConfiguration: FinderConfiguration = FinderConfiguration(),
    arPolygonView: @Composable (
        barcodesFlow: SharedFlow<BarcodeScannerResult?>,
        framesFlow: SharedFlow<FrameHandler.Frame>,
    ) -> Unit = { _, _ -> },
    permissionView: @Composable () -> Unit = {},
    onBarcodesScanned: (BarcodeScannerResult) -> Unit = {},
) {
    val context = LocalContext.current
    val viewModel = remember {
        val scanner = ScanbotBarcodeScannerSDK(context).createBarcodeScanner()
        scanner.setConfiguration(barcodeScannerConfiguration)
        BarcodeScannerViewModelInternal(
            cameraConfiguration = CameraConfiguration(flashEnabled = torchEnabled),
            scanner = scanner,
            flashAvailable = context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH),
        )
    }
    LaunchedEffect(key1 = Unit) {
        viewModel.scanningResults.collect { scannerResult ->
            if (barcodeScanningEnabled) {
                scannerResult?.let { onBarcodesScanned(it) }
            }
        }
    }
    LaunchedEffect(zoom) {
        viewModel.zoomState.value = ZoomCameraValue(zoom, true)
    }
    LaunchedEffect(torchEnabled) {
        viewModel.flashEnabled.value = torchEnabled
    }
    LaunchedEffect(barcodeScanningEnabled) {
        viewModel.frameHandler.isEnabled = barcodeScanningEnabled
    }
    val previewMode = LocalInspectionMode.current

    Box(modifier = modifier) {
        val permissionGranted = if (!previewMode) {
            val cameraPermissionState =
                rememberPermissionState(permission = Manifest.permission.CAMERA)

            CheckPermissionStatus(cameraPermissionState) {
                viewModel.permissionEnabled.value = cameraPermissionState.status.isGranted
            }
            cameraPermissionState.status.isGranted
        } else {
            true
        }
        val backgroundColor = Color.Black
        if (permissionGranted) {
            if (cameraEnabled) {
                ScanbotComposeCamera(
                    modifier = Modifier.fillMaxSize(),
                    viewModel = viewModel,
                    cameraBackgroundColor = backgroundColor,
                    onViewCreated = { camera ->
                        if (!previewMode) {
                            camera.removeFrameHandler(viewModel.frameHandler)
                            camera.addFrameHandler(viewModel.frameHandler)
                        }
                    },
                    cameraArOverlayView = ArComposeView(context).apply {
                        setContent {
                            arPolygonView(
                                viewModel.scanningResults,
                                viewModel.frameFlow,
                            )
                        }
                    },
                    finderConfiguration = finderConfiguration,
                )
            }
        } else {
            permissionView()
        }
    }
}

@Composable
@OptIn(ExperimentalPermissionsApi::class)
fun CheckPermissionStatus(
    cameraPermissionState: PermissionState,
    permissionGrantedBlock: CoroutineScope.() -> Unit,
) {
    LaunchedEffect(
        key1 = cameraPermissionState.status.isGranted, block = permissionGrantedBlock
    )
    if (cameraPermissionState.status == PermissionStatus.Denied(false)) {
        if (!LocalInspectionMode.current) {
            SideEffect {
                cameraPermissionState.launchPermissionRequest()
            }
        }
    }
}

private class BarcodeScannerViewModelInternal(
    val cameraConfiguration: CameraConfiguration,
    val scanner: BarcodeScanner,
    val flashAvailable: Boolean = true,
) : ScanbotComposeCameraViewModel(
    cameraConfiguration.cameraModule,
    initialZoomSteps = cameraConfiguration.zoomSteps,
    defaultZoomFactor = cameraConfiguration.defaultZoomFactor,
    initialFlashEnabled = cameraConfiguration.flashEnabled,
    initialMinFocusDistanceLock = cameraConfiguration.minFocusDistanceLock,
    initialTouchToFocusEnabled = cameraConfiguration.touchToFocusEnabled,
    initialPinchToZoomEnabled = cameraConfiguration.pinchToZoomEnabled,
    initialPlayFlashOnSnap = false,
    initialOrientationLockMode = cameraConfiguration.orientationLockMode,
    initialCameraPreviewMode = cameraConfiguration.cameraPreviewMode,
    flashAvailable = flashAvailable,

    initialFpsLimit = 20
) {
    val frameHandler: BarcodeScannerFrameHandler =
        BarcodeScannerFrameHandler(scanner).apply {
            setScanningInterval(50)
        }
    private val _scanningResults: MutableSharedFlow<BarcodeScannerResult?> = MutableSharedFlow()
    val scanningResults: SharedFlow<BarcodeScannerResult?> = _scanningResults
    private val _frameFlow: MutableSharedFlow<FrameHandler.Frame> = MutableSharedFlow()
    val frameFlow: SharedFlow<FrameHandler.Frame> = _frameFlow
    val handler = BarcodeScannerFrameHandler.ResultHandler { result ->
        if (result is FrameHandlerResult.Success) {
            val res = result.value
            runBlocking {
                _frameFlow.emit(result.frame)
                this@BarcodeScannerViewModelInternal._scanningResults.emit(res)
            }
        }
        false
    }

    init {
        frameHandler.addResultHandler(handler)
    }
}

