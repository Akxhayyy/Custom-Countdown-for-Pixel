package com.skn.countdown

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class ConfigureActivity : Activity() {

    private var widgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private var pickedCalendar: Calendar? = null
    private var pendingSave = false

    private var fontKey = WidgetPrefs.DEFAULT_FONT_KEY
    private var backgroundKey = WidgetPrefs.DEFAULT_BACKGROUND_KEY
    private var opacityPercent = WidgetPrefs.DEFAULT_OPACITY_PERCENT
    private var textColorKey = WidgetPrefs.DEFAULT_TEXT_COLOR_KEY
    private var labelScalePercent = WidgetPrefs.DEFAULT_LABEL_SCALE_PERCENT
    private var valueScalePercent = WidgetPrefs.DEFAULT_VALUE_SCALE_PERCENT

    private lateinit var labelInput: EditText
    private lateinit var datetimeDisplay: TextView
    private lateinit var infoText: TextView
    private lateinit var previewImage: ImageView
    private lateinit var fontSpinner: Spinner
    private lateinit var backgroundSpinner: Spinner
    private lateinit var textColorSpinner: Spinner
    private lateinit var opacitySeekBar: SeekBar
    private lateinit var opacityLabel: TextView
    private lateinit var labelScaleSeekBar: SeekBar
    private lateinit var labelScaleLabel: TextView
    private lateinit var valueScaleSeekBar: SeekBar
    private lateinit var valueScaleLabel: TextView

    private val dateFormat = SimpleDateFormat("EEE, d MMM yyyy 'at' HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        widgetId = intent.getIntExtra(
            AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
        )

        // Standard widget-config contract: if the user backs out without
        // saving, the host must NOT place the widget.
        setResult(Activity.RESULT_CANCELED, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))

        if (widgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            setContentView(TextView(this).apply {
                text = "Add the Countdown widget to your home screen to create a timer."
                setPadding(48, 96, 48, 48)
                textSize = 16f
            })
            return
        }

        setContentView(R.layout.activity_configure)
        labelInput = findViewById(R.id.label_input)
        datetimeDisplay = findViewById(R.id.datetime_display)
        infoText = findViewById(R.id.info_text)
        previewImage = findViewById(R.id.preview_image)
        fontSpinner = findViewById(R.id.font_spinner)
        backgroundSpinner = findViewById(R.id.background_spinner)
        textColorSpinner = findViewById(R.id.text_color_spinner)
        opacitySeekBar = findViewById(R.id.opacity_seekbar)
        opacityLabel = findViewById(R.id.opacity_label)
        labelScaleSeekBar = findViewById(R.id.label_scale_seekbar)
        labelScaleLabel = findViewById(R.id.label_scale_label)
        valueScaleSeekBar = findViewById(R.id.value_scale_seekbar)
        valueScaleLabel = findViewById(R.id.value_scale_label)

        if (WidgetPrefs.isConfigured(this, widgetId)) {
            labelInput.setText(WidgetPrefs.getLabel(this, widgetId))
            val cal = Calendar.getInstance().apply {
                timeInMillis = WidgetPrefs.getTarget(this@ConfigureActivity, widgetId)
            }
            pickedCalendar = cal
            datetimeDisplay.text = dateFormat.format(cal.time)
            fontKey = WidgetPrefs.getFontKey(this, widgetId)
            backgroundKey = WidgetPrefs.getBackgroundKey(this, widgetId)
            opacityPercent = WidgetPrefs.getOpacityPercent(this, widgetId)
            textColorKey = WidgetPrefs.getTextColorKey(this, widgetId)
            labelScalePercent = WidgetPrefs.getLabelScalePercent(this, widgetId)
            valueScalePercent = WidgetPrefs.getValueScalePercent(this, widgetId)
        }

        setUpFontSpinner()
        setUpBackgroundSpinner()
        setUpTextColorSpinner()
        setUpOpacitySeekBar()
        setUpLabelScaleSeekBar()
        setUpValueScaleSeekBar()

        labelInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) = refreshPreview()
        })

        findViewById<Button>(R.id.pick_datetime_button).setOnClickListener { openDatePicker() }
        findViewById<Button>(R.id.save_button).setOnClickListener { attemptSave() }

        maybeRequestNotificationPermission()
        updateExactAlarmNotice()
        refreshPreview()
    }

    override fun onResume() {
        super.onResume()
        updateExactAlarmNotice()
        if (pendingSave && AlarmScheduler.canScheduleExactAlarms(this)) {
            pendingSave = false
            performSave()
        }
    }

    private fun setUpFontSpinner() {
        val choices = FontChoice.entries
        fontSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices.map { it.label })
        fontSpinner.setSelection(choices.indexOfFirst { it.key == fontKey }.coerceAtLeast(0))
        fontSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                fontKey = choices[position].key
                refreshPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setUpBackgroundSpinner() {
        val presets = BackgroundPreset.entries
        backgroundSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, presets.map { it.label })
        backgroundSpinner.setSelection(presets.indexOfFirst { it.key == backgroundKey }.coerceAtLeast(0))
        backgroundSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                backgroundKey = presets[position].key
                updateOpacityEnabled()
                refreshPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
        updateOpacityEnabled()
    }

    private fun setUpTextColorSpinner() {
        val choices = TextColorChoice.entries
        textColorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, choices.map { it.label })
        textColorSpinner.setSelection(choices.indexOfFirst { it.key == textColorKey }.coerceAtLeast(0))
        textColorSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                textColorKey = choices[position].key
                refreshPreview()
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    private fun setUpOpacitySeekBar() {
        opacitySeekBar.progress = opacityPercent
        opacityLabel.text = "Card opacity: $opacityPercent%"
        opacitySeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                opacityPercent = progress
                opacityLabel.text = "Card opacity: $progress%"
                refreshPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setUpLabelScaleSeekBar() {
        labelScaleSeekBar.progress = labelScalePercent
        labelScaleLabel.text = "Label size: $labelScalePercent%"
        labelScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                labelScalePercent = progress
                labelScaleLabel.text = "Label size: $progress%"
                refreshPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun setUpValueScaleSeekBar() {
        valueScaleSeekBar.progress = valueScalePercent
        valueScaleLabel.text = "Value size: $valueScalePercent%"
        valueScaleSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                valueScalePercent = progress
                valueScaleLabel.text = "Value size: $progress%"
                refreshPreview()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
    }

    private fun updateOpacityEnabled() {
        val enabled = backgroundKey != BackgroundPreset.NONE.key
        opacitySeekBar.isEnabled = enabled
        opacityLabel.alpha = if (enabled) 1f else 0.4f
    }

    private fun refreshPreview() {
        val (widthPx, heightPx) = WidgetSizing.currentSizePx(this, AppWidgetManager.getInstance(this), widgetId)
        val label = labelInput.text?.toString()?.trim().orEmpty()
        val countdownText = pickedCalendar?.let { Countdown.format(Countdown.remaining(it.timeInMillis)) } ?: "0:00:00"
        val expired = pickedCalendar?.let { Countdown.isExpired(it.timeInMillis) } ?: false

        previewImage.setImageBitmap(
            WidgetRenderer.render(
                this, label, countdownText, expired, widthPx, heightPx,
                fontKey, backgroundKey, opacityPercent, textColorKey,
                labelScalePercent, valueScalePercent, showUnitCaptions = true
            )
        )
    }

    private fun openDatePicker() {
        val base = pickedCalendar ?: Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, day ->
                val cal = (pickedCalendar ?: Calendar.getInstance()).apply {
                    set(Calendar.YEAR, year)
                    set(Calendar.MONTH, month)
                    set(Calendar.DAY_OF_MONTH, day)
                }
                pickedCalendar = cal
                openTimePicker()
            },
            base.get(Calendar.YEAR), base.get(Calendar.MONTH), base.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun openTimePicker() {
        val base = pickedCalendar ?: Calendar.getInstance()
        TimePickerDialog(
            this,
            { _, hour, minute ->
                val cal = (pickedCalendar ?: Calendar.getInstance()).apply {
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                pickedCalendar = cal
                datetimeDisplay.text = dateFormat.format(cal.time)
                refreshPreview()
            },
            base.get(Calendar.HOUR_OF_DAY), base.get(Calendar.MINUTE), true
        ).show()
    }

    private fun attemptSave() {
        val cal = pickedCalendar
        if (cal == null) {
            Toast.makeText(this, "Pick a date and time first", Toast.LENGTH_SHORT).show()
            return
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            Toast.makeText(this, "That's in the past — timer will show expired immediately", Toast.LENGTH_LONG).show()
        }

        if (!AlarmScheduler.canScheduleExactAlarms(this)) {
            pendingSave = true
            Toast.makeText(this, "Allow \"Alarms & reminders\" for exact timing, then come back", Toast.LENGTH_LONG).show()
            startActivity(
                Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))
            )
            return
        }

        performSave()
    }

    private fun performSave() {
        val cal = pickedCalendar ?: return
        val label = labelInput.text.toString().trim()

        WidgetPrefs.save(
            this, widgetId, label, cal.timeInMillis, fontKey, backgroundKey, opacityPercent, textColorKey,
            labelScalePercent, valueScalePercent
        )
        NotificationHelper.ensureChannels(this)
        AlarmScheduler.scheduleExpiry(this, widgetId, cal.timeInMillis)
        AlarmScheduler.scheduleNextTick(this)

        // Covers the "edit an already-placed widget" path, where finishing
        // this activity alone triggers no host update on its own.
        CountdownWidgetProvider.updateWidget(this, AppWidgetManager.getInstance(this), widgetId)

        setResult(Activity.RESULT_OK, Intent().putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId))
        finish()
    }

    private fun maybeRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    private fun updateExactAlarmNotice() {
        infoText.text = if (AlarmScheduler.canScheduleExactAlarms(this)) {
            ""
        } else {
            "Exact alarm permission not yet granted — the expiry alert may be delayed until you grant it."
        }
    }
}
