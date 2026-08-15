package com.vault.vanishx.presentation.scan

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.zxing.BarcodeFormat
import com.google.zxing.BinaryBitmap
import com.google.zxing.DecodeHintType
import com.google.zxing.MultiFormatReader
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.client.android.Intents
import com.google.zxing.common.HybridBinarizer
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.vault.vanishx.R

/**
 * VanishX-styled QR scanner (story 7.6): wraps ZXing's [CaptureManager] — which already owns
 * camera permission / orientation lock / decode — with a bottom toolbar for close, torch toggle,
 * and gallery import (decodes a QR straight out of a picked image). Kept as a plain Activity
 * because the live preview is backed by ZXing's own SurfaceView, not Compose.
 *
 * Launched by [com.journeyapps.barcodescanner.ScanOptions.setCaptureActivity] from the Join
 * screen; returns the scanned text the same way the library's default `CaptureActivity` would,
 * so [com.journeyapps.barcodescanner.ScanContract] keeps working unchanged.
 */
@Suppress("TooManyFunctions")
class VanishXScanActivity : ComponentActivity() {

    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var capture: CaptureManager
    private var torchOn = false

    private val galleryLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let(::decodeFromGallery)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_vanishx_scan)
        applyScanToolbarInsets()

        barcodeView = findViewById(R.id.zxing_barcode_scanner)
        capture = CaptureManager(this, barcodeView)
        capture.initializeFromIntent(intent, savedInstanceState)
        capture.decode()

        intent?.getStringExtra(Intents.Scan.PROMPT_MESSAGE)?.takeIf { it.isNotBlank() }?.let { prompt ->
            findViewById<TextView>(R.id.scan_hint).text = prompt
        }

        findViewById<View>(R.id.btn_scan_close).setOnClickListener { finish() }
        findViewById<View>(R.id.btn_scan_gallery).setOnClickListener { galleryLauncher.launch("image/*") }

        val torchButton = findViewById<ImageButton>(R.id.btn_scan_torch)
        if (packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            torchButton.setOnClickListener { toggleTorch(torchButton) }
        } else {
            torchButton.visibility = View.GONE
        }
    }

    override fun onResume() {
        super.onResume()
        capture.onResume()
    }

    override fun onPause() {
        super.onPause()
        capture.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        capture.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        capture.onSaveInstanceState(outState)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray,
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        capture.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean =
        barcodeView.onKeyDown(keyCode, event) || super.onKeyDown(keyCode, event)

    private fun toggleTorch(button: ImageButton) {
        torchOn = !torchOn
        if (torchOn) barcodeView.setTorchOn() else barcodeView.setTorchOff()
        button.setBackgroundResource(
            if (torchOn) R.drawable.bg_scan_toolbar_button_active else R.drawable.bg_scan_toolbar_button,
        )
    }

    private fun decodeFromGallery(uri: Uri) {
        val bitmap = runCatching { decodeSampledBitmap(uri) }.getOrNull()
        if (bitmap == null) {
            showGalleryError()
            return
        }
        val pixels = IntArray(bitmap.width * bitmap.height)
        bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
        val source = RGBLuminanceSource(bitmap.width, bitmap.height, pixels)
        val reader = MultiFormatReader().apply {
            setHints(mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.QR_CODE)))
        }
        val result = runCatching { reader.decode(BinaryBitmap(HybridBinarizer(source))) }.getOrNull()
        if (result == null) {
            showGalleryError()
            return
        }
        val resultIntent = Intent(Intents.Scan.ACTION).apply {
            putExtra(Intents.Scan.RESULT, result.text)
            putExtra(Intents.Scan.RESULT_FORMAT, result.barcodeFormat.name)
        }
        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun decodeSampledBitmap(uri: Uri): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (bounds.outWidth / sample > MAX_GALLERY_DIMENSION || bounds.outHeight / sample > MAX_GALLERY_DIMENSION) {
            sample *= 2
        }
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        return contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
    }

    private fun applyScanToolbarInsets() {
        val toolbar = findViewById<View>(R.id.scan_toolbar)
        val initialBottom = toolbar.paddingBottom
        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, view.paddingTop, view.paddingRight, initialBottom + bars.bottom)
            insets
        }
    }

    private fun showGalleryError() {
        Toast.makeText(this, getString(R.string.join_scan_gallery_error), Toast.LENGTH_SHORT).show()
    }

    private companion object {
        const val MAX_GALLERY_DIMENSION = 1600
    }
}
