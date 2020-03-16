package com.corradodev.location_services_permissions

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.annotation.NonNull
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.PluginRegistry
import io.flutter.plugin.common.PluginRegistry.ViewDestroyListener
import io.flutter.view.FlutterNativeView

/** LocationServicesPermissionsPlugin */
class LocationServicesPermissionsPlugin : FlutterPlugin, ViewDestroyListener, EventChannel.StreamHandler, ActivityAware, Application.ActivityLifecycleCallbacks, PluginRegistry.RequestPermissionsResultListener, PluginRegistry.ActivityResultListener {
    private var activityBinding: ActivityPluginBinding? = null
    private var locationServicesAndPermissionsCallback: ((LocationServicesAndPermissionStatus) -> Unit)? = null
    private var lastLocationServicesAndPermissionStatus: LocationServicesAndPermissionStatus? = null

    companion object {
        private const val EVENT_CHANNEL_NAME = "corradodev.com/location_services_permissions/updates"
        private const val ACTIVITY_RESULT_LOCATION_SERVICES = 9100
        private const val ACTIVITY_RESULT_LOCATION_PERMISSIONS = 9200
        private const val LOCATION_PERMISSION = android.Manifest.permission.ACCESS_FINE_LOCATION
    }

    // FlutterPlugin
    override fun onAttachedToEngine(@NonNull flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val eventChannel = EventChannel(flutterPluginBinding.binaryMessenger, EVENT_CHANNEL_NAME)
        eventChannel.setStreamHandler(this)
    }

    override fun onViewDestroy(view: FlutterNativeView?): Boolean {
        onCancel(null)
        return false
    }

    override fun onDetachedFromEngine(@NonNull binding: FlutterPlugin.FlutterPluginBinding) {
    }

    //Stream Handler
    override fun onListen(arguments: Any?, events: EventChannel.EventSink?) {
        locationServicesAndPermissionsCallback = {
            lastLocationServicesAndPermissionStatus = it
            events?.success(it.name)
        }
        requestLocationServices()
    }

    override fun onCancel(arguments: Any?) {
        locationServicesAndPermissionsCallback = null
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

    override fun onActivityResumed(activity: Activity?) {
        updateLocationServicesAndPermissionCallbackIfChanged()
    }

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
        binding.addRequestPermissionsResultListener(this)
        binding.addActivityResultListener(this)
        binding.activity.application.registerActivityLifecycleCallbacks(this)
    }

