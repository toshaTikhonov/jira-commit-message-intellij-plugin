package org.nemwiz.jiracommitmessage.services

import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import java.io.File

data class SvnRevision(
    val revision: Long,
    val author: String,
    val message: String,
    val paths: List<Pair<String, String>>
)

class SvnClient {
    private val state get() = PluginSettingsState.instance.state

    fun findCommittedRevision(previousRevision: Long, expectedMessage: String): SvnRevision? {
        val head = headRevision()
        if (head <= previousRevision) return null

        val from = maxOf(previousRevision + 1, head - 10)
        val xml = runSvn("log", "--xml", "-v", "-r", head.toString() + ":" + from)
        val entries = Regex("<logentry revision=\\\"(\\d+)\\\">([\\s\\S]*?)</logentry>")
        return entries.findAll(xml).map { match ->
            val body = match.groupValues[2]
            SvnRevision(
                match.groupValues[1].toLong(),
                tag(body, "author"),
                unescapeXml(tag(body, "msg")),
                Regex("<path[^>]*action=\\\"([^\\\"]+)\\\"[^>]*>(.*?)</path>")
                    .findAll(body)
                    .map { it.groupValues[1] to unescapeXml(it.groupValues[2]) }
                    .toList()
            )
        }.firstOrNull { it.message.trim() == expectedMessage.trim() }
    }

    fun headRevision(): Long {
        val xml = runSvn("info", "--xml")
        return Regex("<entry[^>]*revision=\\\"(\\d+)\\\"").find(xml)?.groupValues?.get(1)?.toLong()
            ?: error("Cannot read SVN HEAD revision")
    }

    fun jiraComment(revision: SvnRevision): String {
        val repo = state.svnRepositoryUrl.trimEnd('/')
        val revisionUrl = repo + "/?p=" + revision.revision
        val lines = mutableListOf(
            "[SVN revision r" + revision.revision + "|" + revisionUrl + "]",
            "Репозиторий: " + repo,
            "Автор: " + revision.author,
            "",
            revision.message.ifBlank { "(без комментария)" },
            "",
            "Изменено файлов: " + revision.paths.size
        )
        revision.paths.take(30).forEach { (action, path) -> lines += action + " " + path }
        if (revision.paths.size > 30) lines += "... и ещё " + (revision.paths.size - 30)
        return lines.joinToString("\\n")
    }

    private fun findSvnExecutable(): String {
        val pathCandidates = System.getenv("PATH")
            .orEmpty()
            .split(File.pathSeparator)
            .filter { it.isNotBlank() }
            .map { File(it, "svn") }

        val candidates = pathCandidates + listOf(
            File("/opt/homebrew/bin/svn"),
            File("/usr/local/bin/svn"),
            File("/usr/bin/svn")
        )

        return candidates
            .distinctBy { it.absolutePath }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
            ?: error(
                "SVN executable not found. Checked PATH and: " +
                    "/opt/homebrew/bin/svn, /usr/local/bin/svn, /usr/bin/svn"
            )
    }

    private fun runSvn(vararg args: String): String {
        val command = mutableListOf(findSvnExecutable())
        command += args
        command += state.svnRepositoryUrl
        command += "--non-interactive"
        if (state.svnUsername.isNotBlank()) command += listOf("--username", state.svnUsername)
        if (CredentialService.svnPassword.isNotBlank()) command += listOf("--password", CredentialService.svnPassword)

        val process = ProcessBuilder(command).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().use { it.readText() }
        if (process.waitFor() != 0) error(output.trim())
        return output
    }

    private fun tag(xml: String, name: String): String =
        Regex("<" + name + ">([\\s\\S]*?)</" + name + ">").find(xml)?.groupValues?.get(1).orEmpty()

    private fun unescapeXml(value: String): String =
        value.replace("&lt;", "<").replace("&gt;", ">").replace("&quot;", "\"").replace("&amp;", "&")
}
