package com.kotlin.mcp.server

import org.springframework.ai.tool.ToolCallbackProvider
import org.springframework.ai.tool.method.MethodToolCallbackProvider
import org.springframework.boot.SpringApplication
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.context.annotation.Bean


@SpringBootApplication
class McpApplication {
    @Bean
    fun weatherTools(weatherService: WeatherService?): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder().toolObjects(weatherService).build()
    }

    @Bean
    fun askApiTools(askApiTool: AskApiTool): ToolCallbackProvider {
        return MethodToolCallbackProvider.builder().toolObjects(askApiTool).build()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            SpringApplication.run(McpApplication::class.java, *args)
        }
    }
}
