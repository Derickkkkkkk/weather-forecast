@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.myapplication

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.gson.annotations.SerializedName
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class WeatherResponse(
    @SerializedName("items") val items: List<WeatherItem>,
    @SerializedName("api_info") val apiInfo: ApiInfo?
)

data class ApiInfo(
    @SerializedName("status") val status: String
)

data class WeatherItem(
    @SerializedName("update_timestamp") val updateTimestamp: String,
    @SerializedName("timestamp") val timestamp: String,
    @SerializedName("valid_period") val validPeriod: ValidPeriod,
    @SerializedName("general") val general: GeneralWeather,
    @SerializedName("periods") val periods: List<Period>
)

data class ValidPeriod(
    @SerializedName("start") val start: String,
    @SerializedName("end") val end: String
)

data class GeneralWeather(
    @SerializedName("forecast") val forecast: String,
    @SerializedName("relative_humidity") val relativeHumidity: Humidity,
    @SerializedName("temperature") val temperature: Temperature,
    @SerializedName("wind") val wind: Wind
)

data class Humidity(
    @SerializedName("high") val high: Int,
    @SerializedName("low") val low: Int
)

data class Temperature(
    @SerializedName("high") val high: Int,
    @SerializedName("low") val low: Int
)

data class Wind(
    @SerializedName("direction") val direction: String,
    @SerializedName("speed") val speed: WindSpeed
)

data class WindSpeed(
    @SerializedName("high") val high: Int,
    @SerializedName("low") val low: Int
)

data class Period(
    @SerializedName("time") val time: ValidPeriod,
    @SerializedName("regions") val regions: Map<String, String>
)

interface WeatherApi {
    @GET("v1/environment/24-hour-weather-forecast")
    fun getWeather(): retrofit2.Call<WeatherResponse>
}

object RetrofitClient {
    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.data.gov.sg/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    val weatherApi: WeatherApi by lazy {
        retrofit.create(WeatherApi::class.java)
    }
}


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                WeatherApp()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherApp() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var isCelsius by remember { mutableStateOf(true) }

    Scaffold(
        bottomBar = {
            if (currentRoute != "settings") {
                NavigationBar {
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.weather),
                                contentDescription = "",
                                tint = if (currentRoute == "weather") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = { Text("Weather") },
                        selected = currentRoute == "weather",
                        onClick = { navController.navigate("weather") }
                    )
                    NavigationBarItem(
                        icon = {
                            Icon(
                                painter = painterResource(id = R.drawable.setting),
                                contentDescription = "",
                                tint = if (currentRoute == "weather") {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                }
                            )
                        },
                        label = { Text("Setting") },
                        selected = currentRoute == "settings",
                        onClick = { navController.navigate("settings") }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "weather",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("weather") {
                WeatherScreen(isCelsius)
            }
            composable("settings") {
                SettingsScreen(navController, isCelsius,
                    onTemperatureUnitChange = { newUnit ->
                        isCelsius = newUnit
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(isCelsius: Boolean) {
    var weatherItems by remember { mutableStateOf<List<WeatherItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        fetchWeather(
            onSuccess = { items ->
                weatherItems = items
                isLoading = false
            },
            onError = { error ->
                errorMessage = error
                isLoading = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    isCelsius: Boolean,
    onTemperatureUnitChange: (Boolean) -> Unit
) {

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setting") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Text("←", fontSize = 24.sp)
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Unit of temperature",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isCelsius,
                                    onClick = { onTemperatureUnitChange(true) }
                                )
                                Text(
                                    text = "Celsius (°C)",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = !isCelsius,
                                    onClick = { onTemperatureUnitChange(false) }
                                )
                                Text(
                                    text = "Fahrenheit (°F)",
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(start = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

fun fetchWeather(
    onSuccess: (List<WeatherItem>) -> Unit,
    onError: (String) -> Unit
) {
    val call = RetrofitClient.weatherApi.getWeather()

    call.enqueue(object : Callback<WeatherResponse> {
        override fun onResponse(
            call: Call<WeatherResponse>,
            response: Response<WeatherResponse>
        ) {
            if (response.isSuccessful) {
                val body = response.body()
                if (body != null && body.items.isNotEmpty()) {
                    onSuccess(body.items)
                } else {
                    onError("No Data.")
                }
            } else {
                onError("Network error: ${response.code()}")
            }
        }

        override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
            onError(t.message ?: "Network Error.")
        }
    })
}

fun formatTime(timestamp: String): String {
    return try {
        val format = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+08:00'", Locale.US)
        val date = format.parse(timestamp)
        val outputFormat = SimpleDateFormat("HH:mm", Locale.US)
        outputFormat.format(date ?: Date())
    } catch (e: Exception) {
        timestamp
    }
}