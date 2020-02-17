package com.davidcorrado.locationpermissions

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.tasks.Task
import kotlinx.android.synthetic.main.activity_main.*


class MainActivity : AppCompatActivity() {
    companion object {
        private const val ACTIVITY_RESULT_LOCATION_SERVICES = 9100
        private const val ACTIVITY_RESULT_LOCATION_PERMISSIONS = 9200
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        //Android restarts the activity if you revoke permissions.  If you grant permissions you should have something that polls if permission is granted when denied or have a retry button
        //https://stackoverflow.com/questions/32718933/broadcast-action-on-permission-change-in-android-m
        requestLocationServices()
    }

    //1) Check Location Services Available
    //2) Check Location Permission Available
    private fun requestLocationServices() {
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
            requestLocationPermissions()
        }.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        this@MainActivity,
                        ACTIVITY_RESULT_LOCATION_SERVICES
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    //Everywhere online says its ok to ignore this
                }
            } else {
                //I believe when Play Services Not installed
                tv_label.text = "Location Services Denied2"
            }
        }
    }

    private fun requestLocationPermissions() {
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
                    .setTitle("Allow to access your location?")
                    .setMessage("Rationale Here")
                    .setPositiveButton("OK") { _, _ ->
                        requestLocationPermission(arrayOf(locationPermission))
                    }.setNegativeButton("Cancel") { _, _ ->
                        //Denied by pressing cancel on permission rationale
                        tv_label.text = "Permission Denied1"
                    }.show()
            } else {
                requestLocationPermission(arrayOf(locationPermission))
            }
        } else {
            //Accepted before started
            tv_label.text = "Permission Authorized1"
        }
    }

    private fun requestLocationPermission(permissions: Array<String>) {
        ActivityCompat.requestPermissions(this, permissions, ACTIVITY_RESULT_LOCATION_PERMISSIONS)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            ACTIVITY_RESULT_LOCATION_SERVICES -> if (resultCode == Activity.RESULT_OK) {
                requestLocationPermissions()
            } else {
                //Location services dialog cancelled(back)
                tv_label.text = "Location Services Denied3"
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
                //Cancelled
                tv_label.text = "Permission Denied2"
                return
            }
            val grantResult = grantResults[0]
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                //Permission Dialog pressed allowed
                tv_label.text = "Permission Authorized2"
            } else if (grantResult == PackageManager.PERMISSION_DENIED
                && !ActivityCompat.shouldShowRequestPermissionRationale(this, permissions[0])
            ) {
                //Note dialog will show when you deny forever the first time and every time you reload the page afterwards
                //Optionally Show Rationale and redirect to location settings

                AlertDialog.Builder(this)
                    .setTitle("Allow to access your location?")
                    .setMessage("Rationale Here")
                    .setPositiveButton("OK") { _, _ ->
                        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                            data = Uri.fromParts("package", packageName, null)
                        })
                    }.setNegativeButton("Cancel") { _, _ ->
                        //Cancel permission rationale
                        tv_label.text = "Permission Denied3"
                    }.show()
            } else {
                //Pressed permission Deny Button
                tv_label.text = "Permission Denied4"
            }
        }
    }
}
