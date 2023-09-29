package me.naotiki.chiiugo.data.repository.impl

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import dagger.hilt.android.qualifiers.ApplicationContext
import me.naotiki.chiiugo.data.AppInfo
import me.naotiki.chiiugo.data.repository.InstalledAppRepository
import javax.inject.Inject

class InstalledAppRepositoryImpl @Inject constructor(@ApplicationContext private val context: Context) : InstalledAppRepository {
    override fun getInstalledAppInfoList(): List<AppInfo> {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN)
            .also { it.addCategory(Intent.CATEGORY_LAUNCHER) }
        val queryIntentActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong()))
        } else {
            pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
        }
        return queryIntentActivities.mapNotNull { it.activityInfo }.map {
            AppInfo(
                it.loadIcon(pm),
                it.loadLabel(pm).toString(),
                ComponentName(it.packageName, it.name),
            )
        }
    }
}
