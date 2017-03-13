package sexy.mycodeis.suckzoo.sexyforecaster

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.util.Log
import android.widget.Toast
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import kotlinx.android.synthetic.main.activity_main.*
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    var lastKnownLocation: Location? = null


    private val locationManager: LocationManager by lazy {
        getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    private val locationProvider: String
        get() = Criteria().let {
            it.accuracy = Criteria.ACCURACY_FINE
            locationManager.getBestProvider(it, true)
        }

    private val locationListener = object: LocationListener {
        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
        }

        override fun onProviderEnabled(provider: String?) {
        }

        override fun onProviderDisabled(provider: String?) {
        }

        override fun onLocationChanged(location: Location?) {
            location?.let {
                Log.d("onLocationChanged", "got location informations.")
                lastKnownLocation = it
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        handlePermission()

        FuelManager.instance.basePath = getString(R.string.api_root)

        refreshButton?.setOnClickListener {
            refresh()
        }
    }

    override fun onResume() {
        super.onResume()
        locationManager.requestLocationUpdates(locationProvider, 0L, 0F, locationListener)
        refresh()
    }

    override fun onPause() {
        Log.d("onLocationChanged", "removing updates...")
        locationManager.removeUpdates(locationListener)
        Log.d("onLocationChanged", "removed.")
        super.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (grantResults.size < 0 || grantResults[0] == PackageManager.PERMISSION_DENIED) {
            Log.d("permission", "I'm out.")
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }

    fun handlePermission() {
        listOf(Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.INTERNET,
                Manifest.permission.ACCESS_NETWORK_STATE).map {
            if (ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED) {
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, it)) {

                } else {
                    ActivityCompat.requestPermissions(this, arrayOf(it), 1)
                }
            }
        }
    }

    fun refresh() {
        Log.wtf("refresh", "refreshing!")
        lastKnownLocation = locationManager.getLastKnownLocation(locationProvider)
        fetchData()
    }

    fun fetchData() {
        fetchGeoData()
    }

    fun fetchGeoData() {
        Log.d("fetchGeoData", "fetching coordinates...")
        val latitude: Double = lastKnownLocation!!.latitude
        val longitude: Double = lastKnownLocation!!.longitude
        Log.d("fetchGeoData", "lat: $latitude, lon: $longitude")

        Log.d("fetchGeoData", "querying city...")
        "/geolookup/q/$latitude,$longitude.json".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Toast.makeText(applicationContext, "Connection problem happened: try again later :(", 2000)
                }
                is Result.Success -> {
                    Log.d("fetchGeoData", "queried.")
                    fetchWeatherData(result.get().obj())
                }
            }
        }
    }

    fun fetchWeatherData(geoData: JSONObject?) {
        val location = geoData?.getJSONObject("location")
        val state = location?.getString("state")
        val city = location?.getString("city")
        if (state != null && city != null) {
            Log.wtf("city", city)
            Log.wtf("state", state)
            "/conditions/q/$state/$city.json".httpGet().responseJson { request, response, result ->
                when (result) {
                    is Result.Failure -> {
                        Toast.makeText(applicationContext, "Connection problem happened: try again later :(", 2000)
                    }
                    is Result.Success -> {
                        Log.d("fetchGeoData", "queried.")
                        bindData(city, result.get().obj())
                    }
                }
            }
        } else {
            Toast.makeText(applicationContext, "Cannot find any station nearby :(", 2000)
        }
    }

    fun bindData(city: String, weather: JSONObject) {
        cityTextView.text = city
        Log.wtf("city", city)
    }
}
