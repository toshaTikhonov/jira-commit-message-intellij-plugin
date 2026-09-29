package org.nemwiz.jiracommitmessage.configuration

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.*
import com.intellij.util.ui.FormBuilder
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import org.nemwiz.jiracommitmessage.services.CredentialService
import org.nemwiz.jiracommitmessage.services.JiraClient

class PluginSettingsConfigurationPanel {
    val jiraIssueCombo = ComboBox<String>().apply { isEditable = true }
    val refreshIssuesButton = JButton("Refresh issues")
    val jiraBaseUrlField = JBTextField()
    val jiraUserField = JBTextField()
    val jiraPasswordField = JBPasswordField()
    val jiraCertificatePathField = JBTextField()
    val jiraCertificatePasswordField = JBPasswordField()
    val jiraJqlField = JBTextField()
    val publishSvnRevisionCheckbox = JBCheckBox()
    val svnRepositoryUrlField = JBTextField()
    val svnUsernameField = JBTextField()
    val svnPasswordField = JBPasswordField()

    lateinit var mainPanel: JPanel
    var messageWrapperTypeDropdown: ComboBox<String> = ComboBox()
    var prefixTypeDropdown: ComboBox<String> = ComboBox()
    var infixTypeDropdown: ComboBox<String> = ComboBox()
    var isAutoDetectJiraProjectKeyCheckbox = JBCheckBox()
    var prependJiraIssueOnPluginActionClickCheckbox = JBCheckBox()
    var projectKeysList: JBList<String>
    var projectKeysModel: CollectionListModel<String>
    private var toolbar: ToolbarDecorator

    init {
        MessageWrapperType.values().forEach { messageWrapperTypeDropdown.addItem(it.type) }
        PrefixType.values().forEach { prefixTypeDropdown.addItem(it.type) }
        InfixType.values().forEach { infixTypeDropdown.addItem(it.type) }

        projectKeysModel = CollectionListModel(PluginSettingsState.instance.state.jiraProjectKeys)
        projectKeysList = JBList(projectKeysModel)
        toolbar = ToolbarDecorator.createDecorator(projectKeysList).disableUpDownActions()
        toolbar.setAddAction {
            val dialog = AddProjectKeyDialog()
            if (dialog.showAndGet()) projectKeysModel.add(dialog.addProjectKeyField.text)
        }

        refreshIssuesButton.addActionListener {
            refreshIssuesButton.isEnabled = false
            Thread {
                runCatching { JiraClient().searchIssues() }.onSuccess { issues ->
                    javax.swing.SwingUtilities.invokeLater {
                        val selected = currentIssueKey()
                        jiraIssueCombo.removeAllItems()
                        issues.forEach { jiraIssueCombo.addItem(it.toString()) }
                        if (selected.isNotBlank()) jiraIssueCombo.selectedItem = selected
                        refreshIssuesButton.isEnabled = true
                    }
                }.onFailure {
                    javax.swing.SwingUtilities.invokeLater {
                        refreshIssuesButton.isEnabled = true
                        javax.swing.JOptionPane.showMessageDialog(mainPanel, it.message, "Jira", javax.swing.JOptionPane.ERROR_MESSAGE)
                    }
                }
            }.start()
        }

        val issuePanel = JPanel(java.awt.BorderLayout(6, 0)).apply {
            add(jiraIssueCombo, java.awt.BorderLayout.CENTER)
            add(refreshIssuesButton, java.awt.BorderLayout.EAST)
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addLabeledComponent(JBLabel("Current JIRA issue"), issuePanel, 1, false)
            .addLabeledComponent(JBLabel("JIRA URL"), jiraBaseUrlField, 1, false)
            .addLabeledComponent(JBLabel("JIRA user"), jiraUserField, 1, false)
            .addLabeledComponent(JBLabel("JIRA password (Password Safe)"), jiraPasswordField, 1, false)
            .addLabeledComponent(JBLabel("JIRA client certificate (.p12)"), jiraCertificatePathField, 1, false)
            .addLabeledComponent(JBLabel("Certificate password (Password Safe)"), jiraCertificatePasswordField, 1, false)
            .addLabeledComponent(JBLabel("JIRA JQL"), jiraJqlField, 1, false)
            .addLabeledComponent(JBLabel("Publish SVN revision to JIRA after commit"), publishSvnRevisionCheckbox, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("SVN repository URL"), svnRepositoryUrlField, 1, false)
            .addLabeledComponent(JBLabel("SVN username"), svnUsernameField, 1, false)
            .addLabeledComponent(JBLabel("SVN password (Password Safe)"), svnPasswordField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Commit message wrapper"), messageWrapperTypeDropdown, 1, false)
            .addLabeledComponent(JBLabel("Commit message prefix"), prefixTypeDropdown, 1, false)
            .addLabeledComponent(JBLabel("Commit message infix"), infixTypeDropdown, 1, false)
            .addLabeledComponent(JBLabel("Prepend JIRA issue to existing message"), prependJiraIssueOnPluginActionClickCheckbox, 1, false)
            .addLabeledComponent(JBLabel("Detect JIRA issue in commit text"), isAutoDetectJiraProjectKeyCheckbox, 1, false)
            .addLabeledComponent(JBLabel("JIRA project keys"), toolbar.createPanel(), 9, true)
            .addComponentFillVertically(JPanel(), 0).panel
    }

    fun currentIssueKey(): String =
        jiraIssueCombo.editor.item?.toString().orEmpty().substringBefore(" — ").trim()

    fun loadPasswords() {
        jiraPasswordField.text = CredentialService.jiraPassword
        jiraCertificatePasswordField.text = CredentialService.jiraCertificatePassword
        svnPasswordField.text = CredentialService.svnPassword
    }

    fun savePasswords() {
        CredentialService.jiraPassword = String(jiraPasswordField.password)
        CredentialService.jiraCertificatePassword = String(jiraCertificatePasswordField.password)
        CredentialService.svnPassword = String(svnPasswordField.password)
    }

    fun getPreferredFocusedComponent(): JComponent = jiraIssueCombo
}
