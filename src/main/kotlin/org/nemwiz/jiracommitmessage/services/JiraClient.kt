package org.nemwiz.jiracommitmessage.services

import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext

data class JiraIssue(val key: String, val summary: String) {
    override fun toString(): String = key + " — " + summary
}

class JiraClient {
    private val state get() = PluginSettingsState.instance.state

    fun searchIssues(): List<JiraIssue> {
        val jql = URLEncoder.encode(state.jiraJql, StandardCharsets.UTF_8)
        val response = request("GET", "/rest/api/2/search?jql=" + jql + "&maxResults=50&fields=key,summary")
        val issueRegex = Regex("\\\"key\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"[\\s\\S]*?\\\"summary\\\"\\s*:\\s*\\\"((?:\\\\\\.|[^\\\"])*)\\\"")
        return issueRegex.findAll(response).map {
            JiraIssue(it.groupValues[1], unescape(it.groupValues[2]))
        }.toList()
    }

    fun addComment(issueKey: String, comment: String) {
        request("POST", "/rest/api/2/issue/" + issueKey + "/comment", "{\\\"body\\\":\\\"" + escape(comment) + "\\\"}")
    }

    private fun request(method: String, path: String, requestBody: String? = null): String {
        val url = URI(state.jiraBaseUrl.trimEnd('/') + path).toURL()
        val connection = url.openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection && state.jiraCertificatePath.isNotBlank()) {
            connection.sslSocketFactory = createSslContext().socketFactory
        }
        connection.requestMethod = method
        connection.connectTimeout = 15000
        connection.readTimeout = 60000
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "application/json")
        val authSource = state.jiraUser + ":" + CredentialService.jiraPassword
        val auth = java.util.Base64.getEncoder().encodeToString(authSource.toByteArray(StandardCharsets.UTF_8))
        connection.setRequestProperty("Authorization", "Basic " + auth)

        if (requestBody != null) {
            connection.doOutput = true
            connection.outputStream.use { it.write(requestBody.toByteArray(StandardCharsets.UTF_8)) }
        }

        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        val text = stream?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
        if (code !in 200..299) throw IllegalStateException("Jira HTTP " + code + ": " + text)
        return text
    }

    private fun createSslContext(): SSLContext {
        val file = File(state.jiraCertificatePath)
        require(file.isFile) { "JIRA certificate not found: " + file.absolutePath }
        val password = CredentialService.jiraCertificatePassword.toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12")
        file.inputStream().use { keyStore.load(it, password) }
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        kmf.init(keyStore, password)
        return SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, null) }
    }

    private fun escape(value: String): String =
        value.replace("\\\\", "\\\\\\\\").replace("\\\"", "\\\\\\"").replace("\\n", "\\\\n").replace("\\r", "\\\\r")

    private fun unescape(value: String): String =
        value.replace("\\\\\\"", "\\\"").replace("\\\\n", "\\n").replace("\\\\\\\\", "\\\\")
}
