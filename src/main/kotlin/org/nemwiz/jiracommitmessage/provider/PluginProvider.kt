package org.nemwiz.jiracommitmessage.provider

import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.changes.LocalChangeList
import com.intellij.openapi.vcs.changes.ui.CommitMessageProvider
import org.nemwiz.jiracommitmessage.services.JiraCommitMessagePlugin

private val LOG = logger<PluginProvider>()

class PluginProvider : CommitMessageProvider {
    override fun getCommitMessage(forChangelist: LocalChangeList, project: Project): String? {
        val oldCommitMessage = forChangelist.comment
        val plugin = project.service<JiraCommitMessagePlugin>()
        val generated = plugin.getCommitMessage()

        LOG.info("JIRA SVN commit message provider generated -> $generated")

        return generated.ifBlank { oldCommitMessage }
    }
}
