package com.flutter.android

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.TypeCastException
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodCall
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T



class MainActivity : AppCompatActivity() {

    var TAG = MainActivity::class.java.name

    lateinit var mFlutterWrapper: FlutterWrapper

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        mFlutterWrapper = FlutterWrapper()
//        mFlutterWrapper.preInitializeFlutter(this)
        mFlutterWrapper.initFlutterView(this@MainActivity, lifecycle, "PAGE_MAIN")
        mFlutterWrapper.methodCallHandler = MethodChannel.MethodCallHandler { methodCall, result ->
            println("Method Call");
            when (methodCall.method) {
                FlutterConstants.CHANNEL_METHOD_NAVIGATION -> {
                    println("CHANNEL_METHOD_NAVIGATION");
                    when (methodCall.arguments as String) {
                        FlutterConstants.NAVIGATION_CLOSE -> mFlutterWrapper.hideFlutterView()
                    }
                    result.success(methodCall.method + methodCall.arguments)
                }
                FlutterConstants.CHANNEL_METHOD_BACK_STATUS -> {
                    println("CHANNEL_METHOD_BACK_STATUS")
                    try {
                        mFlutterWrapper.flutterBackStatus = methodCall.arguments as Boolean
                    } catch (e: TypeCastException) {
                        Log.e(TAG, methodCall.arguments.toString())
                    }

                    result.success(methodCall.method + methodCall.arguments)
                }
                else -> result.notImplemented()
            }
        }

        button_1.setOnClickListener {
            mFlutterWrapper.initFlutterViewAndShow(this@MainActivity, lifecycle, "PAGE_MAIN")
        }

        button_2.setOnClickListener {
            mFlutterWrapper.showFlutterView("PAGE_DELETE_ACCOUNT")
        }

        fab.setOnClickListener { view ->

        }
    }

    override fun onBackPressed() {
        Log.d(TAG, "onBackPressed")
        Log.d(TAG, "Flutter Back Status: ${mFlutterWrapper.flutterBackStatus}")

        //Send back event to FlutterView if active and has routes that can be popped.
        if (mFlutterWrapper.flutterBackStatus) {
            mFlutterWrapper.popRoute()
            return
        }

        //Hide FlutterView if visible. Usually means FlutterView cannot be popped and is at the top navigation level.
        if (mFlutterWrapper.flutterViewVisible) {
            mFlutterWrapper.hideFlutterView()
            return
        }

        Log.d(TAG, "super.onBackPressed")
        super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * FlutterView Initialization Methods.
     */

    //Add FlutterView to dialog view.
//            if (flutterView.parent != null) {
//                (flutterView.parent as ViewGroup).removeView(flutterView)
//            }
//
//            val dialog = AlertDialog.Builder(this)
//                .setView(flutterView)
//                .show()

    //Add FlutterView to activity layout hierarchy.
//            val layout = FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
//            addContentView(flutterView, layout)

    //Add Flutter Fragment.
//            val tx = supportFragmentManager.beginTransaction()
//            val flutterFragment = Flutter.createFragment("page_transparent")
//            tx.replace(R.id.fragment_container, flutterFragment)
//            tx.commit()

    //Add FlutterView to ViewStub.
    //        if (flutterView.parent != null) {
//            (flutterView.parent as ViewGroup).removeView(flutterView)
//        }
//        view_flutter_container.addView(flutterView)
}
