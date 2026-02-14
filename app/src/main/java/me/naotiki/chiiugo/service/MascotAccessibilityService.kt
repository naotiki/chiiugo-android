package me.naotiki.chiiugo.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import dagger.hilt.android.AndroidEntryPoint
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenController
import me.naotiki.chiiugo.domain.screen.AccessibilityScreenResult
import javax.inject.Inject

@AndroidEntryPoint
class MascotAccessibilityService : AccessibilityService() {

    @Inject
    lateinit var accessibilityScreenController: AccessibilityScreenController

    override fun onServiceConnected() {
        super.onServiceConnected()
        accessibilityScreenController.setAvailable(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val nodeRoot = rootInActiveWindow
        val packageName = event?.packageName?.toString()?.takeIf { it.isNotBlank() }
            ?: nodeRoot?.packageName?.toString()?.takeIf { it.isNotBlank() }
        val activityName = event?.className?.toString()?.takeIf { it.isNotBlank() }
            ?: nodeRoot?.className?.toString()?.takeIf { it.isNotBlank() }

        val extractedText = try {
            if (nodeRoot != null) {
                extractTextFromNodeTree(nodeRoot)
            } else {
                extractTextFromEvent(event)
            }
        } catch (_: Throwable) {
            ""
        }
        val appName = packageName?.let(::resolveAppName)
        accessibilityScreenController.updateSnapshot(
            AccessibilityScreenResult(
                text = extractedText,
                appName = appName,
                packageName = packageName,
                activityName = activityName
            )
        )
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        accessibilityScreenController.setAvailable(false)
        accessibilityScreenController.clearSnapshot()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        accessibilityScreenController.setAvailable(false)
        accessibilityScreenController.clearSnapshot()
        super.onDestroy()
    }

    private fun extractTextFromNodeTree(rootNode: AccessibilityNodeInfo): String {
        val pendingNodes = ArrayDeque<AccessibilityNodeInfo>()
        pendingNodes.addLast(rootNode)

        val uniqueLines = LinkedHashSet<String>()
        var inspectedNodeCount = 0
        while (pendingNodes.isNotEmpty() && inspectedNodeCount < MAX_NODE_SCAN) {
            val node = pendingNodes.removeFirst()
            inspectedNodeCount += 1
            node.text?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let(uniqueLines::add)
            node.contentDescription?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?.let(uniqueLines::add)

            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                pendingNodes.addLast(child)
            }
            if (uniqueLines.size >= MAX_LINE_COUNT) {
                break
            }
        }

        return uniqueLines.joinToString("\n").take(MAX_TEXT_CHARS)
    }

    private fun extractTextFromEvent(event: AccessibilityEvent?): String {
        if (event == null) return ""
        val lines = LinkedHashSet<String>()
        event.contentDescription?.toString()
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.let(lines::add)
        event.text.orEmpty()
            .asSequence()
            .mapNotNull { it?.toString()?.trim() }
            .filter { it.isNotBlank() }
            .take(MAX_LINE_COUNT)
            .forEach(lines::add)
        return lines.joinToString("\n").take(MAX_TEXT_CHARS)
    }

    private fun resolveAppName(packageName: String): String {
        return runCatching {
            val appInfo = packageManager.getApplicationInfo(packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(packageName)
    }

    private companion object {
        const val MAX_NODE_SCAN = 250
        const val MAX_LINE_COUNT = 80
        const val MAX_TEXT_CHARS = 2500
    }
}
