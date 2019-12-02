package com.flutter.android;

import android.content.Context;
import android.content.res.Resources;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;

import java.util.ArrayList;

import io.flutter.embedding.android.FlutterFragment;
import io.flutter.embedding.android.FlutterView;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.embedding.engine.FlutterEngineCache;
import io.flutter.embedding.engine.dart.DartExecutor;
import io.flutter.plugin.common.MethodChannel;

public class FlutterWrapperV2 {

    private static final String TAG = FlutterWrapperV2.class.getSimpleName();
    private static final String FLUTTER_ENGINE = "FLUTTER_ENGINE"; //Flutter Engine tag.
    private static final String FLUTTER_FRAGMENT = "FLUTTER_FRAGMENT";

    //Keep a single copy of this class in memory unless required to create a new instance explicitly.
    private static FlutterWrapperV2 mInstance;
    private AppCompatActivity mActivity;
    private Context mContext;
    private FragmentManager mFragmentManager;
    private FlutterEngine mFlutterEngine;
    private FlutterFragment mFlutterFragment;
    private MethodChannel mFlutterChannel;
    private MethodChannel.MethodCallHandler mMethodCallHandler;
    private ArrayList<FlutterCloseCallback> mFlutterCloseCallbacks = new ArrayList<>();

    private View mLoadingView;

    private boolean flutterBackBlock = true;
    private boolean flutterBackStatus = false;
    private boolean flutterVisible = false;
    private String flutterConfig = "";
    private String flutterData = "";

    public FlutterWrapperV2() { }

    public static FlutterWrapperV2 getInstance() {
        return mInstance;
    }

    public FlutterWrapperV2 init(AppCompatActivity activity) {
        mActivity = activity;
        mContext = activity.getApplicationContext();
        Log.d(TAG, "Flutter Wrapper Init");
        if (mInstance == null) {
            Log.d(TAG, "Flutter Wrapper Initial Init");
            mInstance = new FlutterWrapperV2();
        }
        //Initialize Fragment Manager.
        if (mFragmentManager == null) {
            mFragmentManager = mActivity.getSupportFragmentManager();
        }
        //Initialize Flutter Engine.
        initFlutterEngine();
        //Initialize Flutter Channel.
        initFlutterChannel();
        //Set Flutter
        //Initialize Flutter Fragment.
        mFlutterFragment = (FlutterFragment) mFragmentManager.findFragmentByTag(FLUTTER_FRAGMENT);
        if (mFlutterFragment == null) {
            mFlutterFragment = FlutterFragment.withCachedEngine(FLUTTER_ENGINE)
                    .transparencyMode(FlutterView.TransparencyMode.transparent)
                    .build();
        }

        return this;
    }

    public FlutterWrapperV2 init(AppCompatActivity activity, String flutterConfig) {
        this.flutterConfig = flutterConfig;
        return init(activity);
    }

    /**
     * Create a Flutter Engine instance.
     *
     * Create a prewarmed Flutter Engine and add to cache.
     * @return Flutter Engine.
     */
    public FlutterEngine initFlutterEngine() {
        if (mFlutterEngine == null) {
            mFlutterEngine = new FlutterEngine(mContext);
            mFlutterEngine.getDartExecutor().executeDartEntrypoint(DartExecutor.DartEntrypoint.createDefault());
            FlutterEngineCache.getInstance().put(FLUTTER_ENGINE, mFlutterEngine);
        }

        return mFlutterEngine;
    }

    public FlutterEngine getFlutterEngine() {
        return mFlutterEngine;
    }

    /**
     * Show active Flutter Fragment.
     */
    public void showFlutter() {
        showFlutter("");
    }

    /**
     * Show active FlutterView and set route. Requires a FlutterView to
     * be active. If no FlutterView is active, this method does nothing.
     * @param route - FlutterView page to show.
     */
    public void showFlutter(final String route) {
        Log.d(TAG, "showFlutter");
        if (mFlutterFragment == null) {
            Log.d(TAG, "Flutter Fragment not initialized.");
            return;
        }

        // It is up to the client to not reload the same route multiple times.
        // Cannot check this.route != route because sometimes same route with different data could be reloaded.
        // This function is skipped if route passed is empty to prevent duplicate initialization.
        if (!route.isEmpty()) {
            setMethodChannel();
            setFlutterConfig();
            setFlutterData(flutterData);
            setFlutterRoute(route);
        }

        if (!flutterVisible) {
            flutterVisible = true;
            if (mFragmentManager.findFragmentByTag(FLUTTER_FRAGMENT) == null) {
                mFragmentManager
                        .beginTransaction()
                        .add(android.R.id.content, mFlutterFragment, FLUTTER_FRAGMENT)
                        .commit();
            }
        }
    }

