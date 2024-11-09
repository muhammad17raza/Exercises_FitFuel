package com.ooplab.exercises_fitfuel

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.media.Image
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.poselandmarker.PoseLandmarker
import java.io.ByteArrayOutputStream
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.atan2


// MainActivity.kt

// The MainActivity class is the entry point of the application.
// It extends AppCompatActivity, which provides compatibility support for older Android versions.
class MainActivity : AppCompatActivity() {

    // Declares a variable for the camera executor service.
    // 'private' means it can only be accessed within this class.
    // 'lateinit var' tells the compiler that the variable will be initialized before use.
    private lateinit var cameraExecutor: ExecutorService

    // Declares a variable for the PreviewView, which displays the camera preview on the screen.
    private lateinit var previewView: PreviewView

    // Declares a variable for the PoseLandmarker, which detects human poses in images.
    private lateinit var poseLandmarker: PoseLandmarker

    // TextViews to display count and stage
    private lateinit var countTextView: TextView
    private lateinit var stageTextView: TextView

    // TextViews to display angles
    private lateinit var angle1TextView: TextView
    private lateinit var angle2TextView: TextView
    private lateinit var angle3TextView: TextView
    private lateinit var angle4TextView: TextView

    var count = 0
    var stage = "Down"
    var isAt90Degrees = false

//    private var count = 0
//    private var stage: String? = null

    // OverlayView for drawing landmarks
    private lateinit var overlayView: OverlayView

