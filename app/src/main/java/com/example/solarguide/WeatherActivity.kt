package com.example.solarguide

import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class WeatherActivity : AppCompatActivity() {

    private val api: String = "1c5185c1e4243b004075b87faea7c274"
    private lateinit var popPlayer: MediaPlayer
    private lateinit var locationInput: EditText
    private lateinit var locationButton: Button
    private var location: String = "San Luis, PH" // Default location

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_weather)

        popPlayer = MediaPlayer.create(this, R.raw.pop_sound)

        val infoIcon2: ImageView = findViewById(R.id.info_icon2)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            infoIcon2.tooltipText = "Ryanne, Jordan, Nathaniel, Roger, Wesley, John Mark, Justine"
        } else {
            // For older Android versions, we can use a custom Toast or TooltipCompat
            infoIcon2.setOnLongClickListener {
                Toast.makeText(this, "Ryanne, Jordan, Nathaniel, Roger, Wesley, John Mark, Justine", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val infoIcon: ImageView = findViewById(R.id.info_icon)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            infoIcon.tooltipText = "Enter city name and country name for best results."
        } else {
            // For older Android versions, we can use a custom Toast or TooltipCompat
            infoIcon.setOnLongClickListener {
                Toast.makeText(this, "Enter city name and country name for best results.", Toast.LENGTH_SHORT).show()
                true
            }
        }

        val frameLayoutIds = listOf(
            R.id.WeatherFrameButton,
            R.id.BatteryFrameButton,
            R.id.PanelFrameButton,
            R.id.DevicesFrameButton,
            R.id.AccountFrameButton
        )

        val activities = listOf(
            WeatherActivity::class.java,
            BatteryPerformanceActivity::class.java,
            PanelPerformanceActivity::class.java,
            AvailableDevicesActivity::class.java,
            AccountActivity::class.java
        )

        ButtonUtils.setMenuButtonClickListener(this, frameLayoutIds, activities)
        popPlayer.start()

        locationInput = findViewById(R.id.locationInput)
        locationButton = findViewById(R.id.locationButton)

        locationButton.setOnClickListener {
            location = locationInput.text.toString().trim()
            if (location.isNotEmpty()) {
                fetchWeather()
                locationInput.setText("") // Clear input field after pressing button
            } else {
                Toast.makeText(this, "Please enter a valid location", Toast.LENGTH_SHORT).show()
            }
        }

        // Fetch default weather
        fetchWeather()
    }

    private fun fetchWeather() {
        findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.GONE
        findViewById<TextView>(R.id.errorText).visibility = View.GONE

        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = URL("https://api.openweathermap.org/data/2.5/weather?q=$location&units=metric&appid=$api")
                    .readText(Charsets.UTF_8)

                withContext(Dispatchers.Main) {
                    handleWeatherResponse(response)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Log.e("WeatherActivity", "Error fetching weather: ${e.message}", e)
                    showError()
                }
            }
        }
    }

    private fun handleWeatherResponse(response: String?) {
        try {
            response?.let {
                val jsonObj = JSONObject(it)
                Log.d("WeatherActivity", "JSON Response: $jsonObj")
                val main = jsonObj.getJSONObject("main")
                val sys = jsonObj.getJSONObject("sys")
                val wind = jsonObj.getJSONObject("wind")
                val weather = jsonObj.getJSONArray("weather").getJSONObject(0)
                val updatedAt: Long = jsonObj.getLong("dt")
                val updatedAtText =
                    "Updated at: " + SimpleDateFormat("dd/MM/yyyy hh:mm a", Locale.ENGLISH).format(
                        Date(updatedAt * 1000)
                    )
                val temp = main.getString("temp") + "°C"
                val tempMin = "Min Temp: " + main.getString("temp_min") + "°C"
                val tempMax = "Max Temp: " + main.getString("temp_max") + "°C"
                val pressure = main.getString("pressure")
                val humidity = main.getString("humidity")
                val sunrise: Long = sys.getLong("sunrise")
                val sunset: Long = sys.getLong("sunset")
                val windSpeed = wind.getString("speed")
                val weatherDescription = weather.getString("description")

                // Set the appropriate drawable based on weather status
                val imageView2: ImageView = findViewById(R.id.cloud_status) // Replace with your actual ImageView ID
                when (weatherDescription) {
                    "clear sky" -> imageView2.setImageResource(R.drawable.clear_sky)
                    "overcast clouds" -> imageView2.setImageResource(R.drawable.overcast_clouds)
                    "few clouds", "scattered clouds", "broken clouds" -> imageView2.setImageResource(R.drawable.broken_clouds)
                    "shower rain", "rain", "light rain" -> imageView2.setImageResource(R.drawable.rainy_weather)
                    "thunderstorm" -> imageView2.setImageResource(R.drawable.thunder_weather)
                    else -> imageView2.setImageResource(R.drawable.default_weather) // Fallback image
                }

                val address = jsonObj.getString("name") + ", " + sys.getString("country")

                findViewById<TextView>(R.id.address).text = address
                findViewById<TextView>(R.id.updated_at).text = updatedAtText
                findViewById<TextView>(R.id.status).text = weatherDescription
                findViewById<TextView>(R.id.temp).text = temp
                findViewById<TextView>(R.id.temp_min).text = tempMin
                findViewById<TextView>(R.id.temp_max).text = tempMax
                val dateFormat = SimpleDateFormat("hh:mm a", Locale.ENGLISH)
                dateFormat.timeZone = TimeZone.getTimeZone("Asia/Manila") // Adjust timezone

                findViewById<TextView>(R.id.sunrise).text =
                    dateFormat.format(Date(sunrise * 1000))
                findViewById<TextView>(R.id.sunset).text =
                    dateFormat.format(Date(sunset * 1000))

                findViewById<TextView>(R.id.wind).text = windSpeed
                findViewById<TextView>(R.id.pressure).text = pressure
                findViewById<TextView>(R.id.humidity).text = humidity

                findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
                findViewById<RelativeLayout>(R.id.mainContainer).visibility = View.VISIBLE
            }
        } catch (e: Exception) {
            Log.e("WeatherActivity", "Error parsing weather response: ${e.message}", e)
            showError()
        }
    }

    private fun showError() {
        findViewById<ProgressBar>(R.id.loader).visibility = View.GONE
        findViewById<TextView>(R.id.errorText).visibility = View.VISIBLE
    }
}