    /**
     * Hide active FlutterView by removing view from parent.
     */
    public void hideFlutter() {
        Log.d(TAG, "hideFlutter");
        resetFlutterViewState();
        if (mFragmentManager.findFragmentByTag(FLUTTER_FRAGMENT) != null) {
            //noinspection ConstantConditions
            mFragmentManager.beginTransaction().remove(mFragmentManager.findFragmentByTag(FLUTTER_FRAGMENT)).commit();
        }
        //Notify listeners that FlutterView has closed.
        for (FlutterCloseCallback flutterCloseCallback : mFlutterCloseCallbacks) {
            flutterCloseCallback.onClose();
        }
        clearFlutterCloseCallbacks();
    }

    /**
     * Show a loading indicator while the FlutterView is initializing.
     *
     * @param activity - activity to attach LoadingView instance to.
     */
    private void showLoadingView(AppCompatActivity activity) {
        //Create the LoadingView layout.
        if (mLoadingView == null) {
            mLoadingView = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.layout_flutter_loading, null);
            //Error fallback to manually close loading indicator on click.
            FrameLayout loadingLayout = mLoadingView.findViewById(R.id.layout_loading);
            loadingLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "onClick LoadingView Hidden");
                    mLoadingView.setVisibility(View.GONE);
                }
            });
        }
        if (mLoadingView.getParent() != null) {
            ((ViewGroup) mLoadingView.getParent()).removeView(mLoadingView);
        }
        activity.addContentView(mLoadingView, getFrameLayoutParams());
        if (mLoadingView.getVisibility() == View.GONE) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * Hide LoadingView by removing layout from parent.
     */
    private void hideLoadingView() {
        if (mLoadingView != null && mLoadingView.getParent() != null) {
            ((ViewGroup) mLoadingView.getParent()).removeView(mLoadingView);
        }
    }

    /**
     * Send data to Flutter module.
     *
     * Immediately send data if Flutter module has been created.
     * Otherwise, send data as soon as FlutterView is created.
     * @param flutterData - data to pass to Flutter module as a JSON string.
     */
    public void setFlutterData(String flutterData) {
        this.flutterData = flutterData;

        //Do not send FlutterData if data does not exist.
        if (flutterData != null ) {
            if (mFlutterChannel != null) {
                mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_DATA, this.flutterData, new MethodChannel.Result() {
                    @Override
                    public void success(Object result) {
                        Log.d("setFlutterData", String.valueOf(result));
                    }

                    @Override
                    public void error(String code, String message, Object details) {
                        Log.e("setFlutterData", String.valueOf(message));
                    }

                    @Override
                    public void notImplemented() {
                        Log.e("setFlutterData", "Not Implemented");
                    }
                });
            } else {
                Log.e("setFlutterData", "Flutter Channel not initialized.");
            }
        }
    }

    /**
     * Update Flutter Route.
     *
     * Flutter Navigation update route.
     * @param route - route to navigate to.
     */
    public void setFlutterRoute(String route) {
        Log.d(TAG, "Update Flutter Route: " + route);
        initFlutterChannel();

        mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_PAGE, route, new MethodChannel.Result() {
            @Override
            public void success(Object result) {
                Log.d("Flutter Channel", String.valueOf(result));
            }

            @Override
            public void error(String code, String message, Object details) {
                Log.e("Flutter Channel", String.valueOf(message));
            }

            @Override
            public void notImplemented() {
                Log.e("Flutter Route", "Not Implemented");
            }
        });
    }

    /**
     * Send Android back event to Flutter.
     */
    public void sendBackEvent() {
        if (mFlutterChannel != null) {
            mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_NAVIGATION, FlutterConstants.NAVIGATION_BACK, new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    Log.d("Flutter Back Event", String.valueOf(result));
                }

                @Override
                public void error(String code, String message, Object details) {
                    Log.e("Flutter Back Event", String.valueOf(message));
                }

                @Override
                public void notImplemented() {
                    Log.e("Flutter Back Event", "Not Implemented");
                }
            });
        }
    }

    // BEGIN: Getters and Setters.
    public MethodChannel.MethodCallHandler getMethodCallHandler() {
        return mMethodCallHandler;
    }

    public void setMethodCallHandler(MethodChannel.MethodCallHandler mMethodCallHandler) {
        this.mMethodCallHandler = mMethodCallHandler;
    }

    /**
     * Returns the current MethodChannel attached to the FlutterView.
     *
     * @return - MethodChannel attached to the active FlutterView.
     * Returns null if no FlutterView or MethodChannel has been created.
     */
    public MethodChannel getFlutterChannel() {
        if (mFlutterChannel == null) {
            return null;
        }

        return mFlutterChannel;
    }

    public void setFlutterChannel(MethodChannel mFlutterChannel) {
        this.mFlutterChannel = mFlutterChannel;
    }

    public boolean isFlutterBackStatus() {
        return flutterBackStatus;
    }

    public void setFlutterBackStatus(boolean flutterBackStatus) {
        this.flutterBackStatus = flutterBackStatus;
    }

    public boolean isFlutterBackBlock() {
        return flutterBackBlock;
    }

    public void setFlutterBackBlock(boolean flutterBackBlock) {
        this.flutterBackBlock = flutterBackBlock;
    }

    public boolean isFlutterVisible() {
        return flutterVisible;
    }

    public void setFlutterVisible(boolean flutterVisible) {
        this.flutterVisible = flutterVisible;
    }

    public String getFlutterConfig() {
        return flutterConfig;
    }

    public void setFlutterConfig(String flutterConfig) {
        this.flutterConfig = flutterConfig;
        //Update Flutter module with new initialization variables.
        setFlutterConfig();
    }
    // END: Getters and Setters.

    /**
     * Initialize FlutterChannel if not initialized.
     */
    private void initFlutterChannel() {
        if (mFlutterChannel == null) {
            Log.d("initFlutterChannel", "Initialize Flutter Channel.");
            if (mFlutterEngine != null) {
                mFlutterChannel = new MethodChannel(mFlutterEngine.getDartExecutor(), "app");
            } else {
                Log.e("initFlutterChannel", "Flutter Engine not initialized.");
            }
        }
    }

    /**
     * Attach native method channel to Flutter.
     *
     * Native method channel handles invocations from Flutter
     * to the native app. Create and add to Flutter Method Channel.
     */
    private void setMethodChannel() {
        // Receive method invocations from Flutter and return results.
        if (mFlutterChannel == null) {
            Log.e("setMethodChannel", "Flutter Channel not initialized.");
            return;
        }
        if (mMethodCallHandler != null) {
            mFlutterChannel.setMethodCallHandler(mMethodCallHandler);
        } else {
            Log.e("setMethodChannel", "Flutter Method Call Handler not set.");
        }
    }

    /**
     * Initialize FlutterView with host app configuration variables.
     */
    private void setFlutterConfig() {
        if (!flutterConfig.isEmpty() && mFlutterChannel != null) {
            mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_CONFIG, flutterConfig, new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    Log.d("setFlutterConfig", String.valueOf(result));
                }

                @Override
                public void error(String code, String message, Object details) {
                    Log.e("setFlutterConfig", String.valueOf(message));
                }

                @Override
                public void notImplemented() {
                    Log.e("setFlutterConfig", "Not Implemented");
                }
            });
        } else {
            Log.e("setFlutterConfig", "Unable to set Flutter Config.");
        }
    }

    /**
     * Reset FlutterWrapper state variables.
     */
    private void resetFlutterViewState() {
        flutterVisible = false;
        flutterBackStatus = false;
        flutterBackBlock = true;
    }

    private FrameLayout.LayoutParams getFrameLayoutParams() {
        FrameLayout.LayoutParams frameLayoutParams;
        //FrameLayoutParams used to add FlutterView and LoadingView to the view hierarchy.
        frameLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        return frameLayoutParams;
    }

    /**
     * Converts dp to pixels.
     */
    private int getStatusBarHeight() {
        return (int) (24 * Resources.getSystem().getDisplayMetrics().density);
    }

    /**
     * Add FlutterCloseCallback listener to be called when FlutterView is closed.
     *
     * @param flutterCloseCallback - callback to add.
     */
    public void addFlutterCloseCallback(FlutterCloseCallback flutterCloseCallback) {
        mFlutterCloseCallbacks.add(flutterCloseCallback);
    }

    /**
     * Clear close callback list.
     */
    public void clearFlutterCloseCallbacks() {
        mFlutterCloseCallbacks.clear();
    }
}