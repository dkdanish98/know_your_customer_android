package com.example.knowyourcustomerandroid

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.huawei.agconnect.config.AGConnectServicesConfig
import com.huawei.hms.mlsdk.common.MLApplication
import com.huawei.hms.mlsdk.interactiveliveness.MLInteractiveLivenessCapture
import com.huawei.hms.mlsdk.interactiveliveness.MLInteractiveLivenessCaptureConfig
import com.huawei.hms.mlsdk.interactiveliveness.MLInteractiveLivenessCaptureError
import com.huawei.hms.mlsdk.interactiveliveness.MLInteractiveLivenessCaptureResult
import com.huawei.hms.mlsdk.interactiveliveness.action.InteractiveLivenessStateCode
import com.huawei.hms.mlsdk.interactiveliveness.action.MLInteractiveLivenessConfig
import com.huawei.hms.mlsdk.livenessdetection.MLLivenessDetectView

class MainActivity : AppCompatActivity() {

    private lateinit var retestButton: Button
    private lateinit var exitButton: Button
    private lateinit var faceSuccessImageView: ImageView
    private lateinit var resultImageView: ImageView
    private lateinit var mDetectionFailedImageView: ImageView
    private lateinit var mBackImage: ImageView
    private lateinit var mMaskingView: MaskingView
    private lateinit var mTextSuccess: TextView
    private lateinit var mTextFail: TextView
    private lateinit var mTextFailResult: TextView

    private var mBitmap: Bitmap? = null

    private val TAG = "StartActivity"

    private val PERMISSION_REQUESTS = 1

    private lateinit var mlLivenessDetectView: MLLivenessDetectView

    private lateinit var mPreviewContainer: FrameLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        retestButton = findViewById<View>(R.id.retest_button) as Button
        exitButton = findViewById<View>(R.id.exit_button) as Button
        faceSuccessImageView = findViewById<View>(R.id.imageView) as ImageView
        resultImageView = findViewById<View>(R.id.success_image) as ImageView
        mDetectionFailedImageView = findViewById<View>(R.id.detection_failed_imageView) as ImageView
        //retestButton.setOnClickListener(ClickListenerImpl())
        //exitButton.setOnClickListener(ClickListenerImpl())
        mBackImage = findViewById<View>(R.id.img_back) as ImageView
        //mBackImage.setOnClickListener(ClickListenerImpl())
        mMaskingView = findViewById<View>(R.id.masking_view) as MaskingView
        mTextSuccess = findViewById<View>(R.id.textView) as TextView
        mTextFail = findViewById<View>(R.id.detection_failed_textView) as TextView
        mTextFailResult = findViewById<View>(R.id.failure_cause_textView) as TextView

        // Set the ApiKey of the application for accessing cloud services.
        setApiKey()
        //mPreviewContainer = findViewById(R.id.sur)
        if (!allPermissionsGranted()) {
            getRuntimePermissions()
        }

        val interactiveLivenessConfig = MLInteractiveLivenessConfig.Builder().build()

