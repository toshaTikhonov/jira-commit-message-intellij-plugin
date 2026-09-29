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
        val keyPattern = Regex("\\\"key\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"")
        val summaryPattern = Regex("\\\"summary\\\"\\s*:\\s*\\\"([^\\\"]*)\\\"")
        val keys = keyPattern.findAll(response).map { it.groupValues[1] }.toList()
        val summaries = summaryPattern.findAll(response).map { unescape(it.groupValues[1]) }.toList()
        return keys.zip(summaries).map { JiraIssue(it.first, it.second) }
    }

    fun testConnection(): String {
        val sslStatus = if (state.jiraCertificatePath.isBlank()) {
            "PKCS#12: not configured"
        } else {
            val diagnostics = inspectPkcs12()
            createSslContext()
            "PKCS#12: OK (" + diagnostics + ")"
        }
        request("GET", "/rest/api/2/myself")
        return sslStatus + "\nTLS: OK\nJira authentication: OK"
    }

    fun addComment(issueKey: String, comment: String) {
        val quote = 34.toChar().toString()
        val json = "{" + quote + "body" + quote + ":" + quote + escape(comment) + quote + "}"
        request("POST", "/rest/api/2/issue/" + issueKey + "/comment", json)
    }

    private fun request(method: String, path: String, requestBody: String? = null): String {
        val connection = URI(state.jiraBaseUrl.trimEnd('/') + path).toURL().openConnection() as HttpURLConnection
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

    private fun loadPkcs12(): Pair<KeyStore, CharArray> {
        val file = File(state.jiraCertificatePath).absoluteFile
        require(file.isFile) { "PKCS#12 file not found: " + file.absolutePath }
        require(file.canRead()) { "PKCS#12 file is not readable: " + file.absolutePath }

        val password = CredentialService.jiraCertificatePassword.toCharArray()
        val keyStore = KeyStore.getInstance("PKCS12")
        try {
            file.inputStream().use { keyStore.load(it, password) }
        } catch (e: Exception) {
            throw IllegalStateException(
                "PKCS#12 load failed\n" +
                    "file: " + file.absolutePath + "\n" +
                    "size: " + file.length() + " bytes\n" +
                    "password length: " + password.size + "\n" +
                    "stage: KeyStore.load\n" +
                    "cause: " + (e.cause?.message ?: e.message ?: e.javaClass.simpleName),
                e
            )
        }
        return keyStore to password
    }

    private fun inspectPkcs12(): String {
        val (keyStore, _) = loadPkcs12()
        val aliases = keyStore.aliases().toList()
        val privateKeys = aliases.count { keyStore.isKeyEntry(it) }
        require(aliases.isNotEmpty()) { "PKCS#12 loaded, but contains no aliases" }
        require(privateKeys > 0) { "PKCS#12 loaded, but contains no private key entry" }
        return "aliases=" + aliases.size + ", private keys=" + privateKeys
    }

    private fun createSslContext(): SSLContext {
        val (keyStore, password) = loadPkcs12()
        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm())
        try {
            kmf.init(keyStore, password)
        } catch (e: Exception) {
            throw IllegalStateException(
                "PKCS#12 key initialization failed\n" +
                    "stage: KeyManagerFactory.init\n" +
                    "cause: " + (e.cause?.message ?: e.message ?: e.javaClass.simpleName),
                e
            )
        }
        return try {
            SSLContext.getInstance("TLS").apply { init(kmf.keyManagers, null, null) }
        } catch (e: Exception) {
            throw IllegalStateException(
                "TLS initialization failed\n" +
                    "stage: SSLContext.init\n" +
                    "cause: " + (e.cause?.message ?: e.message ?: e.javaClass.simpleName),
                e
            )
        }
    }

    private fun escape(value: String): String {
        val slash = 92.toChar()
        val quote = 34.toChar()
        return buildString {
            value.forEach {
                when (it) {
                    slash -> append(slash).append(slash)
                    quote -> append(slash).append(quote)
                    '\n' -> append(slash).append('n')
                    '\r' -> append(slash).append('r')
                    else -> append(it)
                }
            }
        }
    }

    private fun unescape(value: String): String =
        value.replace("\\n", "\n").replace("\\\"", "\"")
}
