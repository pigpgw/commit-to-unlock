package com.commitunlock.prototype

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView

object UiKit {
    const val COLOR_SURFACE = 0xFFF6F8FA.toInt()
    const val COLOR_PANEL = 0xFFFFFFFF.toInt()
    const val COLOR_INK = 0xFF111827.toInt()
    const val COLOR_MUTED = 0xFF475569.toInt()
    const val COLOR_SUBTLE = 0xFF64748B.toInt()
    const val COLOR_BORDER = 0xFFE2E8F0.toInt()
    const val COLOR_PRIMARY = 0xFF0969DA.toInt()
    const val COLOR_PRIMARY_DARK = 0xFF0550AE.toInt()
    const val COLOR_SUCCESS = 0xFF1F883D.toInt()
    const val COLOR_WARNING = 0xFF9A6700.toInt()
    const val COLOR_DANGER = 0xFFCF222E.toInt()
    const val COLOR_CODE_BG = 0xFFF1F5F9.toInt()

    enum class ButtonTone {
        PRIMARY,
        SECONDARY,
        DANGER,
        GHOST
    }

    fun dp(context: Context, value: Int): Int {
        return (value * context.resources.displayMetrics.density).toInt()
    }

    fun page(context: Context, content: LinearLayout): ScrollView {
        return ScrollView(context).apply {
            setBackgroundColor(COLOR_SURFACE)
            addView(content)
        }
    }

    fun root(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(context, 20), dp(context, 28), dp(context, 20), dp(context, 32))
            setBackgroundColor(COLOR_SURFACE)
        }
    }

    fun panel(context: Context): LinearLayout {
        return LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
            setPadding(dp(context, 16), dp(context, 14), dp(context, 16), dp(context, 16))
            background = rounded(context, COLOR_PANEL, radiusDp = 8, strokeColor = COLOR_BORDER)
        }
    }

    fun section(context: Context, title: String, subtitle: String? = null): LinearLayout {
        val panel = panel(context)
        panel.addView(label(context, title))
        if (!subtitle.isNullOrBlank()) {
            panel.addView(body(context, subtitle).apply {
                setPadding(0, dp(context, 4), 0, dp(context, 10))
            })
        }
        return panel
    }

    fun title(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 28f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_INK)
            includeFontPadding = false
        }
    }

    fun heading(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 22f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_INK)
            includeFontPadding = false
        }
    }

    fun label(context: Context, text: String): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 16f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(COLOR_INK)
            includeFontPadding = false
        }
    }

    fun body(context: Context, text: String = ""): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 14f
            setTextColor(COLOR_MUTED)
            setLineSpacing(dp(context, 2).toFloat(), 1.0f)
        }
    }

    fun monoBlock(context: Context): TextView {
        return TextView(context).apply {
            layoutParams = topSpacedParams(context)
            textSize = 13f
            typeface = Typeface.MONOSPACE
            setTextColor(COLOR_INK)
            setPadding(dp(context, 12), dp(context, 10), dp(context, 12), dp(context, 10))
            background = rounded(context, COLOR_CODE_BG, radiusDp = 8, strokeColor = COLOR_BORDER)
            setLineSpacing(dp(context, 2).toFloat(), 1.0f)
        }
    }

    fun pill(context: Context, text: String, color: Int = COLOR_PRIMARY): TextView {
        return TextView(context).apply {
            this.text = text
            textSize = 12f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(color)
            setPadding(dp(context, 10), dp(context, 5), dp(context, 10), dp(context, 5))
            background = rounded(context, 0xFFEFF6FF.toInt(), radiusDp = 8, strokeColor = 0xFFBFDBFE.toInt())
        }
    }

    fun input(context: Context, hint: String, minLines: Int = 1): EditText {
        return EditText(context).apply {
            layoutParams = topSpacedParams(context)
            this.hint = hint
            this.minLines = minLines
            textSize = 14f
            setTextColor(COLOR_INK)
            setHintTextColor(COLOR_SUBTLE)
            setPadding(dp(context, 12), dp(context, 8), dp(context, 12), dp(context, 8))
            background = rounded(context, COLOR_PANEL, radiusDp = 8, strokeColor = COLOR_BORDER)
        }
    }

    fun checkbox(context: Context, label: String, checked: Boolean = false): CheckBox {
        return CheckBox(context).apply {
            layoutParams = topSpacedParams(context, topDp = 6)
            text = label
            isChecked = checked
            textSize = 14f
            setTextColor(COLOR_INK)
            buttonTintList = ColorStateList.valueOf(COLOR_PRIMARY)
            setPadding(0, dp(context, 2), 0, dp(context, 2))
        }
    }

    fun button(
        context: Context,
        label: String,
        tone: ButtonTone = ButtonTone.SECONDARY,
        action: () -> Unit
    ): Button {
        return Button(context).apply {
            layoutParams = topSpacedParams(context)
            text = label
            isAllCaps = false
            textSize = 14f
            minHeight = dp(context, 44)
            gravity = Gravity.CENTER
            setPadding(dp(context, 12), 0, dp(context, 12), 0)
            val (backgroundColor, textColor, strokeColor) = when (tone) {
                ButtonTone.PRIMARY -> Triple(COLOR_PRIMARY, 0xFFFFFFFF.toInt(), COLOR_PRIMARY)
                ButtonTone.SECONDARY -> Triple(0xFFEFF6FF.toInt(), COLOR_PRIMARY_DARK, 0xFFBFDBFE.toInt())
                ButtonTone.DANGER -> Triple(0xFFFFEBE9.toInt(), COLOR_DANGER, 0xFFFFC1BA.toInt())
                ButtonTone.GHOST -> Triple(COLOR_PANEL, COLOR_MUTED, COLOR_BORDER)
            }
            backgroundTintList = ColorStateList.valueOf(backgroundColor)
            setTextColor(textColor)
            background = rounded(context, backgroundColor, radiusDp = 8, strokeColor = strokeColor)
            setOnClickListener { action() }
        }
    }

    fun addGap(parent: LinearLayout, sizeDp: Int = 10) {
        parent.addView(View(parent.context), LinearLayout.LayoutParams(1, dp(parent.context, sizeDp)))
    }

    fun addPanelGap(parent: LinearLayout) {
        addGap(parent, 12)
    }

    private fun topSpacedParams(context: Context, topDp: Int = 8): LinearLayout.LayoutParams {
        return LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = dp(context, topDp)
        }
    }

    fun rounded(
        context: Context,
        color: Int,
        radiusDp: Int,
        strokeColor: Int? = null,
        strokeDp: Int = 1
    ): GradientDrawable {
        return GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(context, radiusDp).toFloat()
            if (strokeColor != null) {
                setStroke(dp(context, strokeDp), strokeColor)
            }
        }
    }
}
