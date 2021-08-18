package com.github.njuro.medicquiz

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule
import nz.net.ultraq.thymeleaf.LayoutDialect
import nz.net.ultraq.thymeleaf.decorators.strategies.GroupingRespectLayoutTitleStrategy
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.nio.file.Path
import java.nio.file.Paths

@Configuration
class Config : WebMvcConfigurer {

    companion object {
        val resourcesPath: Path = Paths.get("src", "main", "resources")
        val imageMappingsPath: Path = resourcesPath.resolve("image-mappings.yml")
        val subjectMappingsPath: Path = resourcesPath.resolve("subject-mappings.yml")
        val questionsPath: Path = resourcesPath.resolve("questions.yml")
        val solutionsPath: Path = resourcesPath.resolve("solutions.yml")
        val testsPath: Path = resourcesPath.resolve("tests.yml")
        val tokensPath: Path = resourcesPath.resolve("tokens.yml")
        val sessionsPath: Path = resourcesPath.resolve("sessions.yml")
    }

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        registry.addResourceHandler("/static/" + "**")
            .addResourceLocations(resourcesPath.resolve("static").toUri().toString())
    }

    @Bean
    fun objectMapper(): ObjectMapper {
        return ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER))
            .registerModule(ParameterNamesModule(JsonCreator.Mode.PROPERTIES))
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .apply { findAndRegisterModules() }
    }

    @Bean
    fun layoutDialect(): LayoutDialect {
        // grouping strategy groups same elements in <head>, such as <script> or <link> together when merging
        return LayoutDialect(GroupingRespectLayoutTitleStrategy())
    }
}