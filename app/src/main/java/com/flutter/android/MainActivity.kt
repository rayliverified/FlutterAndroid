package com.flutter.android

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import androidx.appcompat.app.AppCompatActivity;
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup

import kotlinx.android.synthetic.main.activity_main.*
import io.flutter.facade.Flutter
import android.widget.FrameLayout
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import io.flutter.facade.FlutterFragment
import io.flutter.view.FlutterView

class MainActivity : AppCompatActivity() {

    var switchRouteCounter = 0
    var routeArray = arrayListOf("page_main", "page_transparent")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val flutterView = Flutter.createView(
            this@MainActivity,
            lifecycle,
            "page_main"
        )
        flutterView.enableTransparentBackground()

        fab.setOnClickListener { view ->
//            val tx = supportFragmentManager.beginTransaction()
//            val flutterFragment = Flutter.createFragment("page_transparent")
//            tx.replace(R.id.fragment_container, flutterFragment)
//            tx.commit()
//            val viewInflated = LayoutInflater.from(this).inflate(R.layout.layoutDialog, view as ViewGroup?, false)

//            Log.d("Initial Route", routeArray[switchRouteCounter])
//            flutterView.setInitialRoute(routeArray[switchRouteCounter])
//            if (switchRouteCounter >= routeArray.size - 1) {
//                switchRouteCounter = 0
//            } else {
//                switchRouteCounter += 1
//            }

            if (flutterView.parent != null) {
                (flutterView.parent as ViewGroup).removeView(flutterView)
            }

            val dialog = AlertDialog.Builder(this)
                    .setView(flutterView)
                    .show()

//            viewInflated.reset_password_done_button.setOnClickListener {
//                dialog.dismiss()
//                try {
//                    Navigation.findNavController(view!!).popBackStack()
//                    //Hide keyboard after popping backstack to avoid layout jitter
//                } catch (e: IllegalArgumentException) {
//                    Timber.e(e)
//                }
//            }
//
//            dialog.show()

//            val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
//            addContentView(flutterView, layout)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }
}
