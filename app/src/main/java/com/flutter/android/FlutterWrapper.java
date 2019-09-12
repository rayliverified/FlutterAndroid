package com.flutter.android;

import android.app.Activity;
import android.content.Context;
import android.content.res.Resources;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import java.util.ArrayList;

import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StringCodec;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterRunArguments;
import io.flutter.view.FlutterView;

public class FlutterWrapper {

    private static final String TAG = FlutterWrapper.class.getSimpleName();

    //Keep a single copy of this class in memory unless required to create a new instance explicitly.
    private static FlutterWrapper mInstance;
    private Activity mActivity;
    private Context mContext;
    private FlutterView mFlutterView;
    private MethodChannel mFlutterChannel;
    private MethodChannel.MethodCallHandler mMethodCallHandler;
    private ArrayList<FlutterCloseCallback> mFlutterCloseCallbacks = new ArrayList<>();

    private View mLoadingView;

    public boolean flutterBackBlock = false;
    public boolean flutterBackStatus = false;
    public boolean flutterViewVisible = false;
    private boolean preInitialized = false;
    private long startFlutterViewStartTime = 0;
    private long showFlutterViewStartTime = 0;
    private String flutterConfig = "";
    private String route = "";
    private String flutterData = "";
    private boolean setFlutterData = false;

    public FlutterWrapper() { }

    public static FlutterWrapper getInstance() {
        return mInstance;
    }


    public FlutterWrapper init(Context context) {
        mContext = context;
        if (mInstance == null) {
            Log.d(TAG, "Flutter Wrapper Initialization");
            mInstance = new FlutterWrapper();
        }

        return this;
    }

    public FlutterWrapper init(Context context, String flutterConfig) {
        this.flutterConfig = flutterConfig;
        return init(context);
    }

    /**
     * Initiates the Dart VM. Calling this method at an early point may help decreasing time to first
     * frame for a subsequently created {@link FlutterView}.
     *
     * @param applicationContext the application's {@link Context}
     */
    public void preInitializeFlutter(@NonNull Context applicationContext) {
        preInitialized = true;
        FlutterMain.startInitialization(applicationContext);
        FlutterMain.ensureInitializationComplete(applicationContext, null);
    }

    /**
     * Initialize FlutterView in background.
     *
     * @param activity     - activity to attach FlutterView instance to.
     * @param lifecycle    -activity lifecycle callbacks.
     * @param initialRoute - starting Flutter Module page.
     */
    public void initFlutterView(Activity activity, Lifecycle lifecycle, String initialRoute) {
        createFlutterView(activity, lifecycle, initialRoute, false);
    }

    /**
     * Initiallizes FlutterView and shows as soon as possible.
     * Displays a loading indicator during FlutterView initialization time.
     *
     * @param activity - activity to attach FlutterView instance to.
     * @param lifecycle -activity lifecycle callbacks.
     * @param initialRoute - starting Flutter Module page.
     */
    public void initFlutterViewAndShow(Activity activity, Lifecycle lifecycle, String initialRoute) {
        createFlutterView(activity, lifecycle, initialRoute, true);
    }

    /**
     * Create the FlutterView with appropriate attributes (i.e. transparent background)
     * and main MethodChannel. Displays a loading indicator if FlutterView should
     * be shown immediately. If FlutterView is already initialized, show FlutterView.
     *
     * @param activity - activity to attach FlutterView instance to.
     * @param lifecycle -activity lifecycle callbacks.
     * @param initialRoute - starting Flutter Module page.
     * @param show - flag to control whether to show FlutterView once loaded.
     */
    private void createFlutterView(Activity activity, Lifecycle lifecycle, String initialRoute, Boolean show) {
        mActivity = activity;
        //Flutter initialization takes time. Show a graceful loading indicator.
        if (show) {
            showLoadingView(activity);
        }

        if (mFlutterView != null) {
            Log.d(TAG, "Flutter View Already Initialized");
            showFlutterView(initialRoute);
            return;
        }

        startFlutterViewStartTime = System.currentTimeMillis();
        //Create the FlutterView.
        mFlutterView = createView(
                activity,
                lifecycle,
                initialRoute,
                show
        );
        mFlutterView.enableTransparentBackground();
        mFlutterChannel = new MethodChannel(mFlutterView, "app");
        // Receive method invocations from Flutter and return results.
        if (mMethodCallHandler != null) {
            mFlutterChannel.setMethodCallHandler(mMethodCallHandler);
        }
    }

    /**
     * Show active FlutterView.
     */
    public void showFlutterView() {
        showFlutterView("");
    }

