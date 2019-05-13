package com.flutter.android

import android.os.Bundle
import android.util.Log
import android.view.*
import androidx.appcompat.app.AppCompatActivity;

import kotlinx.android.synthetic.main.activity_main.*
import io.flutter.facade.Flutter
import io.flutter.facade.FlutterFragment
import io.flutter.plugin.common.MethodChannel
import io.flutter.view.FlutterView

class MainActivity : AppCompatActivity() {

    var TAG = MainActivity::class.java.name

    var switchRouteCounter = 0
    var routeArray = arrayListOf("page_main", "page_transparent")
    var flutterCanGoBack: Boolean = false;
    lateinit var flutterChannel: MethodChannel;
    lateinit var flutterView: FlutterView;

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        flutterView = Flutter.createView(
            this@MainActivity,
            lifecycle,
            "page_main"
        )
        flutterView.enableTransparentBackground()

        flutterChannel = MethodChannel(flutterView, "app")
        // Receive method invocations from Dart and return results.
        flutterChannel.setMethodCallHandler { call, result ->
            when (call.method) {
                "navigation" -> {
                    when (call.arguments) {
                        "back" -> {
                            onBackPressed()
                        }
                    }
                    result.success("Hello, ${call.arguments}")
                }
                "back_status" -> {
                    try {
                        Log.d(TAG, "Back Status: ${call.arguments}")
                        flutterCanGoBack = call.arguments as Boolean
                    } catch (e: TypeCastException) {
                        Log.e(TAG, "${call.arguments}")
                    }
                }
                "baz" -> {
                    result.error("400", "This is bad", null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }

        button_1.setOnClickListener {
            //Invoke Flutter Method.
            flutterChannel.invokeMethod("page", "page_main", object: MethodChannel.Result {
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
            flutterChannel.invokeMethod("page", "page_transparent", object: MethodChannel.Result {
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

        if (flutterView.parent != null) {
            (flutterView.parent as ViewGroup).removeView(flutterView)
        }
        view_flutter_container.addView(flutterView)

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

        //Invoke Flutter Method.
//        flutterChannel.invokeMethod("navigation", "back", object: MethodChannel.Result {
//            override fun success(result: Any?) {
//                Log.i("Flutter Channel", "$result")
//                flutterView.popRoute()
//                return
//            }
//            override fun error(code: String?, msg: String?, details: Any?) {
//                Log.e("Flutter Channel", "$msg")
//            }
//            override fun notImplemented() {
//                Log.e("Flutter Channel", "Not implemented")
//            }
//        })

        if (flutterCanGoBack) {
            flutterView.popRoute()
            return
        }

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
}
