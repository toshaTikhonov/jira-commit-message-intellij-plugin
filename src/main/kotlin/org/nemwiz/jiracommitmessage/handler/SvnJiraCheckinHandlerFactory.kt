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
        // This factory is loaded only when the bundled Subversion plugin is present.
        // Do not use vcsIsAffected("svn") here: the VCS display/key name differs between
        // IDE versions and caused the handler to be silently replaced with DUMMY.
        return object : CheckinHandler(), DumbAware {
            private var committedPaths: List<String> = emptyList()

            override fun beforeCheckin(): ReturnResult {
                if (PluginSettingsState.instance.state.publishSvnRevisionToJira) {
                    committedPaths = panel.selectedChanges.mapNotNull { change ->
                        change.afterRevision?.file?.path ?: change.beforeRevision?.file?.path
                    }.distinct()
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
                ApplicationManager.getApplication().executeOnPooledThread {
                    runCatching {
                        val svn = SvnClient(panel.project.basePath)
                        val committed = svn.committedRevision(committedPaths)
                            ?: error("SVN revision not found in committed files (" + committedPaths.size + " paths)")
                        JiraClient().addComment(issueKey, svn.jiraComment(committed))
                        PluginNotifier().showInfo(
                            panel.project,
                            "JIRA SVN Commit Message",
                            "SVN revision r" + committed.revision + " опубликована в " + issueKey
                        )
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
