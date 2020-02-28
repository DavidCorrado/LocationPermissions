package com.corradodev.location_services_permissions

import android.app.Activity
import android.app.AlertDialog
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.BinaryMessenger
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.Registrar
import io.flutter.plugin.common.PluginRegistry.ViewDestroyListener
import io.flutter.view.FlutterNativeView

/** LocationServicesPermissionsPlugin */
class LocationServicesPermissionsPlugin : FlutterPlugin, ViewDestroyListener, EventChannel.StreamHandler, ActivityAware, Application.ActivityLifecycleCallbacks, PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {
    private var activityBinding: ActivityPluginBinding? = null
    private var locationServicesAndPermissionsCallback: ((String) -> Unit)? = null

    // This static function is optional and equivalent to onAttachedToEngine. It supports the old
    // pre-Flutter-1.12 Android projects. You are encouraged to continue supporting
    // plugin registration via this function while apps migrate to use the new Android APIs
    // post-flutter-1.12 via https://flutter.dev/go/android-project-migration.
    //
    // It is encouraged to share logic between onAttachedToEngine and registerWith to keep
    // them functionally equivalent. Only one of onAttachedToEngine or registerWith will be called
    // depending on the user's project. onAttachedToEngine or registerWith must both be defined
    // in the same class.
    companion object {
        private const val EVENT_CHANNEL_NAME = "corradodev.com/location_services_permissions/updates"
        private const val ACTIVITY_RESULT_LOCATION_SERVICES = 9100
        private const val ACTIVITY_RESULT_LOCATION_PERMISSIONS = 9200
        @JvmStatic
        fun registerWith(registrar: Registrar) {
            Log.v("LocationServices", "registerWith")
            val plugin = LocationServicesPermissionsPlugin()
            registrar.addRequestPermissionsResultListener(plugin)
            registrar.addActivityResultListener(plugin)
            registrar.activity().application.registerActivityLifecycleCallbacks(plugin)
            plugin.register(registrar.messenger())
        }
    }

