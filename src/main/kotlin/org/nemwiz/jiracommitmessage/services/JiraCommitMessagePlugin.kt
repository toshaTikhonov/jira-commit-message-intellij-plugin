package org.nemwiz.jiracommitmessage.services

import com.intellij.notification.BrowseNotificationAction
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import org.nemwiz.jiracommitmessage.provider.PluginNotifier
import java.util.Locale
import java.util.regex.Pattern

private val LOG = logger<JiraCommitMessagePlugin>()

private const val DEFAULT_REGEX_FOR_JIRA_PROJECT_ISSUES = "([A-Z]+[_-][0-9]+)"

@Service(Service.Level.PROJECT)
class JiraCommitMessagePlugin(private val project: Project) : Disposable {

    fun getCommitMessage(): String {
        val state = PluginSettingsState.instance.state
        val configuredIssue = state.jiraIssueKey.trim()

        if (configuredIssue.isNotEmpty()) {
            return buildCommitMessage(configuredIssue.uppercase(Locale.getDefault()))
        }

        if (state.jiraProjectKeys.isEmpty() && !state.isAutoDetectJiraProjectKey) {
            PluginNotifier().showWarning(
                project,
                "Missing configuration",
                "Configure a JIRA issue key under Settings > Tools > JIRA SVN Commit Message",
                BrowseNotificationAction(
                    "Open repository",
                    "https://github.com/toshaTikhonov/jira-commit-message-intellij-plugin"
                )
            )
            return ""
        }

        return ""
    }

    fun getCommitMessageFromText(text: String?): String {
        if (text.isNullOrBlank()) return getCommitMessage()

        val state = PluginSettingsState.instance.state
        val jiraIssue = extractJiraIssue(state.isAutoDetectJiraProjectKey, text, state.jiraProjectKeys)
            ?: state.jiraIssueKey.trim().takeIf { it.isNotEmpty() }

        return jiraIssue?.let { buildCommitMessage(it.uppercase(Locale.getDefault())) }.orEmpty()
    }

    private fun buildCommitMessage(jiraIssue: String): String {
        val state = PluginSettingsState.instance.state
        return CommitMessageBuilder(jiraIssue)
            .withWrapper(state.messageWrapperType)
            .withInfix(state.messageInfixType)
            .withPrefix(state.messagePrefixType)
            .getCommitMessage()
    }

    fun extractIssueKey(source: String): String? {\n        val state = PluginSettingsState.instance.state\n        return extractJiraIssue(state.isAutoDetectJiraProjectKey, source, state.jiraProjectKeys)\n    }\n\n    private fun extractJiraIssue(
        isAutoDetectProjectKey: Boolean,
        source: String,
        jiraProjectKeys: List<String>
    ): String? {
        if (isAutoDetectProjectKey) {
            return Pattern.compile(DEFAULT_REGEX_FOR_JIRA_PROJECT_ISSUES)
                .toRegex()
                .find(source)
                ?.value
        }

        for (projectKey in jiraProjectKeys) {
            val pattern = Pattern.compile(String.format(Locale.US, "%s+[_-][0-9]+", projectKey)).toRegex()
            val match = pattern.find(source)
                ?: Pattern.compile(String.format(Locale.US, "%s+[_-][0-9]+", projectKey.lowercase())).toRegex().find(source)

            if (match != null) return match.value.uppercase(Locale.getDefault())
        }

        return null
    }

    override fun dispose() = Unit
}
