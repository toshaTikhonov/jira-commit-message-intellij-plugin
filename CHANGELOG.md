# JIRA SVN Commit Changelog

## [1.0.0]

### Added

- SVN-based commit message integration for IntelliJ Platform IDEs.
- Explicit Jira issue selection.
- Jira connection using client certificate support and IntelliJ Password Safe.
- Publication of successful SVN revisions back to Jira.
- VisualSVN revision and changed-path links.
- Configurable VisualSVN Web URL.
- GitHub Actions build and installable ZIP release artifact.

### Changed

- Reworked the original Git branch based workflow into an SVN working-copy workflow.
- Removed the Git4Idea dependency and Git branch listeners.
