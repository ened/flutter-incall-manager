package com.cloudwebrtc.flutterincallmanager;

import android.app.Activity;
import android.content.Context;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.BinaryMessenger;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.PluginRegistry.Registrar;

// import android.media.AudioAttributes; // --- for API 21+

/** FlutterIncallManagerPlugin */
public class FlutterIncallManagerPlugin implements FlutterPlugin, ActivityAware {

  private MethodChannel channel;
  private MethodCallHandlerImpl methodCallHandler;

  private EventChannel.EventSink eventSink = null;

  private EventChannel.StreamHandler streamHandler =
      new EventChannel.StreamHandler() {
        @Override
        public void onListen(Object o, EventChannel.EventSink sink) {
          eventSink = sink;
        }

        @Override
        public void onCancel(Object o) {
          eventSink = null;
        }
      };

  public static void registerWith(Registrar registrar) {
    FlutterIncallManagerPlugin plugin = new FlutterIncallManagerPlugin();
    plugin.startListening(registrar.context(), registrar.messenger());

    if (registrar.activity() != null) {
      plugin.onActivityChanged(registrar.activity());
    }

    registrar.addViewDestroyListener(
        view -> {
          plugin.stopListening();
          return false;
        });
  }

  @Override
  public void onAttachedToEngine(@NonNull FlutterPluginBinding binding) {
    startListening(binding.getApplicationContext(), binding.getBinaryMessenger());
  }

  @Override
  public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
    stopListening();
  }

  @Override
  public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
    onActivityChanged(binding.getActivity());
  }

  @Override
  public void onDetachedFromActivityForConfigChanges() {
    onActivityChanged(null);
  }

  @Override
  public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
    onActivityChanged(binding.getActivity());
  }

  @Override
  public void onDetachedFromActivity() {
    onActivityChanged(null);
  }

  private void onActivityChanged(@Nullable Activity activity) {
    methodCallHandler.setActivity(activity);
  }

  private void startListening(Context applicationContext, BinaryMessenger messenger) {
    EventChannel eventChannel = new EventChannel(messenger, "FlutterInCallManager.Event");
    eventChannel.setStreamHandler(streamHandler);

    methodCallHandler =
        new MethodCallHandlerImpl(
            applicationContext,
            data -> {
              if (eventSink != null) {
                eventSink.success(data);
              }
            });
    channel = new MethodChannel(messenger, "FlutterInCallManager.Method");
    channel.setMethodCallHandler(methodCallHandler);
  }

  private void stopListening() {
    channel.setMethodCallHandler(null);
    methodCallHandler.setActivity(null);
    methodCallHandler = null;
  }
}