        val captureConfig = MLInteractiveLivenessCaptureConfig.Builder()
            .setOptions(MLInteractiveLivenessCaptureConfig.DETECT_MASK)
            .setActionConfig(interactiveLivenessConfig)
            .build()
        val capture = MLInteractiveLivenessCapture.getInstance()
        capture.setConfig(captureConfig)
        //startTime = System.currentTimeMillis()
        capture.startDetect(this, this.callback)

//        mlLivenessDetectView = MLLivenessDetectView.Builder().setContext(this)
//            .setOptions(MLLivenessDetectView.DETECT_MASK).setFaceFrameRect(
//                Rect(0, 0, 0, 200)
//            ).setDetectCallback(object : OnMLLivenessDetectCallback {
//                override fun onCompleted(result: MLLivenessCaptureResult?) {
//
//                }
//
//                override fun onError(error: Int) {
//
//                }
//
//                override fun onInfo(infoCode: Int, bundle: Bundle?) {}
//
//                override fun onStateChange(state: Int, bundle: Bundle?) {}
//            }).build()
//
//        mPreviewContainer.addView(mlLivenessDetectView)
//        mlLivenessDetectView.onCreate(savedInstanceState)
    }

    private val callback: MLInteractiveLivenessCapture.Callback =
        object : MLInteractiveLivenessCapture.Callback {
            override fun onSuccess(result: MLInteractiveLivenessCaptureResult) {
                Log.e(
                    TAG,
                    "Success detection, Thread id is: " + Thread.currentThread().id
                )
                Log.e(
                    TAG,
                    "result.getStateCode(): " + result.stateCode
                )
//                endTime = System.currentTimeMillis()
//                val delayTime: Long = endTime - startTime
//                Log.d(
//                    com.huawei.mlkit.sample.activity.interactivelivenessdetection.InteractiveLivenessDetectionActivity.TAG,
//                    "Detection time is: $delayTime"
//                )
                Log.d(
                    TAG,
                    "Result stateCode is: " + result.stateCode
                )
                if (result.bitmap != null) {
                    Log.d(
                        TAG,
                        "result.getBitmap is not null"
                    )
                    mBitmap = result.bitmap
                }
                mBitmap?.let { handleDetectionResult(result, it) }
            }

            private fun handleDetectionResult(
                result: MLInteractiveLivenessCaptureResult,
                faceBitmap: Bitmap
            ) {
                val detectionResult = result.stateCode
                val actionStringId =
                    MLInteractiveLivenessConfig.getActionDescByType(result.actionType)
                InteractiveLivenessDetectionResultEnum.defaultDetectionFailResultProcess(
                    mTextFailResult, detectionResult, actionStringId, false
                )
                if (result.stateCode == InteractiveLivenessStateCode.ALL_ACTION_CORRECT) {
                    faceSuccessImageView.setImageBitmap(faceBitmap)
                    resultImageView.setImageResource(R.drawable.ic_public_todo_succeed)
                    mMaskingView.setVisibility(View.VISIBLE)
                    mDetectionFailedImageView.setVisibility(View.INVISIBLE)
                    faceSuccessImageView.setVisibility(View.VISIBLE)
                    mTextSuccess.setVisibility(View.VISIBLE)
                    mTextFail.setVisibility(View.INVISIBLE)
                    mTextFailResult.setVisibility(View.INVISIBLE)
                } else {
                    mDetectionFailedImageView.setImageResource(R.mipmap.detectionfailed)
                    resultImageView.setImageResource(R.drawable.ic_public_close_filled)
                    mMaskingView.setVisibility(View.INVISIBLE)
                    mDetectionFailedImageView.setVisibility(View.VISIBLE)
                    faceSuccessImageView.setVisibility(View.INVISIBLE)
                    mTextSuccess.setVisibility(View.INVISIBLE)
                    mTextFail.setVisibility(View.VISIBLE)
                    mTextFailResult.setVisibility(View.VISIBLE)
                }
            }

            override fun onFailure(errorCode: Int) {
                Log.d(
                    TAG,
                    "Fail detection, Thread id is: " + Thread.currentThread().id
                )
                var failResult: String? = null
                when (errorCode) {
                    MLInteractiveLivenessCaptureError.CAMERA_NO_PERMISSION -> failResult =
                        "The camera permission is not obtained."

                    MLInteractiveLivenessCaptureError.CAMERA_START_FAILED -> failResult =
                        "Failed to start the camera."

                    MLInteractiveLivenessCaptureError.DETECT_FACE_TIME_OUT -> failResult =
                        "Detection timed out by the face detection module."

                    MLInteractiveLivenessCaptureError.USER_CANCEL -> finish()
                    -1 -> {
                        failResult = "Initialization failed."
                        finish()
                    }

                    -6001 -> {
                        failResult = "The offline usage exceeds the threshold."
                        finish()
                    }

                    -6002 -> {
                        failResult = "The offline time exceeds the threshold."
                        finish()
                    }

                    -5001 -> {
                        failResult = "Payment is not enabled, and the free quota is used up."
                        finish()
                    }

                    -5002 -> {
                        failResult = "Account Arrears."
                        finish()
                    }

                    -5003 -> {
                        failResult = "Blacklist."
                        finish()
                    }

                    else -> {}
                }
            }
        }


    private fun setApiKey() {
        val config = AGConnectServicesConfig.fromContext(application)
        MLApplication.getInstance().apiKey =
            config.getString("client/api_key")
    }

    private fun getRequiredPermissions(): Array<String?> {
        return try {
            val info = this.packageManager
                .getPackageInfo(this.packageName, PackageManager.GET_PERMISSIONS)
            val ps = info.requestedPermissions
            if (ps != null && ps.size > 0) {
                ps
            } else {
                arrayOfNulls(0)
            }
        } catch (e: RuntimeException) {
            throw e
        } catch (e: Exception) {
            arrayOfNulls(0)
        }
    }

    private fun allPermissionsGranted(): Boolean {
        for (permission in getRequiredPermissions()) {
            if (!isPermissionGranted(
                    this,
                    permission
                )
            ) {
                return false
            }
        }
        return true
    }

    private fun getRuntimePermissions() {
        val allNeededPermissions: MutableList<String?> = ArrayList()
        for (permission in getRequiredPermissions()) {
            if (!isPermissionGranted(
                    this,
                    permission
                )
            ) {
                allNeededPermissions.add(permission)
            }
        }
        if (!allNeededPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                allNeededPermissions.toTypedArray<String?>(),
                PERMISSION_REQUESTS
            )
        }
    }

    private fun isPermissionGranted(context: Context, permission: String?): Boolean {
        if (permission?.let { ContextCompat.checkSelfPermission(context, it) }
            == PackageManager.PERMISSION_GRANTED
        ) {
            Log.i(
                TAG,
                "Permission granted: $permission"
            )
            return true
        }
        Log.i(
            TAG,
            "Permission NOT granted: $permission"
        )
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode != PERMISSION_REQUESTS) {
            return
        }
        var isNeedShowDiag = false
        for (i in permissions.indices) {
            if (permissions[i] == Manifest.permission.CAMERA && grantResults[i] != PackageManager.PERMISSION_GRANTED || permissions[i] == Manifest.permission.READ_EXTERNAL_STORAGE && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                // If the camera or storage permissions are not authorized, need to pop up an authorization prompt box.
                isNeedShowDiag = true
            }
        }
        if (isNeedShowDiag && !ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.CALL_PHONE
            )
        ) {
            val dialog = AlertDialog.Builder(this)
                .setMessage(this.getString(R.string.camera_permission_rationale))
                .setPositiveButton(
                    this.getString(R.string.settings)
                ) { dialog, which ->
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    // Open the corresponding setting interface according to the package name.
                    intent.setData(Uri.parse("package:" + this@MainActivity.packageName))
                    this@MainActivity.startActivityForResult(intent, 200)
                    this@MainActivity.startActivity(intent)
                }
                .setNegativeButton(
                    this.getString(R.string.cancel)
                ) { dialog, which -> this@MainActivity.finish() }.create()
            dialog.show()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 200) {
            if (!allPermissionsGranted()) {
                getRuntimePermissions()
            }
        }
    }

}