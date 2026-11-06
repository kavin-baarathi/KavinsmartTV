package com.kavin.tvhome

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Outline
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextClock
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView

class MainActivity : Activity() {

    data class AppItem(val label: String, val image: Drawable, val banner: Boolean, val intent: Intent)

    private lateinit var grid: RecyclerView
    private val adapter = Adapter()
    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        root.clipChildren = false
        root.background = GradientDrawable(
            GradientDrawable.Orientation.TL_BR,
            intArrayOf(0xFF0B1026.toInt(), 0xFF2A1B4D.toInt(), 0xFF0E3A5C.toInt())
        )
        val clock = TextClock(this).apply {
            format12Hour = "h:mm a"; format24Hour = "h:mm a"
            textSize = 26f; setTextColor(Color.WHITE)
        }
        root.addView(clock, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.END).apply {
            setMargins(0, dp(28), dp(56), 0)
        })
        grid = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@MainActivity, 5)
            clipToPadding = false; clipChildren = false
            setPadding(dp(48), dp(8), dp(48), dp(48))
            adapter = this@MainActivity.adapter
        }
        root.addView(grid, FrameLayout.LayoutParams(-1, -1).apply { topMargin = dp(90) })
        setContentView(root)
    }

    override fun onResume() {
        super.onResume()
        adapter.items = loadApps()
        adapter.notifyDataSetChanged()
        grid.post { grid.getChildAt(0)?.requestFocus() }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* stay on home */ }

    private fun loadApps(): List<AppItem> {
        val pm = packageManager
        val seen = HashSet<String>()
        val out = ArrayList<AppItem>()
        for (cat in listOf(Intent.CATEGORY_LEANBACK_LAUNCHER, Intent.CATEGORY_LAUNCHER)) {
            val q = Intent(Intent.ACTION_MAIN).addCategory(cat)
            for (ri in pm.queryIntentActivities(q, 0)) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                val launch = pm.getLeanbackLaunchIntentForPackage(pkg)
                    ?: pm.getLaunchIntentForPackage(pkg) ?: continue
                val banner = ri.activityInfo.loadBanner(pm)
                out.add(AppItem(ri.loadLabel(pm).toString(), banner ?: ri.loadIcon(pm), banner != null, launch))
            }
        }
        return out.sortedBy { it.label.lowercase() }
    }

    class VH(val card: FrameLayout, val img: ImageView, val label: TextView) : RecyclerView.ViewHolder(card)

    inner class Adapter : RecyclerView.Adapter<VH>() {
        var items: List<AppItem> = emptyList()

        override fun onCreateViewHolder(p: ViewGroup, t: Int): VH {
            val radius = dp(16).toFloat()
            val card = FrameLayout(p.context).apply {
                layoutParams = RecyclerView.LayoutParams(-1, dp(130)).apply {
                    setMargins(dp(12), dp(14), dp(12), dp(14))
                }
                isFocusable = true
                background = GradientDrawable().apply { setColor(0xFF1E2233.toInt()); cornerRadius = radius }
                clipToOutline = true
                outlineProvider = object : ViewOutlineProvider() {
                    override fun getOutline(v: View, o: Outline) { o.setRoundRect(0, 0, v.width, v.height, radius) }
                }
            }
            val img = ImageView(p.context)
            val label = TextView(p.context).apply {
                setTextColor(Color.WHITE); textSize = 14f; gravity = Gravity.CENTER
                setPadding(dp(8), dp(6), dp(8), dp(6)); alpha = 0f
                setBackgroundColor(0xAA000000.toInt()); maxLines = 1
            }
            card.addView(img, FrameLayout.LayoutParams(-1, -1))
            card.addView(label, FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM))
            return VH(card, img, label)
        }

        override fun onBindViewHolder(h: VH, i: Int) {
            val item = items[i]
            h.img.setImageDrawable(item.image)
            h.img.scaleType = if (item.banner) ImageView.ScaleType.CENTER_CROP else ImageView.ScaleType.FIT_CENTER
            val pad = if (item.banner) 0 else dp(28); h.img.setPadding(pad, pad, pad, pad)
            h.label.text = item.label
            h.card.setOnClickListener { startActivity(item.intent) }
            h.card.setOnFocusChangeListener { v, focused ->
                val s = if (focused) 1.12f else 1f
                v.animate().scaleX(s).scaleY(s).translationZ(if (focused) dp(20).toFloat() else 0f)
                    .setDuration(160).start()
                h.label.animate().alpha(if (focused) 1f else 0f).setDuration(160).start()
                h.card.foreground = if (focused) GradientDrawable().apply {
                    setStroke(dp(3), Color.WHITE); cornerRadius = dp(16).toFloat()
                } else null
            }
        }

        override fun getItemCount() = items.size
    }
}