    /**
     * Show active FlutterView and set route. Requires a FlutterView to
     * be active. If no FlutterView is active, this method does nothing.
     * @param route - FlutterView page to show.
     */
    public void showFlutterView(final String route) {
        Log.d(TAG, "showFlutterView");
        if (mFlutterView == null) {
            Log.d(TAG, "Flutter View Not Initialized");
            return;
        }

        // It is up to the client to not reload the same route multiple times.
        // Cannot check this.route != route because sometimes same route with different data could be reloaded.
        // This function is skipped if route passed is empty to prevent duplicate initialization.
        if (!route.isEmpty()) {
            setFlutterConfig();
            if (setFlutterData) {
                setFlutterData(FlutterWrapper.this.flutterData);
            }
            setFlutterRoute(route);
        }

        flutterViewVisible = true;
        showFlutterViewStartTime = System.currentTimeMillis();
        if (mFlutterView.getParent() != null) {
            ((ViewGroup) mFlutterView.getParent()).removeView(mFlutterView);
        }
        mActivity.addContentView(mFlutterView, getFrameLayoutParams(mActivity));
    }

    /**
     * Hide active FlutterView by removing view from parent.
     */
    public void hideFlutterView() {
        resetFlutterViewState();
        //Clear FlutterView contents.
        if (mFlutterChannel != null) {
            mFlutterChannel.invokeMethod(
                    FlutterConstants.CHANNEL_METHOD_PAGE,
                    "PAGE_BLANK", new MethodChannel.Result() {
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
                            Log.e("Flutter Channel", "Not Implemented");
                        }
                    });
        }
        if (mFlutterView != null) {
            if (mFlutterView.getParent() != null) {
                ((ViewGroup) mFlutterView.getParent()).removeView(mFlutterView);
            }
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
    private void showLoadingView(Activity activity) {
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
        activity.addContentView(mLoadingView, getFrameLayoutParams(activity));
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
                        Log.d("Flutter Data", String.valueOf(result));
                        //FlutterView has received data.
                        FlutterWrapper.this.setFlutterData = false;
                    }

                    @Override
                    public void error(String code, String message, Object details) {
                        Log.e("Flutter Data", String.valueOf(message));
                    }

                    @Override
                    public void notImplemented() {
                        Log.e("Flutter Data", "Not Implemented");
                    }
                });
            }
            //FlutterView has not been created. Set flag to pass data to FlutterView when FlutterView is eventually created.
            else {
                this.setFlutterData = true;
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
        if (mFlutterChannel == null) {
            Log.d("Set Flutter Route", "Flutter Channel Null");
            mFlutterChannel = new MethodChannel(mFlutterView, "app");
        }

        mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_PAGE, route, new MethodChannel.Result() {
            @Override
            public void success(Object result) {
                Log.d("Flutter Channel", String.valueOf(result));
                FlutterWrapper.this.route = route;
            }

            @Override
            public void error(String code, String message, Object details) {
                Log.e("Flutter Channel", String.valueOf(message));
            }

            @Override
            public void notImplemented() {
                Log.e("Flutter Channel", "Not Implemented");
            }
        });
    }

    /**
     * Pop FlutterView route.
     */
    public void popRoute() {
        //Safety catch to reset flags if FlutterView has not been initialized.
        if (mFlutterView == null) {
            Log.d(TAG, "Pop Route Null");
            flutterBackStatus = false;
            flutterViewVisible = false;
            return;
        }

        mFlutterView.popRoute();
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
        if (mFlutterView == null) {
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

    public boolean isFlutterViewVisible() {
        return flutterViewVisible;
    }

    public void setFlutterViewVisible(boolean flutterViewVisible) {
        this.flutterViewVisible = flutterViewVisible;
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
     * Initialize FlutterView with host app configuration variables.
     */
    private void setFlutterConfig() {
        if (!flutterConfig.isEmpty() && mFlutterChannel != null) {
            mFlutterChannel.invokeMethod(FlutterConstants.CHANNEL_METHOD_CONFIG, flutterConfig, new MethodChannel.Result() {
                @Override
                public void success(Object result) {
                    Log.d("Flutter Config", String.valueOf(result));
                }

                @Override
                public void error(String code, String message, Object details) {
                    Log.e("Flutter Config", String.valueOf(message));
                }

                @Override
                public void notImplemented() {
                    Log.e("Flutter Config", "Not Implemented");
                }
            });
        }
    }

    /**
     * Creates a {@link FlutterView} linked to the specified {@link Activity} and {@link Lifecycle}.
     * The optional initial route string will be made available to the Dart code (via
     * {@code window.defaultRouteName}) and may be used to determine which widget should be displayed
     * in the view. The default initialRoute is "/".
     *
     * @param activity     an {@link Activity}
     * @param lifecycle    a {@link Lifecycle}
     * @param initialRoute an initial route {@link String}, or null
     * @return a {@link FlutterView}
     */
    @NonNull
    private FlutterView createView(@NonNull final Activity activity, @NonNull final Lifecycle lifecycle, final String initialRoute, final Boolean show) {
        FlutterMain.startInitialization(activity.getApplicationContext());
        FlutterMain.ensureInitializationComplete(activity.getApplicationContext(), null);
        final FlutterNativeView nativeView = new FlutterNativeView(activity);
        mFlutterView = new FlutterView(activity, null, nativeView) {
            private final BasicMessageChannel<String> lifecycleMessages = new BasicMessageChannel<>(this, "flutter/lifecycle", StringCodec.INSTANCE);
            boolean firstStart = true;

            @Override
            public void onFirstFrame() {
                super.onFirstFrame();
                setAlpha(1.0f);
                Log.d(TAG, "onFirstFrame");
                Log.d("Benchmark", "showFlutterView: " + (System.currentTimeMillis() - showFlutterViewStartTime));
                showFlutterViewStartTime = 0;
                //FlutterView is visible to the user. Hide LoadingView.
                hideLoadingView();
            }

            @Override
            public void onPostResume() {
                // Overriding default behavior to avoid dictating system UI via PlatformPlugin.
                lifecycleMessages.send("AppLifecycleState.resumed");
            }

            //Hook into FlutterView lifecycle method to listen to initialization complete event.
            @Override
            public void onStart() {
                super.onStart();
                Log.d(TAG, "onStart");
                if (firstStart) {
                    firstStart = false;
                    Log.d("Benchmark", "startFlutterView: " + (System.currentTimeMillis() - startFlutterViewStartTime));
                    startFlutterViewStartTime = 0;
                    //FlutterView has loaded, set initialization variables.
                    setFlutterConfig();
                    if (setFlutterData) {
                        setFlutterData(FlutterWrapper.this.flutterData);
                    }
                    Log.d("Flutter Config: ", flutterConfig);
                    Log.d("Flutter Data: ", flutterData);
                    setFlutterRoute(initialRoute);
                    //Called by initFlutterViewAndShow() to display FlutterView after initialization.
                    if (show) {
                        showFlutterView();
                    }
                }
            }
        };
        if (initialRoute != null) {
            mFlutterView.setInitialRoute(initialRoute);
        }
        lifecycle.addObserver(new LifecycleObserver() {
            @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
            public void onCreate() {
                final FlutterRunArguments arguments = new FlutterRunArguments();
                arguments.bundlePath = FlutterMain.findAppBundlePath(activity.getApplicationContext());
                arguments.entrypoint = "main";
                mFlutterView.runFromBundle(arguments);
                GeneratedPluginRegistrant.registerWith(mFlutterView.getPluginRegistry());
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_START)
            public void onStart() {
                if (mFlutterView != null) {
                    mFlutterView.onStart();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            public void onResume() {
                if (mFlutterView != null) {
                    mFlutterView.onPostResume();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            public void onPause() {
                if (mFlutterView != null) {
                    mFlutterView.onPause();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                if (mFlutterView != null) {
                    mFlutterView.onStop();
                }
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                if (mFlutterView != null) {
                    mFlutterView.destroy();
                }
            }
        });
        mFlutterView.setAlpha(0f);
        return mFlutterView;
    }

    /**
     * Destory FlutterView.
     * Destroying a FlutterView is the same as hiding a FlutterView and then destroying it.
     *
     * @return - true if FlutterView was successfully destoryed. False if view is null and already destroyed.
     */
    public boolean destoryView() {
        resetFlutterViewState();
        if (mFlutterView != null) {
            mFlutterView.destroy();
            mFlutterView = null;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Reset FlutterWrapper state variables.
     */
    private void resetFlutterViewState() {
        flutterViewVisible = false;
        flutterBackStatus = false;
        flutterBackBlock = false;
    }

    private FrameLayout.LayoutParams getFrameLayoutParams(Activity activity) {
        FrameLayout.LayoutParams frameLayoutParams;
        //FrameLayoutParams used to add FlutterView and LoadingView to the view hierarchy.
        frameLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );

        //Set top margin status bar and safe area.
        int topMargin = getStatusBarHeight();
        //If device has display insets, set top margin to display inset instead of status bar height.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && activity.getWindow() != null) {
            WindowInsets windowInsets = activity.getWindow().getDecorView().getRootWindowInsets();
            if (windowInsets != null && windowInsets.getDisplayCutout() != null) {
                topMargin = windowInsets.getDisplayCutout().getSafeInsetTop();
            }
        }
//        frameLayoutParams.topMargin = topMargin;

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