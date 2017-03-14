package sexy.mycodeis.suckzoo.sexyforecaster

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.location.Criteria
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.text.SpannableString
import android.text.Spanned
import android.text.TextUtils
import android.text.style.AbsoluteSizeSpan
import android.text.style.ForegroundColorSpan
import android.text.style.RelativeSizeSpan
import android.text.style.UnderlineSpan
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import com.github.kittinunf.fuel.android.extension.responseJson
import com.github.kittinunf.fuel.core.FuelManager
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.result.Result
import org.jetbrains.anko.*
import org.jetbrains.anko.relativeLayout
import org.jetbrains.anko.setContentView
import org.json.JSONObject

class MainActivity : AppCompatActivity() {
    val TAG = "MainActivity"
    lateinit var mainUI: MainActivityUI

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
        mainUI = MainActivityUI()
        mainUI.setContentView(this)

        handlePermission()

        FuelManager.instance.basePath = getString(R.string.api_root)
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
        mainUI.cityTextView.text = "Loading..."
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
                    val location = result.get().obj().getJSONObject("location")
                    val state = location?.getString("state")
                    val city = location?.getString("city")
                    if (state != null && city != null) {
                        mainUI.cityTextView.text = city
                        fetchWeatherData(city, state)
                        fetchForecastData(city, state)
                        fetchHourlyData(city, state)
                    } else {
                        Toast.makeText(applicationContext, "Cannot fetch location information", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    fun fetchWeatherData(city: String, state: String) {
        Log.wtf("city", city)
        Log.wtf("state", state)
        "/conditions/q/$state/$city.json".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Toast.makeText(applicationContext, "Connection problem happened: try again later :(", Toast.LENGTH_LONG).show()
                }
                is Result.Success -> {
                    Log.d("fetchGeoData", "queried.")
                    bindWeatherData(result.get().obj())
                }
            }
        }
    }

    fun fetchForecastData(city: String, state: String) {
        "/forecast/q/$state/$city.json".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Toast.makeText(applicationContext, "Connection problem happened: try again later :(", Toast.LENGTH_LONG).show()
                }
                is Result.Success -> {
                    Log.d("fetchForecastData", "queried.")
                    bindForecastData(result.get().obj())
                }
            }
        }
    }

    fun fetchHourlyData(city: String, state: String) {
        "/hourly/q/$state/$city.json".httpGet().responseJson { request, response, result ->
            when (result) {
                is Result.Failure -> {
                    Toast.makeText(applicationContext, "Connection problem happened: try again later :(", Toast.LENGTH_LONG).show()
                }
                is Result.Success -> {
                    Log.d("fetchHourlyData", "queried.")
                    bindHourlyData(result.get().obj())
                }
            }
        }
    }

    fun getDrawableIdByWeather(weatherText: String): Int {
        if (weatherText == "Clear") {
            return R.drawable.sunny
        }
        if (weatherText.indexOf("Cloud") > -1) {
            return R.drawable.cloudy
        }
        return R.drawable.rainy
    }

    fun getDrawableByWeather(weatherText: String): Drawable {
        // ^^;;
        return getDrawable(getDrawableIdByWeather(weatherText))
    }

    fun bindWeatherData(weatherData: JSONObject) {
        try {
            val degree = SpannableString("℃")
            degree.setSpan(RelativeSizeSpan(0.5f), 0, degree.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            val currentObservation = weatherData.getJSONObject("current_observation")
            val weather = currentObservation.getString("weather")
            mainUI.weatherTextView.text = weather

            val temperature = TextUtils.concat(currentObservation.getInt("temp_c").toString(), degree)
            mainUI.tempTextView.text = temperature

            mainUI.weatherImageView.image = getDrawableByWeather(weather)
            mainUI.weatherImageView.imageAlpha = 50

        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Unknown problem happened: try again later :(", Toast.LENGTH_LONG).show()
        }
    }

    fun bindForecastData(forecast: JSONObject) {
        try {
            val degree = SpannableString("℃")
            degree.setSpan(RelativeSizeSpan(0.5f), 0, degree.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

            val forecastArray = forecast.getJSONObject("forecast").getJSONObject("simpleforecast").getJSONArray("forecastday")
            val currentForecast = forecastArray.getJSONObject(0)
            val highestTemp = TextUtils.concat(currentForecast.getJSONObject("high").getString("celsius"), degree)
            val lowestTemp = TextUtils.concat(currentForecast.getJSONObject("low").getString("celsius"), degree)
            val tempRange = TextUtils.concat(highestTemp, " / ", lowestTemp)
            mainUI.tempRangeTextView.text = tempRange

            val chanceOfRain = currentForecast.getInt("pop").toString() + "% chance of rain"
            mainUI.chanceOfRainTextView.text = chanceOfRain

            mainUI.forecastLayout.removeAllViews()
            for (i in 1..forecastArray.length()) {
                forecastArray.getJSONObject(i)?.let {
                    mainUI.forecastLayout.addView(getForecastView(it, mainUI.forecastLayout))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Cannot get forecast information.", 2000)
        }
    }

    fun getForecastView(item: JSONObject, parent: View): View {
        val degree = SpannableString("℃")
        degree.setSpan(RelativeSizeSpan(0.5f), 0, degree.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        val weather = item.getString("conditions")

        val highestTemp = TextUtils.concat(item.getJSONObject("high").getString("celsius"), degree)
        val lowestTemp = TextUtils.concat(item.getJSONObject("low").getString("celsius"), degree)
        val tempRange = TextUtils.concat(highestTemp, " / ", lowestTemp)

        val pop = item.getString("pop").toString() + "%"
        val view = with(parent.context) {
            linearLayout {
                imageView {
                    imageResource = getDrawableIdByWeather(weather)
                }.lparams {
                    width = dip(24)
                    height = dip(24)
                }
                textView {
                    text = item.getJSONObject("date").getString("weekday")
                    textSize = 20f
                }
                textView {
                    text = "$weather, $tempRange, $pop"
                    textSize = 14f
                }
            }
        }
        return view
    }

    fun bindHourlyData(hourly: JSONObject) {
        try {
            val hourlyArray = hourly.getJSONArray("hourly_forecast")
            mainUI.hourlyLayout.removeAllViews()
            for (i in 0..4) {
                hourlyArray.getJSONObject(i)?.let {
                    mainUI.hourlyLayout.addView(getHourlyView(it, mainUI.hourlyLayout))
                }
            }
        } catch (e: Exception) {
            Toast.makeText(applicationContext, "Cannot get forecast information.", 2000)
        }
    }

    fun getHourlyView(item: JSONObject, parent: View): View {
        val degree = SpannableString("℃")
        degree.setSpan(RelativeSizeSpan(0.5f), 0, degree.length, Spanned.SPAN_INCLUSIVE_INCLUSIVE)

        val weather = item.getString("condition")

        val time = item.getJSONObject("FCTTIME").getString("civil")

        val temp = item.getJSONObject("temp").getString("metric")
        val tempString = TextUtils.concat(temp, degree)

        val pop = item.getString("pop").toString() + "%"
        val view = with(parent.context) {
            linearLayout {
                imageView {
                    imageResource = getDrawableIdByWeather(weather)
                }.lparams {
                    width = dip(24)
                    height = dip(24)
                }
                textView {
                    text = time
                    textSize = 20f
                }
                textView {
                    text = "$weather, $tempString, $pop"
                    textSize = 14f
                }
            }
        }
        return view
    }


}
