package sexy.mycodeis.suckzoo.sexyforecaster

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.TextView
import org.jetbrains.anko.*
import org.jetbrains.anko.design.*

/**
 * Created by Suckzoo on 2017. 3. 14..
 */
class MainActivityUI : AnkoComponent<MainActivity> {
    lateinit var weatherImageView: ImageView
    lateinit var cityTextView: TextView
    lateinit var weatherTextView: TextView
    lateinit var tempTextView: TextView
    lateinit var tempRangeTextView: TextView
    lateinit var chanceOfRainTextView: TextView
    lateinit var forecastLayout: LinearLayout
    lateinit var hourlyLayout: LinearLayout

    override fun createView(ui: AnkoContext<MainActivity>) = with(ui) {
        coordinatorLayout {
            weatherImageView = imageView {
                maxHeight = 480

            }
            verticalLayout {
                padding = dip(8)
                cityTextView = textView {
                    text = ""
                    textSize = 24f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                weatherTextView = textView {
                    text = ""
                    textSize = 16f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                tempTextView = textView {
                    text = ""
                    textSize = 72f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                tempRangeTextView = textView {
                    text = ""
                    textSize = 24f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                chanceOfRainTextView = textView {
                    text = ""
                    textSize = 16f
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                view {
                    backgroundColor = Color.GRAY
                }.lparams {
                    height = dip(1)
                }
                textView {
                    text = "Daily forecast"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                forecastLayout = verticalLayout {

                }.lparams {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
                view {
                    backgroundColor = Color.GRAY
                }.lparams {
                    height = dip(1)
                }
                textView {
                    text = "Hourly forecast"
                    textAlignment = View.TEXT_ALIGNMENT_CENTER
                }
                hourlyLayout = verticalLayout {

                }.lparams {
                    gravity = Gravity.CENTER_HORIZONTAL
                }
            }.lparams {
                gravity = Gravity.CENTER_HORIZONTAL or Gravity.TOP
            }
            floatingActionButton {
                imageResource = android.R.drawable.ic_menu_rotate
                onClick {
                    ui.owner.refresh()
                }
            }.lparams {
                gravity = Gravity.BOTTOM or Gravity.END
            }
        }
    }
}