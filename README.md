# jira-commit-message-intellij-plugin

Fork adapted for CLion/IntelliJ projects that use **Subversion (SVN)** instead of Git.

<!-- Plugin description -->
The plugin inserts a JIRA issue id into the SVN commit message in the IDE commit dialog/tool window.
Unlike the original Git version, it does not depend on a Git branch name.

The current JIRA issue is configured explicitly in **Settings > Tools > JIRA SVN Commit Message**.
<!-- Plugin description end -->

## Usage

1. Open **Settings > Tools > JIRA SVN Commit Message**.
2. Set **Current JIRA issue**.
3. Open the SVN Commit dialog/tool window.
4. The plugin fills the message with the configured issue id.
5. If the message was cleared, use the frog action to insert it again.

Formatting from the original plugin is preserved: wrapper, prefix, infix and prepend-to-existing-message options.

## Installation

1. Open the repository's **Releases** page.
2. Download the plugin `.zip` attached to the latest release. Do not unpack it.
3. In CLion/IntelliJ IDEA open **Settings > Plugins**.
4. Click the gear icon and choose **Install Plugin from Disk...**.
5. Select the downloaded ZIP and restart the IDE when prompted.

## Build

```bash
./gradlew clean buildPlugin
```

The plugin now depends on JetBrains' bundled **Subversion** plugin instead of `Git4Idea`.

## Branch

SVN adaptation is developed in `feature/svn-support`.
