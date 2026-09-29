package org.nemwiz.jiracommitmessage.provider

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

class PluginNotifier {
    fun showInfo(project: Project?, title: String, message: String) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("JIRA Id Commit Message Notification Group")
            .createNotification(message, NotificationType.INFORMATION)
            .setTitle(title)
            .notify(project)
    }

    fun showWarning(project: Project?, title: String, message: String, action: AnAction? = null) {
        val notification = NotificationGroupManager.getInstance()
            .getNotificationGroup("JIRA Id Commit Message Notification Group")
            .createNotification(message, NotificationType.WARNING)
            .setTitle(title)
        if (action != null) notification.addAction(action)
        notification.notify(project)
    }
}
