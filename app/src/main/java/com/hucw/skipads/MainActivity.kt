package com.hucw.skipads

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.hucw.skipads.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBehavior()
        setupKeywords()
        setupLogs()

        binding.btnEnableService.setOnClickListener { showEnableGuide() }
    }

    override fun onResume() {
        super.onResume()
        updateServiceStatus()
        updateStats()
        renderLogs()
    }

    private fun setupBehavior() {
        binding.switchEnabled.isChecked = Prefs.enabled(this)
        binding.switchEnabled.setOnCheckedChangeListener { _, checked ->
            Prefs.setEnabled(this, checked)
        }

        binding.seekWindow.max = MAX_WINDOW_SECONDS - MIN_WINDOW_SECONDS
        val seconds = (Prefs.windowMs(this) / 1000L).toInt()
            .coerceIn(MIN_WINDOW_SECONDS, MAX_WINDOW_SECONDS)
        binding.seekWindow.progress = seconds - MIN_WINDOW_SECONDS
        updateWindowLabel(seconds)

        binding.seekWindow.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val value = MIN_WINDOW_SECONDS + progress
                updateWindowLabel(value)
                if (fromUser) {
                    Prefs.setWindowMs(this@MainActivity, value * 1000L)
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit

            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })
    }

    private fun setupKeywords() {
        binding.etKeywords.setText(Prefs.keywords(this).joinToString("\n"))

        binding.btnSaveKeywords.setOnClickListener { saveKeywords() }
        binding.btnResetKeywords.setOnClickListener {
            Prefs.setKeywords(this, Prefs.DEFAULT_KEYWORDS)
            binding.etKeywords.setText(Prefs.DEFAULT_KEYWORDS.joinToString("\n"))
            toast(getString(R.string.keywords_reset))
        }
    }

    private fun setupLogs() {
        binding.btnClearLog.setOnClickListener {
            SkipLogStore.clear(this)
            renderLogs()
            toast(getString(R.string.logs_cleared))
        }
    }

    private fun saveKeywords() {
        val list = binding.etKeywords.text.toString()
            .split('\n', ',', '，')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
        if (list.isEmpty()) {
            toast(getString(R.string.keywords_empty))
            return
        }
        Prefs.setKeywords(this, list)
        binding.etKeywords.setText(list.joinToString("\n"))
        toast(getString(R.string.keywords_saved))
    }

    private fun updateServiceStatus() {
        val enabled = isServiceEnabled()
        binding.tvServiceStatus.text = getString(
            if (enabled) R.string.service_enabled else R.string.service_disabled
        )
        binding.btnEnableService.text = getString(
            if (enabled) R.string.open_accessibility_settings else R.string.enable_service
        )
    }

    private fun updateStats() {
        val (today, total) = Prefs.stats(this)
        binding.tvStats.text = getString(R.string.stats_format, today, total)
    }

    private fun updateWindowLabel(seconds: Int) {
        binding.tvWindowValue.text = getString(R.string.seconds_format, seconds)
    }

    private fun renderLogs() {
        val entries = SkipLogStore.recent(this, LOG_DISPLAY_LIMIT)
        if (entries.isEmpty()) {
            binding.tvLog.text = getString(R.string.logs_empty)
            return
        }
        val builder = StringBuilder()
        for (entry in entries) {
            val text = entry.text.ifBlank { getString(R.string.log_no_text) }
            builder.append(entry.time).append("  ").append(text).append('\n')
            builder.append("    ").append(entry.pkg).append('\n')
        }
        binding.tvLog.text = builder.toString().trimEnd()
    }

    private fun isServiceEnabled(): Boolean {
        val manager = getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
        val services =
            manager.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        val myService = SkipAccessibilityService::class.java.name
        return services.any { it.resolveInfo.serviceInfo.name == myService }
    }

    private fun showEnableGuide() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !isServiceEnabled()) {
            AlertDialog.Builder(this)
                .setTitle(R.string.enable_dialog_title)
                .setMessage(R.string.enable_dialog_message)
                .setPositiveButton(R.string.enable_dialog_go_accessibility) { _, _ ->
                    openAccessibilitySettings()
                }
                .setNeutralButton(R.string.enable_dialog_app_info) { _, _ ->
                    openAppInfo()
                }
                .setNegativeButton(android.R.string.cancel, null)
                .show()
        } else {
            openAccessibilitySettings()
        }
    }

    private fun openAccessibilitySettings() {
        runCatching { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
            .onFailure { toast(getString(R.string.open_settings_failed)) }
    }

    private fun openAppInfo() {
        val intent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.fromParts("package", packageName, null)
        )
        runCatching { startActivity(intent) }
            .onFailure { toast(getString(R.string.open_settings_failed)) }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        private const val MIN_WINDOW_SECONDS = 2
        private const val MAX_WINDOW_SECONDS = 12
        private const val LOG_DISPLAY_LIMIT = 20
    }
}
