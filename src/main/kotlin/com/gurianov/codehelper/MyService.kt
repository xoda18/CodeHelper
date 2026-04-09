package com.gurianov.codehelper

import com.google.gson.Gson
import com.google.gson.JsonObject
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import java.io.File
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import javax.swing.SwingUtilities

@Service(Service.Level.PROJECT)
class MyService(val project: Project) {

    var selectedCode: String = ""
    private val gson = Gson()
    private val httpClient = HttpClient.newHttpClient()

    private val codeListeners = mutableListOf<(String) -> Unit>()
    private val responseListeners = mutableListOf<(String) -> Unit>()

    fun addCodeListener(listener: (String) -> Unit) { codeListeners.add(listener) }
    fun addResponseListener(listener: (String) -> Unit) { responseListeners.add(listener) }

    private fun post(endpoint: String, body: Map<String, Any>) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:8080/$endpoint"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(body)))
                    .build()

                val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())
                val result = gson.fromJson(response.body(), JsonObject::class.java)
                    .get("result").asString

                SwingUtilities.invokeLater { responseListeners.forEach { it(result) } }
            } catch (e: Exception) {
                SwingUtilities.invokeLater { responseListeners.forEach { it("Error: ${e.message}") } }
            }
        }
    }

    private fun collectProjectFiles(): List<Map<String, String>> {
        val projectDir = File(project.basePath ?: return emptyList())
        val skipDirs = setOf("build", ".gradle", ".idea", "gradle", "out")

        return projectDir.walkTopDown()
            .onEnter { dir -> dir.name !in skipDirs }
            .filter { it.isFile && it.extension in listOf("kt", "java", "xml", "py") }
            .map { mapOf("path" to it.relativeTo(projectDir).path, "content" to it.readText()) }
            .toList()
    }

    fun analyzeCode(code: String) {
        selectedCode = code
        codeListeners.forEach { it(code) }
        post("analyze", mapOf("code" to code, "project_files" to collectProjectFiles()))
    }

    fun sendChatMessage(text: String) {
        post("chat", mapOf("message" to text))
    }
}
