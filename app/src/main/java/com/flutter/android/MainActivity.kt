package com.flutter.android

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_main.*
import io.flutter.facade.Flutter
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterView
import android.view.ViewGroup


class MainActivity : AppCompatActivity() {

    var TAG = MainActivity::class.java.name

    val CHANNEL_METHOD_PAGE = "CHANNEL_METHOD_PAGE";
    val CHANNEL_METHOD_NAVIGATION = "CHANNEL_METHOD_NAVIGATION";
    val NAVIGATION_BACK = "NAVIGATION_BACK";
    val NAVIGATION_CLOSE = "NAVIGATION_CLOSE";
    val CHANNEL_METHOD_BACK_STATUS = "CHANNEL_METHOD_BACK_STATUS";

    var flutterBackStatus: Boolean = false
    var flutterViewVisible: Boolean = false
    lateinit var flutterChannel: MethodChannel
    lateinit var flutterView: FlutterView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        flutterView = Flutter.createView(
            this@MainActivity,
            lifecycle,
            "PAGE_MAIN"
        )
        flutterView.enableTransparentBackground()

        flutterChannel = MethodChannel(flutterView, "app")
        // Receive method invocations from Dart and return results.
        flutterChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                CHANNEL_METHOD_NAVIGATION -> {
                    when (call.arguments) {
                        NAVIGATION_BACK -> {
                            Log.d(TAG, "NAVIGATION_BACK")
                            onBackPressed()
                        }
                        NAVIGATION_CLOSE -> {
                            Log.d(TAG, "NAVIGATION_CLOSE")
                            hideFlutterView()
                        }
                    }
                    result.success("Navigation: ${call.arguments}")
                }
                CHANNEL_METHOD_BACK_STATUS -> {
                    try {
                        Log.d(TAG, "Back Status: ${call.arguments}")
                        flutterBackStatus = call.arguments as Boolean
                    } catch (e: TypeCastException) {
                        Log.e(TAG, "${call.arguments}")
                    }
                    result.success("Back Status: ${call.arguments}")
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        button_1.setOnClickListener {
            //Invoke Flutter Method.
            flutterChannel.invokeMethod(CHANNEL_METHOD_PAGE, "PAGE_MAIN", object : MethodChannel.Result {
                override fun success(result: Any?) {
                    Log.i("Flutter Channel", "$result")
                }

                override fun error(code: String?, msg: String?, details: Any?) {
                    Log.e("Flutter Channel", "$msg")
                }

                override fun notImplemented() {
                    Log.e("Flutter Channel", "Not implemented")
                }
            })
        }

        button_2.setOnClickListener {
            //Invoke Flutter Method.
            flutterChannel.invokeMethod(CHANNEL_METHOD_PAGE, "PAGE_TRANSPARENT", object : MethodChannel.Result {
                override fun success(result: Any?) {
                    Log.i("Flutter Channel", "$result")
                }

                override fun error(code: String?, msg: String?, details: Any?) {
                    Log.e("Flutter Channel", "$msg")
                }

                override fun notImplemented() {
                    Log.e("Flutter Channel", "Not implemented")
                }
            })
        }

        button_3.setOnClickListener {
            flutterViewVisible = true
            flutterChannel.invokeMethod(
                CHANNEL_METHOD_PAGE,
                "PAGE_DELETE_ACCOUNT",
                object : MethodChannel.Result {
                    override fun success(result: Any?) {
                        Log.i("Flutter Channel", "$result")
                    }

                    override fun error(code: String?, msg: String?, details: Any?) {
                        Log.e("Flutter Channel", "$msg")
                    }

                    override fun notImplemented() {
                        Log.e("Flutter Channel", "Not implemented")
                    }
                })

            if (flutterView.parent != null) {
                (flutterView.parent as ViewGroup).removeView(flutterView)
            }
            val layout = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
            )
            addContentView(flutterView, layout)
        }

//        if (flutterView.parent != null) {
//            (flutterView.parent as ViewGroup).removeView(flutterView)
//        }
//        view_flutter_container.addView(flutterView)

        fab.setOnClickListener { view ->
            //            val tx = supportFragmentManager.beginTransaction()
//            val flutterFragment = Flutter.createFragment("page_transparent")
//            tx.replace(R.id.fragment_container, flutterFragment)
//            tx.commit()
//            val viewInflated = LayoutInflater.from(this).inflate(R.layout.layoutDialog, view as ViewGroup?, false)

//            if (flutterView.parent != null) {
//                (flutterView.parent as ViewGroup).removeView(flutterView)
//            }
//
//            val dialog = AlertDialog.Builder(this)
//                .setView(flutterView)
//                .show()

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

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        Log.d(TAG, "Flutter Back Status: $flutterBackStatus")

        //Send back event to FlutterView if active and can go back.
        if (flutterBackStatus) {
            flutterView.popRoute()
            return
        }

        if (flutterViewVisible) {
            hideFlutterView()
        }

        Log.d(TAG, "super.onBackPressed")
        super.onBackPressed()
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

    fun hideFlutterView() {
        flutterViewVisible = false
        if (flutterView.parent != null) {
            (flutterView.parent as ViewGroup).removeView(flutterView)
        }
    }
}
