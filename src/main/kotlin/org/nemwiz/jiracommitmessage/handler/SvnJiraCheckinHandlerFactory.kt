package org.nemwiz.jiracommitmessage.handler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.vcs.CheckinProjectPanel
import com.intellij.openapi.vcs.changes.CommitContext
import com.intellij.openapi.vcs.checkin.CheckinHandler
import com.intellij.openapi.vcs.checkin.CheckinHandlerFactory
import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import org.nemwiz.jiracommitmessage.provider.PluginNotifier
import org.nemwiz.jiracommitmessage.services.JiraClient
import org.nemwiz.jiracommitmessage.services.JiraCommitMessagePlugin
import org.nemwiz.jiracommitmessage.services.SvnClient

private val LOG = logger<SvnJiraCheckinHandlerFactory>()

class SvnJiraCheckinHandlerFactory : CheckinHandlerFactory() {
    override fun createHandler(panel: CheckinProjectPanel, commitContext: CommitContext): CheckinHandler {
        if (!panel.vcsIsAffected("svn")) return CheckinHandler.DUMMY

        return object : CheckinHandler(), DumbAware {
            private var beforeRevision: Long? = null

            override fun beforeCheckin(): ReturnResult {
                if (PluginSettingsState.instance.state.publishSvnRevisionToJira) {
                    beforeRevision = runCatching { SvnClient().headRevision() }.getOrNull()
                }
                return ReturnResult.COMMIT
            }

            override fun checkinSuccessful() {
                val state = PluginSettingsState.instance.state
                if (!state.publishSvnRevisionToJira) return

                val message = panel.commitMessage
                val issueKey = panel.project.service<JiraCommitMessagePlugin>().extractIssueKey(message)
                    ?: state.jiraIssueKey.trim().takeIf { it.isNotEmpty() }
                    ?: return
                val oldRevision = beforeRevision ?: return

                ApplicationManager.getApplication().executeOnPooledThread {
                    runCatching {
                        val svn = SvnClient()
                        var revision = svn.findCommittedRevision(oldRevision, message)
                        repeat(4) {
                            if (revision != null) return@repeat
                            Thread.sleep(500)
                            revision = svn.findCommittedRevision(oldRevision, message)
                        }
                        val committed = revision ?: error("SVN revision not found after successful commit")
                        JiraClient().addComment(issueKey, svn.jiraComment(committed))
                    }.onFailure {
                        LOG.warn("Cannot publish SVN revision to Jira", it)
                        PluginNotifier().showWarning(
                            panel.project,
                            "JIRA SVN Commit Message",
                            "SVN commit выполнен, но revision не удалось опубликовать в Jira: " + (it.message ?: "unknown error"),
                            null
                        )
                    }
                }
            }
        }
    }
}
