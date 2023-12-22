package ind.arming.gostructtojson

import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.project.Project


object GoStruct2JsonNotifier {
    // Compatible with older versions
//        int buildNumber = ApplicationInfo.getInstance().getBuild().getBaselineVersion();
//        if (buildNumber <203){
//            notificationGroup = new NotificationGroup("GoStruct2Json.NotificationGroup", NotificationDisplayType.BALLOON, true);
//        }else{
    private var notificationGroup: NotificationGroup = NotificationGroupManager.getInstance().getNotificationGroup("GoStruct2Json.NotificationGroup")

    init {
        //        }
    }

    fun notifyWarning(project: Project?, msg: String?) {
        notificationGroup.createNotification(msg!!, NotificationType.WARNING).notify(project)
    }

    fun notifyInfo(project: Project?, msg: String?) {
        notificationGroup.createNotification(msg!!, NotificationType.INFORMATION).notify(project)
    }

    fun notifyError(project: Project?, msg: String?) {
        notificationGroup.createNotification(msg!!, NotificationType.ERROR).notify(project)
    }
}