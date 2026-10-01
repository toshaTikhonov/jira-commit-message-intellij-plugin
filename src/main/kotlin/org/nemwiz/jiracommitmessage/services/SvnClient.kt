package org.nemwiz.jiracommitmessage.services

import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import java.io.File

data class SvnRevision(
    val revision: Long,
    val author: String,
    val message: String,
    val paths: List<Pair<String, String>>
)

class SvnClient(private val workingCopyPath: String? = null) {
    private var lastSearchDiagnostics: String = ""

    fun committedRevision(
        paths: Collection<String>,
        commitMessage: String
    ): SvnRevision? {
        if (paths.isEmpty()) {
            lastSearchDiagnostics = "no committed paths received from CLion"
            return null
        }

        var lastErrors = emptyList<String>()
        repeat(10) { attempt ->
            val revisions = mutableListOf<Pair<Long, String>>()
            val errors = mutableListOf<String>()

            paths.forEach { path ->
                runCatching {
                    val revisionText = runSvnForTarget(
                        path, "info", "--show-item", "last-changed-revision"
                    ).trim()
                    val revision = revisionText.toLongOrNull()
                        ?: error("unexpected revision: " + revisionText.take(160))
                    revisions += revision to path
                }.onFailure {
                    errors += File(path).name + ": " + (it.message ?: "unknown error")
                }
            }

            val revision = revisions.maxOfOrNull { it.first }
            if (revision != null) {
                val committedPaths = revisions
                    .filter { it.first == revision }
                    .map { (_, path) ->
                        val relativeUrl = runCatching {
                            runSvnForTarget(path, "info", "--show-item", "relative-url")
                                .trim()
                                .removePrefix("^")
                        }.getOrDefault("")
                        "M" to relativeUrl.ifBlank { File(path).name }
                    }

                val author = runCatching {
                    val target = revisions.first { it.first == revision }.second
                    runSvnForTarget(target, "info", "--show-item", "last-changed-author").trim()
                }.getOrDefault("")

                return SvnRevision(
                    revision = revision,
                    author = author,
                    message = commitMessage,
                    paths = committedPaths
                )
            }

            lastErrors = errors
            if (attempt < 9) Thread.sleep(500)
        }

        lastSearchDiagnostics = "svn info failed for " + paths.size + " path(s): " +
            lastErrors.joinToString("; ").take(1000)
        return null
    }

    fun searchDiagnostics(): String = lastSearchDiagnostics

    fun jiraComment(revision: SvnRevision): String {
        val state = PluginSettingsState.instance.state
        val repositoryRoot = runCatching {
            runSvn("info", "--show-item", "repos-root-url").trim().trimEnd('/')
        }.getOrDefault("")
        val repositoryName = repositoryRoot.substringAfterLast('/').ifBlank { "SVN" }

        // VisualSVN web UI normally lives on the same host as the repository URL.
        // The working copy remains the source of truth; nothing environment-specific
        // is stored in plugin defaults.
        val visualSvnBase = normalizeHttpUrl(
            state.visualSvnWebUrl.trim().trimEnd('/').ifBlank {
                runCatching {
                    val uri = java.net.URI(repositoryRoot)
                    uri.scheme + "://" + uri.authority
                }.getOrDefault("")
            }
        )

        fun visualRevisionUrl(): String =
            if (visualSvnBase.isBlank()) "" else
                visualSvnBase + "/!/#" + repositoryName + "/commit/r" + revision.revision + "/"

        // VisualSVN's stable pathrevision URL opens the file contents, not its diff.
        // Commit Details is the stable page that contains the changed-path list and
        // opens the inline diff for a modified file, so changed files link there.
        fun visualPathUrl(): String = visualRevisionUrl()

        fun jiraIssueLink(message: String): String {
            val issue = Regex("[A-Z][A-Z0-9]+-\\d+").find(message)?.value ?: return message
            val jiraBase = state.jiraBaseUrl.trimEnd('/')
            if (jiraBase.isBlank()) return message
            return message.replaceFirst(
                issue,
                "[" + issue + "|" + jiraBase + "/browse/" + issue + "]"
            )
        }

        val revisionUrl = visualRevisionUrl()
        val lines = mutableListOf<String>()
        lines += if (revisionUrl.isBlank()) {
            "SVN revision r" + revision.revision
        } else {
            "[SVN revision r" + revision.revision + "|" + revisionUrl + "]"
        }
        lines += "Репозиторий: " + repositoryName
        if (revision.author.isNotBlank()) lines += "Автор: " + revision.author
        lines += ""
        lines += jiraIssueLink(revision.message.ifBlank { "(без комментария)" })
        lines += ""
        lines += "Изменено файлов: " + revision.paths.size
        revision.paths.take(30).forEach { (action, path) ->
            val url = visualPathUrl()
            lines += if (url.isBlank()) {
                action + " " + path
            } else {
                action + " [" + path + "|" + url + "]"
            }
        }
        if (revision.paths.size > 30) lines += "... и ещё " + (revision.paths.size - 30)
        return lines.joinToString("\n")
    }

    private fun normalizeHttpUrl(value: String): String =
        value
            .replace(Regex("^https:/+(?!/)"), "https://")
            .replace(Regex("^http:/+(?!/)"), "http://")

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

    fun repositoryUrl(): String {
        val xml = runSvn("info", "--xml")
        return Regex("<url>([\\s\\S]*?)</url>")
            .find(xml)
            ?.groupValues
            ?.get(1)
            ?.let(::unescapeXml)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?: error("Cannot determine SVN repository URL from working copy")
    }

    private fun svnTarget(): String =
        workingCopyPath?.takeIf { it.isNotBlank() }
            ?: error("SVN working copy is not available")

    private fun runSvn(vararg args: String): String = runSvnForTarget(svnTarget(), *args)

    private fun runSvnForTarget(target: String, vararg args: String): String {
        val command = mutableListOf(findSvnExecutable())
        command += args
        command += target
        command += "--non-interactive"

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
