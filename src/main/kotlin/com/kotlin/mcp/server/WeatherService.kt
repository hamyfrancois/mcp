package com.kotlin.mcp.server

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.ai.tool.annotation.Tool
import org.springframework.ai.tool.annotation.ToolParam
import org.springframework.stereotype.Service
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import java.util.stream.Collectors

@Service
class WeatherService {
    private final val restClient: RestClient = RestClient.builder()
        .baseUrl(BASE_URL)
        .defaultHeader("Accept", "application/geo+json")
        .defaultHeader("User-Agent", "WeatherApiClient/1.0 (your@email.com)")
        .build()

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JvmRecord
    data class Points(
        @field:JsonProperty("properties") @param:JsonProperty(
            "properties"
        ) val properties: Props
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JvmRecord
        data class Props(
            @field:JsonProperty("forecast") @param:JsonProperty(
                "forecast"
            ) val forecast: String
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JvmRecord
    data class Forecast(
        @field:JsonProperty("properties") @param:JsonProperty(
            "properties"
        ) val properties: Props
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JvmRecord
        data class Props(
            @field:JsonProperty("periods") @param:JsonProperty(
                "periods"
            ) val periods: List<Period>
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JvmRecord
        data class Period(
            @field:JsonProperty("number") @param:JsonProperty(
                "number"
            ) val number: Int, @field:JsonProperty("name") @param:JsonProperty(
                "name"
            ) val name: String,
            @field:JsonProperty("startTime") @param:JsonProperty(
                "startTime"
            ) val startTime: String, @field:JsonProperty("endTime") @param:JsonProperty(
                "endTime"
            ) val endTime: String,
            @field:JsonProperty("isDaytime") @param:JsonProperty(
                "isDaytime"
            ) val isDayTime: Boolean, @field:JsonProperty("temperature") @param:JsonProperty(
                "temperature"
            ) val temperature: Int,
            @field:JsonProperty("temperatureUnit") @param:JsonProperty(
                "temperatureUnit"
            ) val temperatureUnit: String,
            @field:JsonProperty("temperatureTrend") @param:JsonProperty(
                "temperatureTrend"
            ) val temperatureTrend: String,
            @field:JsonProperty("probabilityOfPrecipitation") @param:JsonProperty(
                "probabilityOfPrecipitation"
            ) val probabilityOfPrecipitation: Map<*, *>,
            @field:JsonProperty("windSpeed") @param:JsonProperty(
                "windSpeed"
            ) val windSpeed: String, @field:JsonProperty("windDirection") @param:JsonProperty(
                "windDirection"
            ) val windDirection: String,
            @field:JsonProperty("icon") @param:JsonProperty(
                "icon"
            ) val icon: String, @field:JsonProperty("shortForecast") @param:JsonProperty(
                "shortForecast"
            ) val shortForecast: String,
            @field:JsonProperty("detailedForecast") @param:JsonProperty(
                "detailedForecast"
            ) val detailedForecast: String
        )
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    @JvmRecord
    data class Alert(
        @field:JsonProperty("features") @param:JsonProperty(
            "features"
        ) val features: List<Feature>
    ) {
        @JsonIgnoreProperties(ignoreUnknown = true)
        @JvmRecord
        data class Feature(
            @field:JsonProperty("properties") @param:JsonProperty(
                "properties"
            ) val properties: Properties
        )

        @JsonIgnoreProperties(ignoreUnknown = true)
        @JvmRecord
        data class Properties(
            @field:JsonProperty("event") @param:JsonProperty(
                "event"
            ) val event: String, @field:JsonProperty("areaDesc") @param:JsonProperty(
                "areaDesc"
            ) val areaDesc: String,
            @field:JsonProperty("severity") @param:JsonProperty(
                "severity"
            ) val severity: String, @field:JsonProperty("description") @param:JsonProperty(
                "description"
            ) val description: String,
            @field:JsonProperty("instruction") @param:JsonProperty(
                "instruction"
            ) val instruction: String
        )
    }

    /**
     * Get forecast for a specific latitude/longitude
     * @param latitude Latitude
     * @param longitude Longitude
     * @return The forecast for the given location
     * @throws RestClientException if the request fails
     */
    @Tool(description = "Get weather forecast for a specific latitude/longitude")
    fun getWeatherForecastByLocation(latitude: Double, longitude: Double): String? {
        val points: Points? = restClient.get()
            .uri("/points/{latitude},{longitude}", latitude, longitude)
            .retrieve()
            .body(Points::class.java)

        val forecast =
            points?.properties?.forecast?.let { restClient.get().uri(it).retrieve().body(Forecast::class.java) }

        val forecastText = forecast?.properties?.periods?.stream()?.map { p ->
            java.lang.String.format(
                """
					%s:
					Temperature: %s %s
					Wind: %s %s
					Forecast: %s
					
					""".trimIndent(), p.name, p.temperature, p.temperatureUnit, p.windSpeed, p.windDirection,
                p.detailedForecast
            )
        }?.collect(Collectors.joining())

        return forecastText
    }

    /**
     * Get alerts for a specific area
     * @param state Area code. Two-letter US state code (e.g. CA, NY)
     * @return Human readable alert information
     * @throws RestClientException if the request fails
     */
    @Tool(description = "Get weather alerts for a US state. Input is Two-letter US state code (e.g. CA, NY)")
    fun getAlerts(@ToolParam(description = "Two-letter US state code (e.g. CA, NY") state: String?): String? {
        val alert: Alert? =
            restClient.get().uri("/alerts/active/area/{state}", state).retrieve().body(Alert::class.java)

        return alert?.features
            ?.stream()
            ?.map { f: Alert.Feature ->
                String.format(
                    """
					Event: %s
					Area: %s
					Severity: %s
					Description: %s
					Instructions: %s
					
					""".trimIndent(), f.properties.event, f.properties.areaDesc, f.properties.severity,
                    f.properties.description, f.properties.instruction
                )
            }
            ?.collect(Collectors.joining("\n"))
    }

    companion object {
        private const val BASE_URL = "https://api.weather.gov"
    }
}