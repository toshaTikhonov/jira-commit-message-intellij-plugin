package org.nemwiz.jiracommitmessage.configuration

import com.intellij.openapi.ui.ComboBox
import com.intellij.ui.CollectionListModel
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JComponent
import javax.swing.JPanel

class PluginSettingsConfigurationPanel {

    val jiraIssueKeyField = JBTextField()
    val jiraBaseUrlField = JBTextField()
    val jiraUserField = JBTextField()
    val jiraCertificatePathField = JBTextField()
    val svnRepositoryUrlField = JBTextField()
    val svnUsernameField = JBTextField()

    var mainPanel: JPanel
    var messageWrapperTypeDropdown: ComboBox<String> = ComboBox()
    var prefixTypeDropdown: ComboBox<String> = ComboBox()
    var infixTypeDropdown: ComboBox<String> = ComboBox()
    private var projectKeys = PluginSettingsState.instance.state.jiraProjectKeys
    var isAutoDetectJiraProjectKeyCheckbox: JBCheckBox = JBCheckBox()
    var isConventionalCommitCheckbox: JBCheckBox = JBCheckBox()
    var prependJiraIssueOnPluginActionClickCheckbox: JBCheckBox = JBCheckBox()
    var projectKeysList: JBList<String>
    var projectKeysModel: CollectionListModel<String>
    private var toolbar: ToolbarDecorator

    init {
        messageWrapperTypeDropdown.addItem(MessageWrapperType.NO_WRAPPER.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.ROUND.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.BOX.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.CURLY.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.ANGLE.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.STAR.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.VERTICAL_SLASH.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.FORWARD_SLASH.type)
        messageWrapperTypeDropdown.addItem(MessageWrapperType.BACKSLASH.type)

        prefixTypeDropdown.addItem(PrefixType.NO_PREFIX.type)
        prefixTypeDropdown.addItem(PrefixType.DASH.type)
        prefixTypeDropdown.addItem(PrefixType.COLON.type)
        prefixTypeDropdown.addItem(PrefixType.HASH.type)

        infixTypeDropdown.addItem(InfixType.NO_INFIX.type)
        infixTypeDropdown.addItem(InfixType.DASH.type)
        infixTypeDropdown.addItem(InfixType.DASH_SPACE.type)
        infixTypeDropdown.addItem(InfixType.COLON.type)
        infixTypeDropdown.addItem(InfixType.COLON_SPACE.type)

        projectKeysModel = CollectionListModel(projectKeys)
        projectKeysList = JBList(projectKeysModel)
        projectKeysList.setEmptyText("No project keys configured")

        toolbar = ToolbarDecorator.createDecorator(projectKeysList).disableUpDownActions()
        toolbar.setAddAction {
            val addProjectKeyDialog = AddProjectKeyDialog()
            if (addProjectKeyDialog.showAndGet()) {
                projectKeysModel.add(addProjectKeyDialog.addProjectKeyField.text)
            }
        }

        mainPanel = FormBuilder.createFormBuilder()
            .addSeparator()
            .addLabeledComponent(JBLabel("Current JIRA issue"), jiraIssueKeyField, 1, false)
            .addLabeledComponent(JBLabel("JIRA URL"), jiraBaseUrlField, 1, false)
            .addLabeledComponent(JBLabel("JIRA user"), jiraUserField, 1, false)
            .addLabeledComponent(JBLabel("JIRA client certificate (.p12)"), jiraCertificatePathField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("SVN repository URL"), svnRepositoryUrlField, 1, false)
            .addLabeledComponent(JBLabel("SVN username"), svnUsernameField, 1, false)
            .addSeparator()
            .addLabeledComponent(JBLabel("Commit message bracket/wrapper type"), messageWrapperTypeDropdown, 1, false)
            .addLabeledComponent(JBLabel("Commit message prefix"), prefixTypeDropdown, 1, false)
            .addLabeledComponent(JBLabel("Commit message infix"), infixTypeDropdown, 1, false)
            .addLabeledComponent(
                JBLabel("Prepend JIRA issue to existing commit message on plugin action click"),
                prependJiraIssueOnPluginActionClickCheckbox,
                1,
                false
            )
            .addLabeledComponent(
                JBLabel("Automatically detect JIRA project key from text"),
                isAutoDetectJiraProjectKeyCheckbox,
                1,
                false
            )
            .addLabeledComponent(JBLabel("JIRA project keys"), toolbar.createPanel(), 9, true)
            .addComponentFillVertically(JPanel(), 0)
            .panel
    }

    fun getPreferredFocusedComponent(): JComponent = jiraIssueKeyField
}
