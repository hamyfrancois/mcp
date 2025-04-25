package com.kotlin.mcp.server

import org.springframework.ai.tool.annotation.Tool
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.stereotype.Component
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

@Component
class AskApiTool {

    private val swaggerUrl = "http://localhost:8080/api/v3/api-docs"
    private val mapper = jacksonObjectMapper()

    private val apiKeyHeader = "X-API-KEY"
    private val apiKeyValue = "API_KEY_EXAMPLE"

    @Tool(
        name = "askApi",
        description = "Pose une question à l’API REST documentée par Swagger. Le modèle doit analyser la doc Swagger et choisir dynamiquement un endpoint et ses paramètres."
    )
    fun askApi(
        @Schema(description = "Question utilisateur en langage naturel") question: String
    ): String {
        val swagger = fetchSwagger()
        val matchingPath = guessPathFromQuestion(swagger, question) ?: return "Aucun endpoint ne correspond à la question."

        val url = buildUrlFromPath(matchingPath.path, matchingPath.queryParams)
        val curlCommand = listOf(
            "curl", "-s", "-X", matchingPath.method,
            url,
            "-H", "accept: */*",
            "-H", "$apiKeyHeader: $apiKeyValue"
        )

        val process = ProcessBuilder(curlCommand).redirectErrorStream(true).start()
        val output = BufferedReader(InputStreamReader(process.inputStream)).readText()
        process.waitFor()
        return output
    }

    data class Endpoint(val path: String, val method: String, val description: String, val queryParams: Map<String, String>)

    private fun fetchSwagger(): JsonNode {
        val connection = URL(swaggerUrl).openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.setRequestProperty("accept", "application/json")
        return mapper.readTree(connection.inputStream)
    }

    private fun guessPathFromQuestion(swagger: JsonNode, question: String): Endpoint? {
        val paths = swagger["paths"] ?: return null

        return paths.fields().asSequence()
            .flatMap { (path, methods) ->
                methods.fields().asSequence().map { (method, op) ->
                    val summary = op["summary"]?.asText() ?: ""
                    val description = op["description"]?.asText() ?: ""
                    val fullText = "$path $method $summary $description".lowercase()

                    val score = question.lowercase().split(" ").count { it in fullText }
                    val queryParams = extractDefaultQueryParams(op)

                    Endpoint(path, method.uppercase(), description, queryParams) to score
                }
            }
            .maxByOrNull { it.second }
            ?.first
    }

    private fun extractDefaultQueryParams(op: JsonNode): Map<String, String> {
        return op["parameters"]?.associate {
            val name = it["name"].asText()
            val defaultValue = it["example"]?.asText()
                ?: it["schema"]?.get("default")?.asText()
                ?: when (it["schema"]?.get("type")?.asText()) {
                    "string" -> "example"
                    "integer" -> "0"
                    else -> ""
                }
            name to defaultValue
        } ?: emptyMap()
    }

    private fun buildUrlFromPath(path: String, queryParams: Map<String, String>): String {
        val query = queryParams.entries.joinToString("&") { "${it.key}=${it.value}" }
        return if (query.isNotBlank()) {
            "http://localhost:8080/api$path?$query"
        } else {
            "http://localhost:8080/api$path"
        }
    }
}