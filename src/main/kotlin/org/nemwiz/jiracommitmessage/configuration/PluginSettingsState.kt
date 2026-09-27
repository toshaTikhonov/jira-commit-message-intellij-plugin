package org.nemwiz.jiracommitmessage.configuration

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "CommitMessageStoredConfiguration", storages = [Storage("CommitMessageStoredConfiguration.xml")])
class PluginSettingsState : PersistentStateComponent<PluginSettingsState.PluginState> {

    var pluginState: PluginState = PluginState()

    override fun getState(): PluginState = pluginState

    override fun loadState(state: PluginState) {
        XmlSerializerUtil.copyBean(state, this.pluginState)
    }

    companion object {
        val instance: PluginSettingsState
            get() = ApplicationManager.getApplication().getService(PluginSettingsState::class.java)
    }

    class PluginState {
        var messageWrapperType = MessageWrapperType.ROUND.type
        var messagePrefixType = PrefixType.NO_PREFIX.type
        var messageInfixType = InfixType.NO_INFIX.type
        var isPrependJiraIssueOnActionClick = false
        var isAutoDetectJiraProjectKey = true
        var isConventionalCommit = false
        var jiraProjectKeys = listOf("FAREPLUS")

        var jiraIssueKey = ""
        var jiraBaseUrl = "https://jira.srvrg.com"
        var jiraUser = "tikhonov"
        var jiraCertificatePath = ""
        var jiraVerifyTls = true
        var jiraJql = "assignee = currentUser() AND resolution = Unresolved ORDER BY updated DESC"
        var publishSvnRevisionToJira = true

        var svnRepositoryUrl = "https://svn.srvrg.com/svn/PaySys"
        var svnUsername = "tikhonov"
    }
}
