# jira-commit-message-intellij-plugin

Fork adapted for CLion/IntelliJ projects that use **Subversion (SVN)** instead of Git.

<!-- Plugin description -->
The plugin inserts a JIRA issue id into the SVN commit message in the IDE commit dialog/tool window.
Unlike the original Git version, it does not depend on a Git branch name.

The current JIRA issue is configured explicitly in **Settings > Tools > JIRA SVN Commit Message**.
The same settings page also contains the JIRA and SVN connection parameters used in Anton's Jira activity tooling.
<!-- Plugin description end -->

## Default configuration

- JIRA URL: `https://jira.srvrg.com`
- JIRA user: `tikhonov`
- SVN repository: `https://svn.srvrg.com/svn/PaySys`
- SVN user: `tikhonov`
- JIRA client certificate path: configured locally, for example `tikhonov26.p12`

Passwords are intentionally **not committed to the repository**. The existing Jira activity tool uses local configuration for `JIRA_PASSWORD`, `JIRA_CERT_PASSWORD` and SVN password; this plugin keeps credentials out of source control as well.

## Usage

1. Open **Settings > Tools > JIRA SVN Commit Message**.
2. Set **Current JIRA issue**, for example `FAREPLUS-4032`.
3. Open the SVN Commit dialog/tool window.
4. The plugin fills the message with the configured issue id.
5. If the message was cleared, use the frog action to insert it again.

Formatting from the original plugin is preserved: wrapper, prefix, infix and prepend-to-existing-message options.

## Build

```bash
./gradlew clean buildPlugin
```

The plugin now depends on JetBrains' bundled **Subversion** plugin instead of `Git4Idea`.

## Branch

SVN adaptation is developed in `feature/svn-support`.
