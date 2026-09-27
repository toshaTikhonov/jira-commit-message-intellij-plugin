package org.nemwiz.jiracommitmessage.services

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe

object CredentialService {
    private const val JIRA_PASSWORD_KEY = "org.nemwiz.jira-commit-message.jira.password"
    private const val JIRA_CERT_PASSWORD_KEY = "org.nemwiz.jira-commit-message.jira.cert-password"
    private const val SVN_PASSWORD_KEY = "org.nemwiz.jira-commit-message.svn.password"

    var jiraPassword: String
        get() = get(JIRA_PASSWORD_KEY)
        set(value) = set(JIRA_PASSWORD_KEY, value)

    var jiraCertificatePassword: String
        get() = get(JIRA_CERT_PASSWORD_KEY)
        set(value) = set(JIRA_CERT_PASSWORD_KEY, value)

    var svnPassword: String
        get() = get(SVN_PASSWORD_KEY)
        set(value) = set(SVN_PASSWORD_KEY, value)

    private fun get(key: String): String =
        PasswordSafe.instance.get(CredentialAttributes(key))?.getPasswordAsString().orEmpty()

    private fun set(key: String, value: String) {
        val attributes = CredentialAttributes(key)
        PasswordSafe.instance.set(attributes, if (value.isBlank()) null else Credentials("", value))
    }
}