    // FlutterPlugin
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        Log.v("LocationServices", "onAttachedToEngine")
        register(flutterPluginBinding.binaryMessenger)
    }

    private fun register(binaryMessenger: BinaryMessenger) {
        val eventChannel = EventChannel(binaryMessenger, EVENT_CHANNEL_NAME)
        eventChannel.setStreamHandler(this)
        Log.v("LocationServices", "Register")
    }

    override fun onViewDestroy(view: FlutterNativeView?): Boolean {
        onCancel(null)
        return false
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    }

    //Stream Handler
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        Log.v("LocationServices", "Listen")
        registerLocationServicesAndPermissionsCallback {
            Log.v("LocationServices", "Listen" + it)
            events?.success(it)
        }
    }

    override fun onCancel(arguments: Any?) {
        deregisterLocationServicesAndPermissionsCallback()
    }

    //Activity Lifecycle
    override fun onDetachedFromActivity() {
        detachFromActivity()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        onAttachedToActivity(binding)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        attachToActivity(binding)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        onDetachedFromActivity()
    }

    override fun onActivityPaused(activity: Activity?) {}

    override fun onActivityResumed(activity: Activity?) {}

    override fun onActivityStarted(activity: Activity?) {}

    override fun onActivityDestroyed(activity: Activity?) {}

    override fun onActivitySaveInstanceState(activity: Activity?, outState: Bundle?) {}

    override fun onActivityStopped(activity: Activity?) {}

    override fun onActivityCreated(activity: Activity?, savedInstanceState: Bundle?) {}

    private fun attachToActivity(binding: ActivityPluginBinding) {
        if (activityBinding != null) {
            detachFromActivity()
        }
        activityBinding = binding
        Log.v("LocationServices", "attachToActivity")
        binding.addRequestPermissionsResultListener(this)
        binding.addActivityResultListener(this)
        binding.activity.application.registerActivityLifecycleCallbacks(this)

        requestLocationServices()
    }

    private fun detachFromActivity() {
        val binding = activityBinding ?: return

        binding.removeRequestPermissionsResultListener(this)
        binding.removeActivityResultListener(this)
        binding.activity.application.unregisterActivityLifecycleCallbacks(this)

        activityBinding = null
    }

    //Request Location
    //1) Check Location Services Available
    //2) Check Location Permission Available
    private fun requestLocationServices() {
        Log.v("LocationServices", "requestLocationServices")
        val activity = activityBinding!!.activity
        val locationManager = activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (LocationManagerCompat.isLocationEnabled(locationManager)) {
            requestLocationPermissions()
            return
        }
        val builder = LocationSettingsRequest.Builder().addLocationRequest(LocationRequest())
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> =
                client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            requestLocationPermissions()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(activity,
                            ACTIVITY_RESULT_LOCATION_SERVICES
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    //Everywhere online says its ok to ignore this
                    locationServicesAndPermissionsCallback?.invoke("LocationServicesDisabled")
                }
            } else {
                //I believe when Play Services Not installed
                locationServicesAndPermissionsCallback?.invoke("LocationServicesDisabled")
            }
        }
    }

    private fun requestLocationPermissions() {
        Log.v("LocationServices", "requestLocationPermissions")
        val activity = activityBinding!!.activity
        val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val permissionGranted = ContextCompat.checkSelfPermission(
                activity,
                locationPermission
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(
                            activity,
                            locationPermission
                    )
            ) {
                AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog)
                        .setTitle("Allow to access your location?")
                        .setMessage("Rationale Here")
                        .setPositiveButton("OK") { _, _ ->
                            requestLocationPermission(arrayOf(locationPermission))
                        }.setNegativeButton("Cancel") { _, _ ->
                            //Denied by pressing cancel on permission rationale
                            //tv_label.text = "Permission Denied1"
                        }.show()
            } else {
                requestLocationPermission(arrayOf(locationPermission))
            }
        } else {
            //Accepted before started
            locationServicesAndPermissionsCallback?.invoke("LocationPermissionAllowed")
        }
    }

    private fun requestLocationPermission(permissions: Array<String>) {
        val activity = activityBinding!!.activity
        ActivityCompat.requestPermissions(activity, permissions, ACTIVITY_RESULT_LOCATION_PERMISSIONS)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        val activity = activityBinding!!.activity
        if (requestCode == ACTIVITY_RESULT_LOCATION_PERMISSIONS) {
            if (grantResults?.isEmpty() == true) {
                //https://developer.android.com/reference/android/app/Activity.html#onRequestPermissionsResult(int,%20java.lang.String%5B%5D,%20int%5B%5D)
                //Cancelled
                locationServicesAndPermissionsCallback?.invoke("LocationPermissionDenied")
                return true
            }
            val grantResult = grantResults!![0]
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                //Permission Dialog pressed allowed
                Log.v("LocationServices", "onRequestPermissionsResult LocationPermissionAllowed")
                locationServicesAndPermissionsCallback?.invoke("LocationPermissionAllowed")
            } else if (grantResult == PackageManager.PERMISSION_DENIED
                    && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions!![0])
            ) {
                //Note dialog will show when you deny forever the first time and every time you reload the page afterwards
                //Optionally Show Rationale and redirect to location settings

                AlertDialog.Builder(activity, android.R.style.Theme_Material_Light_Dialog)
                        .setTitle("Allow to access your location?")
                        .setMessage("Rationale Here")
                        .setPositiveButton("OK") { _, _ ->
                            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", activity.packageName, null)
                            })
                        }.setNegativeButton("Cancel") { _, _ ->
                            //Cancel permission rationale
                            //tv_label.text = "Permission Denied3"
                        }.show()
            } else {
                //Pressed permission Deny Button
                locationServicesAndPermissionsCallback?.invoke("LocationPermissionDenied")
            }
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (resultCode) {
            ACTIVITY_RESULT_LOCATION_SERVICES -> {
                if (requestCode == Activity.RESULT_OK) {
                    requestLocationPermissions()
                } else {
                    //Location services dialog cancelled(back)
                    locationServicesAndPermissionsCallback?.invoke("LocationServicesDisabled")
                }
                return true
            }
        }
        return false
    }

    //Location Services and Permission Updates
    private fun registerLocationServicesAndPermissionsCallback(callback: (String) -> Unit) {
        check(locationServicesAndPermissionsCallback == null) { "trying to register a 2nd location services and permissions callback" }
        locationServicesAndPermissionsCallback = callback
    }

    private fun deregisterLocationServicesAndPermissionsCallback() {
        check(locationServicesAndPermissionsCallback != null) { "trying to deregister a non-existent location services and permissions callback" }
        locationServicesAndPermissionsCallback = null
    }
}