    private fun detachFromActivity() {
        val binding = activityBinding ?: return

        binding.removeRequestPermissionsResultListener(this)
        binding.removeActivityResultListener(this)
        binding.activity.application.unregisterActivityLifecycleCallbacks(this)

        activityBinding = null
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>?, grantResults: IntArray?): Boolean {
        val activity = activityBinding!!.activity
        if (requestCode == ACTIVITY_RESULT_LOCATION_PERMISSIONS) {
            if (grantResults?.isEmpty() == true) {
                //Location permission request interrupted
                //https://developer.android.com/reference/android/app/Activity.html#onRequestPermissionsResult(int,%20java.lang.String%5B%5D,%20int%5B%5D)
                locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionDenied)
                return true
            }
            val grantResult = grantResults!![0]
            if (grantResult == PackageManager.PERMISSION_GRANTED) {//Location permission dialog pressed "Allow"
                locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionAllowed)
            } else if (grantResult == PackageManager.PERMISSION_DENIED
                    && !ActivityCompat.shouldShowRequestPermissionRationale(activity, permissions!![0])
            ) { //Location permission denied forever
                MaterialAlertDialogBuilder(activity, R.style.Theme_MaterialComponents_Light_Dialog_Alert)
                        .setTitle("Allow to access your location?")
                        .setMessage("Permission required in order to request local events.")
                        .setCancelable(false)
                        .setPositiveButton("OK") { _, _ ->
                            activity.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                data = Uri.fromParts("package", activity.packageName, null)
                            })
                        }.setNegativeButton("Cancel") { _, _ ->
                            //Location dialog denied by pressing "Cancel"
                            locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionDenied)
                        }.show()
            } else {
                //Location permission dialog pressed "Deny" button
                locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionDenied)
            }
            return true
        }
        return false
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        when (requestCode) {
            ACTIVITY_RESULT_LOCATION_SERVICES -> {
                if (resultCode == Activity.RESULT_OK) {
                    //Location services dialog pressed "OK"
                    requestLocationPermissions()
                } else {
                    //Location services dialog pressed back or pressed "No Thanks" button
                    locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationServicesDisabled)
                }
                return true
            }
        }
        return false
    }


    private fun requestLocationServices() {
        val activity = activityBinding!!.activity
        val builder = LocationSettingsRequest.Builder().addLocationRequest(LocationRequest())
        val client: SettingsClient = LocationServices.getSettingsClient(activity)
        val task: Task<LocationSettingsResponse> =
                client.checkLocationSettings(builder.build())
        task.addOnSuccessListener {
            requestLocationPermissions()
        }.addOnFailureListener { exception ->
            locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationServicesDisabled)
            if (exception is ResolvableApiException) {
                try {
                    //Try to resolve the location permission issue.
                    exception.startResolutionForResult(activity,
                            ACTIVITY_RESULT_LOCATION_SERVICES
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    private fun requestLocationPermissions() {
        val activity = activityBinding!!.activity
        val permissionGranted = isLocationPermissionGranted()
        if (!permissionGranted) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(activity, LOCATION_PERMISSION)) {
                MaterialAlertDialogBuilder(activity, R.style.Theme_MaterialComponents_Light_Dialog_Alert)
                        .setTitle("Allow to access your location?")
                        .setMessage("Permission required in order to request local events.")
                        .setCancelable(false)
                        .setPositiveButton("OK") { _, _ ->
                            requestLocationPermission(arrayOf(LOCATION_PERMISSION))
                        }.setNegativeButton("Cancel") { _, _ ->
                            //Location dialog denied by pressing "Cancel"
                            locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionDenied)
                        }.show()
            } else {
                requestLocationPermission(arrayOf(LOCATION_PERMISSION))
            }
        } else {
            //Accepted location permission before app loaded
            locationServicesAndPermissionsCallback?.invoke(LocationServicesAndPermissionStatus.LocationPermissionAllowed)
        }
    }

    private fun updateLocationServicesAndPermissionCallbackIfChanged() {
        if (locationServicesAndPermissionsCallback == null) {
            return
        }
        if (!isLocationEnabled()) {
            updateLocationServicesAndPermissionCallbackIfChanged(LocationServicesAndPermissionStatus.LocationServicesDisabled)
        } else if (isLocationPermissionGranted()) {
            updateLocationServicesAndPermissionCallbackIfChanged(LocationServicesAndPermissionStatus.LocationPermissionAllowed)
        } else {
            updateLocationServicesAndPermissionCallbackIfChanged(LocationServicesAndPermissionStatus.LocationPermissionDenied)
        }
    }

    private fun updateLocationServicesAndPermissionCallbackIfChanged(status: LocationServicesAndPermissionStatus) {
        if (lastLocationServicesAndPermissionStatus != status) {
            locationServicesAndPermissionsCallback?.invoke(status)
        }
    }

    private fun isLocationEnabled(): Boolean {
        val locationManager = activityBinding!!.activity.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return LocationManagerCompat.isLocationEnabled(locationManager)
    }

    private fun isLocationPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
                activityBinding!!.activity,
                LOCATION_PERMISSION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestLocationPermission(permissions: Array<String>) {
        val activity = activityBinding!!.activity
        ActivityCompat.requestPermissions(activity, permissions, ACTIVITY_RESULT_LOCATION_PERMISSIONS)
    }
}