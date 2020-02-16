package com.davidcorrado.locationpermissions

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.davidcorrado.locationpermissions.MainActivity.LocationPermissionStep.LocationPermissionStep
import com.davidcorrado.locationpermissions.MainActivity.LocationPermissionStep.LocationServicesStep
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task


class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTIVITY_RESULT_LOCATION_SERVICES = 9100
        private const val ACTIVITY_RESULT_LOCATION_PERMISSIONS = 9200
        private const val TAG = "MainActivity"
    }

    //1) Check Google Play Services Available
    //2) Check Location Services Available
    //3) Check Location Permission Available
    enum class LocationPermissionStep {
        LocationServicesStep,
        LocationPermissionStep
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //TODO demo this
        //Have no means to know if permission was changed directly in settings.
        //Android restarts the activity if you revoke permissions.  If you grant permissions you should have a retry button that a user should press when they come back or some other smart solution.
        requestLocationPermissions(LocationServicesStep)
    }

    override fun onResume() {
        super.onResume()
    }

    private fun requestLocationPermissions(locationPermissionStep: LocationPermissionStep) {
        when (locationPermissionStep) {
            LocationServicesStep -> {
                val locationRequest = LocationRequest().apply {
                    interval = 10000
                    fastestInterval = 5000
                    priority = LocationRequest.PRIORITY_HIGH_ACCURACY
                }
                val builder = LocationSettingsRequest.Builder()
                    .addLocationRequest(locationRequest)
                val client: SettingsClient = LocationServices.getSettingsClient(this)
                val task: Task<LocationSettingsResponse> =
                    client.checkLocationSettings(builder.build())
                task.addOnSuccessListener {
                    requestLocationPermissions(LocationPermissionStep)
                }.addOnFailureListener { exception ->
                    if (exception is ResolvableApiException) {
                        try {
                            exception.startResolutionForResult(
                                this@MainActivity,
                                ACTIVITY_RESULT_LOCATION_SERVICES
                            )
                        } catch (sendEx: IntentSender.SendIntentException) {
                            Log.w(
                                TAG,
                                "Location Services Denied via pressing No Thanks button on dialog"
                            )
                        }
                    } else {
                        Log.w(TAG, "Location Services Denied???3")
                    }
                }

            }
            else -> {
                val locationPermission = android.Manifest.permission.ACCESS_FINE_LOCATION
                val permissionGranted = ContextCompat.checkSelfPermission(
                    this,
                    locationPermission
                ) == PackageManager.PERMISSION_GRANTED
                if (!permissionGranted) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(
                            this,
                            locationPermission
                        )
                    ) {
                        AlertDialog.Builder(this)
                            .setMessage("I need location because...")
                            .setTitle("I need location")
                            .setPositiveButton("Ok") { _, _ ->
                                requestLocationPermission(arrayOf(locationPermission))
                            }.setNegativeButton("Cancel") { _, _ ->
                                Log.w(
                                    TAG,
                                    "Location Permissions Denied by pressing cancel on permission rationale"
                                )
                            }.show()
                    } else {
                        requestLocationPermission(arrayOf(locationPermission))
                    }
                } else {
                    Log.w(TAG, "Location Permissions Already accepted")
                }
            }
        }
    }

    private fun requestLocationPermission(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, ACTIVITY_RESULT_LOCATION_PERMISSIONS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ACTIVITY_RESULT_LOCATION_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                requestLocationPermissions(LocationPermissionStep)
            } else {
                Log.w(TAG, "Location Services Denied via Cancelling the dialog(Pressed Back)")
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == ACTIVITY_RESULT_LOCATION_PERMISSIONS) {
            if (grantResults.isEmpty()) {
                //https://developer.android.com/reference/android/app/Activity.html#onRequestPermissionsResult(int,%20java.lang.String%5B%5D,%20int%5B%5D)
                Log.w(TAG, "Location Permissions Cancelled")
                return
            }
            val grantResult = grantResults[0]
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Location Permissions Pressed Allow button.")
            } else if (grantResult == PackageManager.PERMISSION_DENIED
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])
            ) {
                //TODO dont show this on first deny forever
                Log.w(TAG, "Location Permissions Deny(Don't Ask Again)")
                //Optionally Show Rationale and redirect to location settings

                AlertDialog.Builder(this)
                    .setMessage("I need location because...")
                    .setTitle("I need location")
                    .setPositiveButton("Ok") { _, _ ->
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }.setNegativeButton("Cancel") { _, _ ->
                        Log.w(
                            TAG,
                            "Location Permissions Denied by pressing cancel on permission rationale"
                        )
                    }.show()
            } else {
                Log.w(TAG, "Location Permissions Denied by pressing Deny button")
            }
        }
    }
}
