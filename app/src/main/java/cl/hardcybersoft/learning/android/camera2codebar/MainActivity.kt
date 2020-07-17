package cl.hardcybersoft.learning.android.camera2codebar

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.hardware.camera2.params.StreamConfigurationMap
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.util.SparseArray
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import cl.hardcybersoft.learning.android.camera2codebar.exception.CamaraNoAdecuadaException
import com.google.android.gms.vision.Frame
import com.google.android.gms.vision.barcode.Barcode
import com.google.android.gms.vision.barcode.BarcodeDetector

class MainActivity : AppCompatActivity() {

    val CAMERA_PERMISSION_REQUEST:Int = 100
    lateinit var textureView: TextureView
    lateinit var textView: TextView
    lateinit var surfaceTexture: SurfaceTexture
    lateinit var cameraManager: CameraManager
    lateinit var cameraDevice: CameraDevice
    lateinit var cameraId: String
    lateinit var size:Size
    lateinit var captureRequestBuilder:CaptureRequest.Builder
    lateinit var button: Button
    lateinit var imageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        init()
    }

    private fun init():Unit {
        textureView     = findViewById<TextureView>(R.id.textureView)
        textView        = findViewById<TextView>(R.id.textView)
        cameraManager   = getSystemService(Context.CAMERA_SERVICE) as CameraManager
        cameraId        = configureCameraId()
        size            = configureSize()
        textureView.surfaceTextureListener = getSurfaceTextureListener()

        button = findViewById<Button>(R.id.button)
        imageView = findViewById<ImageView>(R.id.imageView)
        button.setOnClickListener(object: View.OnClickListener{
            override fun onClick(p0: View?) {
                var bitmap:Bitmap? = this@MainActivity.textureView.getBitmap(size.width, size.height)
                imageView.setImageBitmap(bitmap)
                var codigoBarras:String = leerCodigoBarras(bitmap!!)
                if(codigoBarras.length > 3) {
                    this@MainActivity.textView.setText(codigoBarras)
                    Log.i("CODEBAR", "Código de barras actualizado")
                }
            }
        })
    }

    @Throws(CamaraNoAdecuadaException::class)
    private fun configureCameraId(): String {
        for (cid:String in this.cameraManager.cameraIdList ) {
            val cameraCharacteristics:CameraCharacteristics = cameraManager.getCameraCharacteristics(cid)
            if( cameraCharacteristics.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK ) {
                return cid
            }
        }
        throw CamaraNoAdecuadaException("Cámara no adecuada. Se requiere utilizar un dispositivo con cámara trasera.")
    }

    private fun configureSize(): Size {
        val cameraCharacteristics:CameraCharacteristics = cameraManager.getCameraCharacteristics(this.cameraId)
        val streamConfigurationMap:StreamConfigurationMap = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP) as StreamConfigurationMap
        return streamConfigurationMap.getOutputSizes(ImageFormat.JPEG).first()
    }

    private fun getSurfaceTextureListener(): TextureView.SurfaceTextureListener {
        return object: TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surfaceTexture: SurfaceTexture, p1: Int, p2: Int) {
                this@MainActivity.surfaceTexture = surfaceTexture
                ejecutarConPermiso()
            }

            override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

            }

            override fun onSurfaceTextureUpdated(surfaceTexture: SurfaceTexture) {
                /*
                var bitmap:Bitmap? = this@MainActivity.textureView.getBitmap(size.width, size.height)
                var codigoBarras:String = leerCodigoBarras(bitmap!!)
                if(codigoBarras.length > 3) {
                    this@MainActivity.textView.setText(codigoBarras)
                    Log.i("CODEBAR", "Código de barras actualizado")
                }
                 */
            }

            override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                return true
            }
        }
    }

    private fun leerCodigoBarras(imagenCodigoBarras:Bitmap): String {
        var barcodeDetector:BarcodeDetector = BarcodeDetector.Builder(this).build()
        val frame:Frame = Frame.Builder().setBitmap( imagenCodigoBarras ).build()
        val barcodes:SparseArray<Barcode> = barcodeDetector.detect( frame )
        if( barcodes.size() > 0) {
            return barcodes.valueAt(0).rawValue
        } else {
            return ""
        }
    }

    private fun ejecutarConPermiso() {
        if( ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED ) {
            openCamera()
        } else {
            if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if(shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                    Toast.makeText(this, "La cámara es necesaria para escanear los códigos de barra", Toast.LENGTH_LONG).show()
                }

                requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if( requestCode == this.CAMERA_PERMISSION_REQUEST ) {
            if( grantResults.get(0) == PackageManager.PERMISSION_GRANTED ) {
                openCamera()
            } else {
                Toast.makeText(this, "El permiso para la cámara es necesario para que funcione la aplicación.", Toast.LENGTH_LONG).show()
                System.exit(0)
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        }
    }

    private fun openCamera() {
        try {
            this.cameraManager.openCamera(this.cameraId, getCameraDeviceStateCallback(), null)
        } catch (se:SecurityException) {

        }
    }

    private fun showCamera() {
        var surfaceTexture:SurfaceTexture? = this.textureView.surfaceTexture
        surfaceTexture?.setDefaultBufferSize(this.size.width, this.size.height)
        var surface:Surface = Surface(surfaceTexture)
        captureRequestBuilder = this.cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
        captureRequestBuilder.addTarget(surface)
        cameraDevice.createCaptureSession(listOf(surface), getCameraCaptureSessionStateCallback(), null)
    }

    private fun getCameraDeviceStateCallback(): CameraDevice.StateCallback {
        return object: CameraDevice.StateCallback() {
            override fun onOpened(cd: CameraDevice) {
                this@MainActivity.cameraDevice = cd
                showCamera()
            }

            override fun onDisconnected(p0: CameraDevice) {
                TODO("Not yet implemented")
            }

            override fun onError(p0: CameraDevice, p1: Int) {
                TODO("Not yet implemented")
            }

        }
    }

    private fun getCameraCaptureSessionStateCallback(): CameraCaptureSession.StateCallback {
        return object: CameraCaptureSession.StateCallback() {
            override fun onConfigured(cameraCaptureSession: CameraCaptureSession) {
                var captureRequest:CaptureRequest = this@MainActivity.captureRequestBuilder.build()
                cameraCaptureSession.setRepeatingRequest(captureRequest, null, null)
            }

            override fun onConfigureFailed(p0: CameraCaptureSession) {
                TODO("Not yet implemented")
            }
        }
    }


}