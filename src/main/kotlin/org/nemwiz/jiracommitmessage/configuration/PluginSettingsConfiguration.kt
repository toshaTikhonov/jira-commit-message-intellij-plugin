package org.nemwiz.jiracommitmessage.configuration

import com.intellij.openapi.options.Configurable
import com.intellij.ui.CollectionListModel
import javax.swing.JComponent

class PluginSettingsConfiguration : Configurable {
    private lateinit var panel: PluginSettingsConfigurationPanel
    private val state get() = PluginSettingsState.instance.state

    override fun createComponent(): JComponent {
        panel = PluginSettingsConfigurationPanel()
        reset()
        return panel.mainPanel
    }

    override fun isModified(): Boolean =
        panel.currentIssueKey() != state.jiraIssueKey ||
            panel.jiraBaseUrlField.text != state.jiraBaseUrl ||
            panel.jiraUserField.text != state.jiraUser ||
            panel.jiraCertificatePathField.text != state.jiraCertificatePath ||
            panel.jiraJqlField.text != state.jiraJql ||
            panel.publishSvnRevisionCheckbox.isSelected != state.publishSvnRevisionToJira ||
            panel.svnRepositoryUrlField.text != state.svnRepositoryUrl ||
            panel.svnUsernameField.text != state.svnUsername ||
            panel.messageWrapperTypeDropdown.selectedItem != state.messageWrapperType ||
            panel.prefixTypeDropdown.selectedItem != state.messagePrefixType ||
            panel.infixTypeDropdown.selectedItem != state.messageInfixType ||
            panel.prependJiraIssueOnPluginActionClickCheckbox.isSelected != state.isPrependJiraIssueOnActionClick ||
            panel.isAutoDetectJiraProjectKeyCheckbox.isSelected != state.isAutoDetectJiraProjectKey ||
            setOf(panel.projectKeysModel.items) != setOf(state.jiraProjectKeys)

    override fun apply() {
        state.jiraIssueKey = panel.currentIssueKey()
        state.jiraBaseUrl = panel.jiraBaseUrlField.text.trim()
        state.jiraUser = panel.jiraUserField.text.trim()
        state.jiraCertificatePath = panel.jiraCertificatePathField.text.trim()
        state.jiraJql = panel.jiraJqlField.text.trim()
        state.publishSvnRevisionToJira = panel.publishSvnRevisionCheckbox.isSelected
        state.svnRepositoryUrl = panel.svnRepositoryUrlField.text.trim()
        state.svnUsername = panel.svnUsernameField.text.trim()
        state.messageWrapperType = panel.messageWrapperTypeDropdown.selectedItem.toString()
        state.messagePrefixType = panel.prefixTypeDropdown.selectedItem.toString()
        state.messageInfixType = panel.infixTypeDropdown.selectedItem.toString()
        state.isPrependJiraIssueOnActionClick = panel.prependJiraIssueOnPluginActionClickCheckbox.isSelected
        state.isAutoDetectJiraProjectKey = panel.isAutoDetectJiraProjectKeyCheckbox.isSelected
        state.jiraProjectKeys = panel.projectKeysModel.items
        panel.savePasswords()
    }

    override fun getDisplayName(): String = "JIRA SVN Commit Message"
    override fun getPreferredFocusedComponent(): JComponent = panel.getPreferredFocusedComponent()

    override fun reset() {
        panel.jiraIssueCombo.selectedItem = state.jiraIssueKey
        panel.jiraBaseUrlField.text = state.jiraBaseUrl
        panel.jiraUserField.text = state.jiraUser
        panel.jiraCertificatePathField.text = state.jiraCertificatePath
        panel.jiraJqlField.text = state.jiraJql
        panel.publishSvnRevisionCheckbox.isSelected = state.publishSvnRevisionToJira
        panel.svnRepositoryUrlField.text = state.svnRepositoryUrl
        panel.svnUsernameField.text = state.svnUsername
        panel.messageWrapperTypeDropdown.selectedItem = state.messageWrapperType
        panel.prefixTypeDropdown.selectedItem = state.messagePrefixType
        panel.infixTypeDropdown.selectedItem = state.messageInfixType
        panel.prependJiraIssueOnPluginActionClickCheckbox.isSelected = state.isPrependJiraIssueOnActionClick
        panel.isAutoDetectJiraProjectKeyCheckbox.isSelected = state.isAutoDetectJiraProjectKey
        panel.projectKeysModel = CollectionListModel(state.jiraProjectKeys)
        panel.projectKeysList.model = panel.projectKeysModel
        panel.loadPasswords()
    }
}
