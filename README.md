# JIRA SVN Commit

IntelliJ Platform plugin for projects that use **Subversion (SVN)** and **Jira**.

<!-- Plugin description -->
JIRA SVN Commit inserts the selected Jira issue into an SVN commit message and can publish the resulting SVN revision back to Jira after a successful commit.

It uses the IDE's Subversion integration and the current working copy. Jira credentials are stored through IntelliJ Password Safe. VisualSVN links can use a separately configured web URL.
<!-- Plugin description end -->

## Features

- Select a Jira issue and insert its key into the SVN commit message.
- Use the current project's SVN working copy and existing SVN authentication.
- Publish the committed SVN revision to the Jira issue.
- Include revision, author and changed paths in the Jira comment.
- Link revisions and changed files to VisualSVN.
- Store Jira credentials with IntelliJ Password Safe.

## Installation

1. Open this repository's **Releases** page.
2. Download the plugin `.zip` attached to the latest release. Do not unpack it.
3. In CLion or another compatible IntelliJ IDE open **Settings > Plugins**.
4. Click the gear icon and choose **Install Plugin from Disk...**.
5. Select the ZIP and restart the IDE when prompted.

## Configuration

Open **Settings > Tools > JIRA SVN Commit** and configure your Jira connection, current issue and optional VisualSVN Web URL. No organization-specific servers or credentials are built into the plugin.

## Build

```bash
./gradlew clean buildPlugin
```

The installable ZIP is created in `build/distributions/`.

## Origin and license

This project started from the Apache-2.0 licensed **jira-commit-message-intellij-plugin** by nemwiz and was substantially reworked for an SVN/Jira workflow. The original project and contributors are acknowledged here to preserve the origin of the derivative work.

The project remains distributed under the Apache License 2.0. See `LICENSE`.
