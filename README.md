# Singapore Weather Forecast

An Android weather app developed based on the Singapore government's open data API, built using Kotlin + Jetpack Compose, with network requests handled by Retrofit + Gson.

## Functions

### 1. Weather forecast

Overall weather overview: Displays comprehensive information such as temperature, humidity, wind direction, and wind speed
Regional weather forecast: Displaying the weather conditions of five regions in Singapore: North, South, East, West, and Central
Multi-time forecast: Provides weather change information for different time periods.

### 2. Switching of temperature units

Supports switching between Celsius (°C) and Fahrenheit (°F)

## Main Implementation

### 1. Network request (using Retrofit callback)

```
fun fetchWeather(
    onSuccess: (List<WeatherItem>) -> Unit,
    onError: (String) -> Unit
) {
    RetrofitClient.weatherApi.getWeather()
        .enqueue(object : Callback<WeatherResponse> {
            override fun onResponse(...) {
                // Processing succeeded
            }
            override fun onFailure(...) {
                // Processing failed
            }
        })
}
```

### 2. UI state management

```
@Composable
fun WeatherScreen() {
    var weatherItems by remember { mutableStateOf<List<WeatherItem>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // . ..
}
```

### 3. Temperature conversion

```
fun celsiusToFahrenheit(celsius: Int): Int {
    return (celsius * 9 / 5 + 32)
}
```