    // The onCreate function is called when the activity is starting.
    // 'override' indicates that this function overrides a function in the superclass.
    // 'fun' declares a function in Kotlin.
    // 'savedInstanceState: Bundle?' is a nullable parameter that can be used to restore previous state.
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState) // Calls the superclass implementation.

        setContentView(R.layout.activity_main) // Sets the layout for the activity using XML file 'activity_main'.

        setupEdgeToEdge() // Calls a function to adjust the layout for devices with edge-to-edge displays.

        initCameraExecutor() // Initializes the camera executor and pose landmarker.

        previewView = findViewById(R.id.previewCam) // Finds the PreviewView from the layout to display the camera feed.

        // Initialize TextViews for displaying count and stage
        countTextView = findViewById(R.id.countTextView)
        stageTextView = findViewById(R.id.stageTextView)

        // Initialize TextViews for displaying angles
        angle1TextView = findViewById(R.id.angle1TextView)
        angle2TextView = findViewById(R.id.angle2TextView)
        angle3TextView = findViewById(R.id.angle3TextView)
        angle4TextView = findViewById(R.id.angle4TextView)

        // Initialize OverlayView
        overlayView = findViewById(R.id.overlayView)

        requestCameraPermission() // Initiates the process to request camera permission from the user.
    }

    // This function adjusts the layout to fit edge-to-edge displays.
    // It ensures that the app content doesn't overlap with system UI elements like the status bar.
    private fun setupEdgeToEdge() {
        // 'findViewById' finds a view with the specified ID in the layout.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { view, insets ->
            // Gets the system bar insets (areas occupied by the status bar, navigation bar, etc.).
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            // Adjusts the padding of the view to account for the system bars.
            view.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)

            insets // Returns the insets unmodified.
        }
    }

    // Initializes the camera executor and pose landmarker.
    private fun initCameraExecutor() {
        // Creates a single-threaded executor for running camera-related tasks asynchronously.
        cameraExecutor = Executors.newSingleThreadExecutor()

        initializePoseLandmarker() // Calls a function to initialize the pose landmarker.
    }

    // Sets up the pose landmarker with the necessary options.
    private fun initializePoseLandmarker() {
        // BaseOptions is used to configure the model for the pose landmarker.
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("pose_landmarker_lite.task") // Specifies the path to the model asset file.
            .build()

        // Configures the PoseLandmarker with options.
        val options = PoseLandmarker.PoseLandmarkerOptions.builder()
            .setBaseOptions(baseOptions) // Sets the base options with the model path.
            .setRunningMode(RunningMode.LIVE_STREAM) // Sets the running mode to live stream for real-time processing.
            .setResultListener { result, inputImage ->
                // This is a lambda function that gets called when pose detection results are available.
                // 'result' contains the detected pose landmarks.
                // 'inputImage' is the image that was processed.

                // TODO: Implement exercise counting logic here using the landmarks.

                // Ensure that the landmarks list is not empty before accessing the first element.
                val allLandmarks = result.landmarks()

                if (allLandmarks.isNotEmpty() && allLandmarks[0].isNotEmpty()) {
                    // Access the first set of landmarks (for the first detected person)
                    val landmarks = allLandmarks[0]

                    // Update UI elements on the main thread
                    runOnUiThread {
                        // Update the overlay view with the landmarks
                        overlayView.setLandmarks(landmarks)

                        // Define points for left and right hips, knees, and shoulders
                        val leftHip = landmarks[23]
                        val leftKnee = landmarks[25]
                        val rightHip = landmarks[24]
                        val rightKnee = landmarks[26]
                        val leftShoulder = landmarks[11]
                        val rightShoulder = landmarks[12]

// Check if all necessary landmarks are detected
                        if (leftHip != null && leftKnee != null &&
                            rightHip != null && rightKnee != null &&
                            leftShoulder != null && rightShoulder != null
                        ) {
                            // Calculate hip angles
                            val angleLeftHip = calculateAngle(
                                leftShoulder.x(), leftShoulder.y(),
                                leftHip.x(), leftHip.y(),
                                leftKnee.x(), leftKnee.y()
                            )

                            val angleRightHip = calculateAngle(
                                rightShoulder.x(), rightShoulder.y(),
                                rightHip.x(), rightHip.y(),
                                rightKnee.x(), rightKnee.y()
                            )

                            // Update UI elements (need to run on main thread)
                            runOnUiThread {
                                // Define angle conditions
                                val bothLegsDownCondition = (angleLeftHip >= 175 && angleLeftHip <= 185) &&
                                        (angleRightHip >= 175 && angleRightHip <= 185)
                                val bothLegsUpCondition = (angleLeftHip >= 85 && angleLeftHip <= 95) &&
                                        (angleRightHip >= 85 && angleRightHip <= 95)

                                // Check the stage and conditions for rep counting
                                if (bothLegsUpCondition && stage == "Down") {
                                    // When both legs reach approximately 90 degrees, set stage to "Up"
                                    stage = "Up"
                                    isAt90Degrees = true
                                } else if (isAt90Degrees && bothLegsDownCondition) {
                                    // When both legs go back to approximately 180 degrees from 90 degrees, increment rep count
                                    count++
                                    stage = "Down"       // Reset stage to "Down" after counting
                                    isAt90Degrees = false // Reset the 90-degree tracker
                                }

                                // Update TextViews
                                countTextView.text = "Reps: $count"
                                stageTextView.text = "Stage: $stage"
                                angle1TextView.text = "Angle Left Hip: $angleLeftHip"
                                angle2TextView.text = "Angle Right Hip: $angleRightHip"
                            }
                        }

                    } }else {
                    Log.d("PoseLandmarks", "No landmarks detected.")
                    // Clear the overlay if no landmarks are detected
                    runOnUiThread {
                        overlayView.setLandmarks(mutableListOf())

                    }
                }
            }
            .build()

        // Creates the PoseLandmarker instance with the specified options.
        // 'this' refers to the current context (MainActivity).
        poseLandmarker = PoseLandmarker.createFromOptions(this, options)
    }

    // Calculates the angle between three points
    private fun calculateAngle(ax: Float, ay: Float, bx: Float, by: Float, cx: Float, cy: Float): Double {
        val radians = atan2(cy - by, cx - bx) - atan2(ay - by, ax - bx)
        var angle = Math.toDegrees(abs(radians).toDouble())
        if (angle > 180.0) {
            angle = 360.0 - angle
        }
        return angle
    }

    // Declares a launcher for handling the result of the camera permission request.
    // 'ActivityResultLauncher<String>' is a generic type that launches an activity for a result.
    private val cameraPermissionLauncher: ActivityResultLauncher<String> =
        // Registers for activity result with a contract for requesting permissions.
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            // This lambda function is called with 'granted' indicating whether permission was granted.
            if (granted) {
                setupCamera() // If permission is granted, proceed to set up the camera.
            } else {
                // If permission is denied, show a toast message to the user.
                Toast.makeText(this, "Camera permission required", Toast.LENGTH_SHORT).show()
            }
        }

    // Requests camera permission if it hasn't been granted yet.
    private fun requestCameraPermission() {
        if (hasCameraPermission()) {
            setupCamera() // If permission is already granted, set up the camera.
        } else {
            // Launches the permission request dialog.
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Checks if the app already has camera permission.
    private fun hasCameraPermission(): Boolean {
        // Uses 'ContextCompat.checkSelfPermission' to check permission status.
        // Returns true if permission is granted, false otherwise.
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    // Sets up the camera preview and image analysis pipeline.
    private fun setupCamera() {
        // Gets a future that resolves to a ProcessCameraProvider, which provides access to the camera.
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        // Adds a listener that runs when the camera provider is available.
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get() // Retrieves the camera provider.

            // Configures the camera preview use case.
            val preview = Preview.Builder()
                .build()
                .apply {
                    // Sets the surface provider where the camera preview will be displayed.
                    setSurfaceProvider(previewView.surfaceProvider)
                }

            // Configures the image analysis use case.
            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                // Sets the backpressure strategy to discard old frames if the analyzer is busy.
                .build()
                .apply {
                    // Sets the analyzer that processes each frame.
                    // 'setAnalyzer' accepts an executor and an analyzer function.
                    setAnalyzer(cameraExecutor, ::analyzeImage)
                }

            try {
                cameraProvider.unbindAll() // Unbinds any previously bound use cases.

                // Binds the camera to the lifecycle with the specified use cases.
                cameraProvider.bindToLifecycle(
                    this, // LifecycleOwner, which is MainActivity in this case.
                    CameraSelector.DEFAULT_FRONT_CAMERA, // Selects the front-facing camera.
                    preview, // Adds the preview use case.
                    imageAnalyzer // Adds the image analysis use case.
                )
            } catch (e: Exception) {
                // Logs any exceptions that occur during camera setup.
                Log.e("CameraSetup", "Error binding camera use cases", e)
            }
        }, ContextCompat.getMainExecutor(this)) // Ensures that the listener runs on the main thread.
    }

    // Indicates that we are using an experimental API (getImage()).
    @OptIn(ExperimentalGetImage::class)
    // Analyzes each frame captured by the camera.
    private fun analyzeImage(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image // Gets the underlying media image.

        // Checks if the image is not null and is in the correct format.
        if (mediaImage != null && imageProxy.format == ImageFormat.YUV_420_888) {
            // Converts the YUV image to an RGB bitmap.
            val bitmap = yuvToRgb(mediaImage, imageProxy)

            // Creates a matrix to apply transformations to the bitmap.
            val matrix = Matrix().apply {
                postRotate(imageProxy.imageInfo.rotationDegrees.toFloat()) // Rotates the image based on its rotation degrees.

                // Mirrors the image horizontally to correct the front camera preview.
                postScale(-1f, 1f, bitmap.width.toFloat(), bitmap.height.toFloat())
            }

            // Creates a new bitmap with the applied transformations.
            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, // Source bitmap.
                0, 0, // Starting coordinates.
                bitmap.width, bitmap.height, // Dimensions.
                matrix, // Transformation matrix.
                true // Indicates whether to filter the bitmap.
            )

            // Builds a MediaPipe Image from the rotated bitmap.
            val mpImage = com.google.mediapipe.framework.image.BitmapImageBuilder(rotatedBitmap).build()

            val timestamp = imageProxy.imageInfo.timestamp // Gets the timestamp of the frame.

            // Performs asynchronous pose detection on the image.
            poseLandmarker.detectAsync(mpImage, timestamp)

            imageProxy.close() // Closes the image proxy to allow the next frame to be processed.
        } else {
            // Logs an error if the image format is unsupported.
            Log.e("AnalyzeImage", "Unsupported image format")
            imageProxy.close() // Closes the image proxy.
        }
    }

    // Converts a YUV image to an RGB bitmap.
    private fun yuvToRgb(image: Image, imageProxy: ImageProxy): Bitmap {
        // Accesses the Y, U, and V planes from the image.
        val yBuffer = image.planes[0].buffer // Luminance plane.
        val uBuffer = image.planes[1].buffer // Chrominance U plane.
        val vBuffer = image.planes[2].buffer // Chrominance V plane.

        // Calculates the size of each plane.
        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

        // Creates a byte array to hold the NV21 formatted data.
        val nv21 = ByteArray(ySize + uSize + vSize)

        // Copies the Y plane data into the byte array.
        yBuffer.get(nv21, 0, ySize)
        // Copies the V and U plane data into the byte array.
        vBuffer.get(nv21, ySize, vSize)
        uBuffer.get(nv21, ySize + vSize, uSize)

        // Creates a YuvImage from the NV21 byte array.
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, imageProxy.width, imageProxy.height, null)

        // Creates an output stream to hold the JPEG data.
        val out = ByteArrayOutputStream()

        // Compresses the YUV image to JPEG format.
        yuvImage.compressToJpeg(Rect(0, 0, imageProxy.width, imageProxy.height), 100, out)

        // Converts the output stream to a byte array.
        val imageBytes = out.toByteArray()

        // Decodes the byte array into a Bitmap.
        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
    }

    // Called when the activity is destroyed to clean up resources.
    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown() // Shuts down the executor service.
        poseLandmarker.close() // Closes the pose landmarker to release resources.
    }

}

