package org.nemwiz.jiracommitmessage.services

import com.intellij.openapi.components.service
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.nemwiz.jiracommitmessage.configuration.InfixType
import org.nemwiz.jiracommitmessage.configuration.MessageWrapperType
import org.nemwiz.jiracommitmessage.configuration.PluginSettingsState
import org.nemwiz.jiracommitmessage.configuration.PrefixType

class JiraCommitMessagePluginTest : BasePlatformTestCase() {

    override fun setUp() {
        super.setUp()
        PluginSettingsState.instance.pluginState = PluginSettingsState.PluginState()
    }

    fun testBuildsCommitMessageFromConfiguredIssue() {
        val state = PluginSettingsState.instance.state
        state.jiraIssueKey = "fareplus-4032"
        state.messageWrapperType = MessageWrapperType.ROUND.type
        state.messagePrefixType = PrefixType.NO_PREFIX.type
        state.messageInfixType = InfixType.NO_INFIX.type

        val plugin = project.service<JiraCommitMessagePlugin>()

        assertEquals("(FAREPLUS-4032) ", plugin.getCommitMessage())
    }

    fun testSupportsManualFormatting() {
        val state = PluginSettingsState.instance.state
        state.jiraIssueKey = "FAREPLUS-4018"
        state.messageWrapperType = MessageWrapperType.NO_WRAPPER.type
        state.messagePrefixType = PrefixType.HASH.type
        state.messageInfixType = InfixType.COLON_SPACE.type

        val plugin = project.service<JiraCommitMessagePlugin>()

        assertEquals("#FAREPLUS-4018:  ", plugin.getCommitMessage())
    }

    fun testCanExtractIssueFromText() {
        val state = PluginSettingsState.instance.state
        state.isAutoDetectJiraProjectKey = true
        state.messageWrapperType = MessageWrapperType.ROUND.type

        val plugin = project.service<JiraCommitMessagePlugin>()

        assertEquals("(FAREPLUS-3980) ", plugin.getCommitMessageFromText("fix FAREPLUS-3980"))
    }

    fun testFallsBackToConfiguredIssueWhenTextHasNoIssue() {
        val state = PluginSettingsState.instance.state
        state.jiraIssueKey = "FAREPLUS-4032"
        state.isAutoDetectJiraProjectKey = true

        val plugin = project.service<JiraCommitMessagePlugin>()

        assertEquals("(FAREPLUS-4032) ", plugin.getCommitMessageFromText("обычный комментарий"))
    }
}
