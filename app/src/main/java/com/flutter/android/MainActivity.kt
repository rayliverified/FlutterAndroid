package com.flutter.android

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    var TAG = MainActivity::class.java.name

    lateinit var mFlutterWrapper: FlutterWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mFlutterWrapper = FlutterWrapper()
        mFlutterWrapper.initFlutterView(this@MainActivity, lifecycle, "PAGE_MAIN")

        button_1.setOnClickListener {
            mFlutterWrapper.showFlutterView(this@MainActivity, lifecycle, "PAGE_MAIN")
        }

        button_2.setOnClickListener {
            mFlutterWrapper.showFlutterView("PAGE_DELETE_ACCOUNT")
        }

//
//        button_1.setOnClickListener {
//            //Invoke Flutter Method.
//            flutterChannel.invokeMethod(CHANNEL_METHOD_PAGE, "PAGE_MAIN", object : MethodChannel.Result {
//                override fun success(result: Any?) {
//                    Log.i("Flutter Channel", "$result")
//                }
//
//                override fun error(code: String?, msg: String?, details: Any?) {
//                    Log.e("Flutter Channel", "$msg")
//                }
//
//                override fun notImplemented() {
//                    Log.e("Flutter Channel", "Not implemented")
//                }
//            })
//        }
//
//        button_2.setOnClickListener {
//            //Invoke Flutter Method.
//            flutterChannel.invokeMethod(CHANNEL_METHOD_PAGE, "PAGE_TRANSPARENT", object : MethodChannel.Result {
//                override fun success(result: Any?) {
//                    Log.i("Flutter Channel", "$result")
//                }
//
//                override fun error(code: String?, msg: String?, details: Any?) {
//                    Log.e("Flutter Channel", "$msg")
//                }
//
//                override fun notImplemented() {
//                    Log.e("Flutter Channel", "Not implemented")
//                }
//            })
//        }
//
//        button_3.setOnClickListener {
//            flutterViewVisible = true
//            flutterChannel.invokeMethod(
//                CHANNEL_METHOD_PAGE,
//                "PAGE_DELETE_ACCOUNT",
//                object : MethodChannel.Result {
//                    override fun success(result: Any?) {
//                        Log.i("Flutter Channel", "$result")
//                    }
//
//                    override fun error(code: String?, msg: String?, details: Any?) {
//                        Log.e("Flutter Channel", "$msg")
//                    }
//
//                    override fun notImplemented() {
//                        Log.e("Flutter Channel", "Not implemented")
//                    }
//                })
//
//            if (flutterView.parent != null) {
//                (flutterView.parent as ViewGroup).removeView(flutterView)
//            }
//            val layout = FrameLayout.LayoutParams(
//                FrameLayout.LayoutParams.MATCH_PARENT,
//                FrameLayout.LayoutParams.MATCH_PARENT
//            )
//            addContentView(flutterView, layout)
//        }

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
        Log.d(TAG, "Flutter Back Status: ${mFlutterWrapper.flutterBackStatus}")

        //Send back event to FlutterView if active and can go back.
        if (mFlutterWrapper.flutterBackStatus) {
            mFlutterWrapper.popRoute()
            return
        }

        if (mFlutterWrapper.flutterViewVisible) {
            mFlutterWrapper.hideFlutterView()
            return
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
}
