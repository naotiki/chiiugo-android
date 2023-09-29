package me.naotiki.chiiugo.data.repository

import me.naotiki.chiiugo.data.AppInfo

interface InstalledAppRepository {
    fun getInstalledAppInfoList():List<AppInfo>
}
