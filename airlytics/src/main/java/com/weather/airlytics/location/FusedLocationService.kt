package com.weather.airlytics.location

import android.annotation.SuppressLint
import android.app.Service
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient

class FusedLocationService : Service() {

    // bunch of location related apis
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var lastLat : String = ""
    private var lastLon: String  = ""

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        init()
        Log.v(TAG, "start Locations service")
        return START_STICKY
    }

    /**
     * Initialize Location Apis
     * Create Builder if Share location true
     */
    private fun init() {
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                receiveLocation(locationResult)
            }
        }
        mLocationRequest = LocationRequest()
        mLocationRequest?.let {
            it.interval = (interval * 1000).toLong()
            it.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            val builder: LocationSettingsRequest.Builder = LocationSettingsRequest.Builder()
            builder.addLocationRequest(it)
            mLocationSettingsRequest = builder.build()
            startLocationUpdates()
        }
    }

    /**
     * Request Location Update
     */
    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        mSettingsClient
            ?.checkLocationSettings(mLocationSettingsRequest)
            ?.addOnSuccessListener {
                Log.v(
                    TAG,
                    "All location settings are satisfied. No MissingPermission"
                )
                mFusedLocationClient?.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback,
                    Looper.myLooper()
                )
            }
            ?.addOnFailureListener { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> Log.e(
                        TAG,
                        "settings are not satisfied. Attempting to upgrade location settings " + e.message
                    )
                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> Log.e(
                        TAG,
                        "settings are inadequate, and cannot be " + "fixed here. Fix in Settings." + e.message
                    )
                }
            }
    }

    /**
     * onLocationResult
     * on Receive Location  share to other activity and save if save true
     *
     * @param locationResult
     */
    private fun receiveLocation(locationResult: LocationResult) {
        val location: Location = locationResult.lastLocation
        val curAlt = getLocationValue(location.latitude)
        val curLon = getLocationValue(location.longitude)

        if (lastLat != curAlt || lastLon != curLon) {
            lastLat = curAlt
            lastLon = curLon
            Log.v(
                TAG,
                    "sending location event to airlytics :$lastLat , $lastLon"
                )
            //TODO send the actual event to airlytics
        }
    }

    private fun getLocationValue(locationValue: Double?): String {
        if (locationValue == null){
            return "0"
        }
        return String.format("%." + precision + "f", locationValue)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    /**
     * Remove Location Update
     */
    private fun stopLocationUpdates() {
        mFusedLocationClient
            ?.removeLocationUpdates(mLocationCallback)
            ?.addOnCompleteListener { Log.v(TAG, "stopLocationUpdates") }
        mFusedLocationClient = null
    }



    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    companion object {
        private const val TAG = "Locations"
        var precision = -1
        var interval = -1
        private var mFusedLocationClient: FusedLocationProviderClient? = null


        fun canStartService(): Boolean{
            return mFusedLocationClient == null && precision > 0 && interval > 0
        }
    }
}