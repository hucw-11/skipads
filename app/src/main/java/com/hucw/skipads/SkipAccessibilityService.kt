package com.hucw.skipads

import android.accessibilityservice.AccessibilityService
import android.os.SystemClock
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

class SkipAccessibilityService : AccessibilityService() {

    private var currentPackage: String? = null
    private var windowStartAt = 0L
    private var clickedInWindow = false
    private var lastAttemptAt = 0L

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return
        val pkg = ev.packageName?.toString() ?: return

        val now = SystemClock.uptimeMillis()
        if (pkg != currentPackage) {
            currentPackage = pkg
            windowStartAt = now
            clickedInWindow = false
            lastAttemptAt = 0L
        }

        if (pkg == packageName || pkg in IGNORED_PACKAGES) return
        if (!Prefs.enabled(this)) return
        if (clickedInWindow) return
        if (now - windowStartAt > Prefs.windowMs(this)) return
        if (now - lastAttemptAt < ATTEMPT_GAP_MS) return
        lastAttemptAt = now

        val keywords = Prefs.keywords(this)
        val match = findInActiveWindow(keywords) ?: findInOtherWindows(keywords) ?: return
        val label = match.text?.toString() ?: match.contentDescription?.toString().orEmpty()
        val clickable = clickableTarget(match) ?: return
        if (!clickable.isVisibleToUser) return

        if (clickable.performAction(AccessibilityNodeInfo.ACTION_CLICK)) {
            clickedInWindow = true
            Prefs.recordSkip(this)
            SkipLogStore.add(this, pkg, label)
        }
    }

    override fun onInterrupt() = Unit

    private fun findInActiveWindow(keywords: List<String>): AccessibilityNodeInfo? {
        val root = rootInActiveWindow ?: return null
        return findSkipNode(root, keywords)
    }

    private fun findInOtherWindows(keywords: List<String>): AccessibilityNodeInfo? {
        val activeRoot = rootInActiveWindow
        for (window in windows) {
            val root = window.root ?: continue
            if (root == activeRoot) continue
            val found = findSkipNode(root, keywords)
            if (found != null) return found
        }
        return null
    }

    private fun findSkipNode(
        root: AccessibilityNodeInfo,
        keywords: List<String>
    ): AccessibilityNodeInfo? {
        val queue = ArrayDeque<Pair<AccessibilityNodeInfo, Int>>()
        queue.addLast(root to 0)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val (node, depth) = queue.removeFirst()
            visited++

            val text = node.text?.toString()
            val description = node.contentDescription?.toString()
            val matched = SkipMatcher.isSkipText(text, keywords) ||
                SkipMatcher.isSkipText(description, keywords)
            if (matched && node.isVisibleToUser) {
                return node
            }

            if (depth < MAX_DEPTH) {
                for (index in 0 until node.childCount) {
                    val child = node.getChild(index) ?: continue
                    queue.addLast(child to depth + 1)
                }
            }
        }
        return null
    }

    private fun clickableTarget(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        var current: AccessibilityNodeInfo? = node
        var hops = 0
        while (current != null && hops <= MAX_PARENT_HOPS) {
            if (current.isClickable && current.isEnabled) return current
            current = current.parent
            hops++
        }
        return null
    }

    companion object {
        private const val ATTEMPT_GAP_MS = 300L
        private const val MAX_NODES = 3000
        private const val MAX_DEPTH = 30
        private const val MAX_PARENT_HOPS = 5

        private val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.permissioncontroller",
            "com.google.android.permissioncontroller",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",
            "com.miui.home",
            "com.huawei.android.launcher",
            "com.oppo.launcher",
            "com.vivo.launcher",
            "com.bbk.launcher2",
            "com.sec.android.app.launcher"
        )
    }
}
