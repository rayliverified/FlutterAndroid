package com.flutter.android;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import io.flutter.plugin.common.BasicMessageChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.StringCodec;
import io.flutter.plugins.GeneratedPluginRegistrant;
import io.flutter.view.FlutterMain;
import io.flutter.view.FlutterNativeView;
import io.flutter.view.FlutterRunArguments;
import io.flutter.view.FlutterView;
import kotlin.TypeCastException;

public class FlutterWrapper {

    private static final String TAG = FlutterWrapper.class.getSimpleName();

    //Keep a single copy of this in memory unless required to create a new instance explicitly.
    private static FlutterWrapper mInstance;
    public boolean flutterBackStatus = false;
    public boolean flutterViewVisible = false;
    public long startTime = 0;
    private Activity mActivity;
    private Context mContext;
    private FlutterView mFlutterView;
    private MethodChannel mFlutterChannel;
    private View mLoadingView;
    private FrameLayout.LayoutParams mFrameLayoutParams;

    public FlutterWrapper() {
        mFrameLayoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT
        );
    }

    public static FlutterWrapper getInstance() {
        return mInstance;
    }

    /**
     * Initiates the Dart VM. Calling this method at an early point may help decreasing time to first
     * frame for a subsequently created {@link FlutterView}.
     *
     * @param applicationContext the application's {@link Context}
     */
    public static void startInitialization(@NonNull Context applicationContext) {
        FlutterMain.startInitialization(applicationContext);
        FlutterMain.ensureInitializationComplete(applicationContext, null);
    }

    public FlutterWrapper init(Context context) {
        mContext = context;
        if (mInstance == null) {
            Log.d(TAG, "Flutter Wrapper Initialization");
            mInstance = new FlutterWrapper();
            startInitialization(context);
        }

        return this;
    }

    public void initFlutterView(Activity activity, Lifecycle lifecycle, String initialRoute) {
        createFlutterView(activity, lifecycle, initialRoute, false);
    }

    public void initFlutterViewAndShow(Activity activity, Lifecycle lifecycle, String initialRoute) {
        createFlutterView(activity, lifecycle, initialRoute, true);
    }

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

        //Create the FlutterView.
        mFlutterView = createView(
                activity,
                lifecycle,
                initialRoute,
                show
        );
        mFlutterView.enableTransparentBackground();
        mFlutterChannel = new MethodChannel(mFlutterView, "app");
        // Receive method invocations from Dart and return results.
        mFlutterChannel.setMethodCallHandler(new MethodChannel.MethodCallHandler() {
            @Override
            public void onMethodCall(MethodCall methodCall, MethodChannel.Result result) {
                switch (methodCall.method) {
                    case FlutterConstants.CHANNEL_METHOD_NAVIGATION:
                        switch ((String) methodCall.arguments) {
                            case FlutterConstants.NAVIGATION_CLOSE:
                                hideFlutterView();
                                break;
                        }
                        result.success(methodCall.method + methodCall.arguments);
                        break;
                    case FlutterConstants.CHANNEL_METHOD_BACK_STATUS:
                        try {
                            flutterBackStatus = (Boolean) methodCall.arguments;
                        } catch (TypeCastException e) {
                            Log.e(TAG, String.valueOf(methodCall.arguments));
                        }
                        result.success(methodCall.method + methodCall.arguments);
                        break;
                    default:
                        result.notImplemented();
                        break;
                }
            }
        });
    }

    public void showFlutterView() {
        showFlutterView("");
    }

    public void showFlutterView(String route) {
        Log.d(TAG, "Show Flutter View");
        if (mFlutterView == null) {
            Log.d(TAG, "Flutter View Not Initialized");
            return;
        }

        if (!route.isEmpty()) {
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
                    Log.e("Flutter Channel", "Not Implemented");
                }
            });
        }

        flutterViewVisible = true;
        startTime = System.currentTimeMillis();
        mActivity.addContentView(mFlutterView, mFrameLayoutParams);
    }

    public void hideFlutterView() {
        flutterViewVisible = false;
        flutterBackStatus = false;
        //Clear FlutterView contents.
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
        if (mFlutterView.getParent() != null) {
            ((ViewGroup) mFlutterView.getParent()).removeView(mFlutterView);
        }
    }

    private void showLoadingView(Activity activity) {
        if (mLoadingView == null) {
            mLoadingView = LayoutInflater.from(activity.getApplicationContext()).inflate(R.layout.layout_flutter_loading, null);
            FrameLayout loadingLayout = mLoadingView.findViewById(R.id.layout_loading);
            loadingLayout.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.d(TAG, "Click Loading Layout Hidden");
                    mLoadingView.setVisibility(View.GONE);
                }
            });
        }
        activity.addContentView(mLoadingView, mFrameLayoutParams);
        if (mLoadingView.getVisibility() == View.GONE) {
            mLoadingView.setVisibility(View.VISIBLE);
        }
    }

    private void hideLoadingView() {
        if (mLoadingView != null && mLoadingView.getParent() != null) {
            ((ViewGroup) mLoadingView.getParent()).removeView(mLoadingView);
        }
    }

    public void popRoute() {
        if (mFlutterView == null) {
            Log.d(TAG, "Flutter View Null");
            flutterBackStatus = false;
            return;
        }

        mFlutterView.popRoute();
    }

    public MethodChannel getFlutterChannel() {
        if (mFlutterView == null) {
            return null;
        }
        if (mFlutterChannel == null) {
            mFlutterChannel = new MethodChannel(mFlutterView, "app");
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

    public boolean isFlutterViewVisible() {
        return flutterViewVisible;
    }

    public void setFlutterViewVisible(boolean flutterViewVisible) {
        this.flutterViewVisible = flutterViewVisible;
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
    private FlutterView createView(@NonNull final Activity activity, @NonNull final Lifecycle lifecycle, final String initialRoute, Boolean show) {
        FlutterMain.startInitialization(activity.getApplicationContext());
        FlutterMain.ensureInitializationComplete(activity.getApplicationContext(), null);
        final FlutterNativeView nativeView = new FlutterNativeView(activity);
        mFlutterView = new FlutterView(activity, null, nativeView) {
            private final BasicMessageChannel<String> lifecycleMessages = new BasicMessageChannel<>(this, "flutter/lifecycle", StringCodec.INSTANCE);
            boolean showFirstTime = true;

            @Override
            public void onPostResume() {
                // Overriding default behavior to avoid dictating system UI via PlatformPlugin.
                lifecycleMessages.send("AppLifecycleState.resumed");
            }

            //Hook into FlutterView lifecycle method to listen to initialization complete event.
            @Override
            public void onStart() {
                super.onStart();
                Log.d(TAG, "Flutter View onStart");
                if (show && showFirstTime) {
                    showFirstTime = false;
                    showFlutterView();
                }
            }

            @Override
            public void onFirstFrame() {
                super.onFirstFrame();
                setAlpha(1.0f);
                Log.d(TAG, "Flutter View onFirstFrame");
                Log.d("Benchmark", String.valueOf(System.currentTimeMillis() - startTime));
                hideLoadingView();
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
                mFlutterView.onStart();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
            public void onResume() {
                mFlutterView.onPostResume();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
            public void onPause() {
                mFlutterView.onPause();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
            public void onStop() {
                mFlutterView.onStop();
            }

            @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
            public void onDestroy() {
                mFlutterView.destroy();
            }
        });
        mFlutterView.setAlpha(0f);
        return mFlutterView;
    }
}
