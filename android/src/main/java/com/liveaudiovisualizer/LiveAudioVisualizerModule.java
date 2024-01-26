package com.liveaudiovisualizer;

import android.media.AudioRecord;
import android.media.AudioFormat;
import android.media.MediaRecorder;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.core.DeviceEventManagerModule;

import java.util.ArrayList;
import java.util.List;

@ReactModule(name = LiveAudioVisualizerModule.NAME)
public class LiveAudioVisualizerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "LiveAudioVisualizer";
  private static final String EVENT_AUDIO_STARTED = "audioStarted";
  private static final String EVENT_VISUALIZATION_CHANGED = "VisualizationChanged";

  private AudioRecord audioRecord;
  private boolean isListening = false;
  private List<Integer> audioDataList = new ArrayList<>();
  private Handler handler = new Handler(Looper.getMainLooper());

  public LiveAudioVisualizerModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  @NonNull
  public String getName() {
    return NAME;
  }

  @ReactMethod
  public void startAudioListening(Promise promise) {
    if (!isListening) {
      startAudioCapture();
      sendEvent(EVENT_AUDIO_STARTED, null);
      promise.resolve("Audio listening started");
    } else {
      promise.reject("ALREADY_LISTENING", "Audio listening is already started");
    }
  }

  @ReactMethod
  public void stopAudioListening(Promise promise) {
    if (isListening) {
      stopAudioCapture();
      promise.resolve("Audio listening stopped");
    } else {
      promise.reject("NOT_LISTENING", "Audio listening is not started");
    }
  }

  // Add audio capture logic
  private void startAudioCapture() {
    int bufferSize = AudioRecord.getMinBufferSize(44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
    audioRecord = new AudioRecord(MediaRecorder.AudioSource.MIC, 44100, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufferSize);
    audioRecord.startRecording();
    isListening = true;

    // Start a separate thread to continuously read audio data
    new Thread(() -> {
        short[] buffer = new short[bufferSize / 2];
        while (isListening) {
            audioRecord.read(buffer, 0, buffer.length);
            processData(buffer);
        }
    }).start();
  }

  private void stopAudioCapture() {
    if (audioRecord != null) {
      isListening = false;
      audioRecord.stop();
      audioRecord.release();
      audioRecord = null;
    }
  }

  // Process audio data and send it to React Native
  private void processData(short[] buffer) {
    // For simplicity, this example just sends the average amplitude of the audio data
    int sum = 0;
    for (short sample : buffer) {
      sum += Math.abs(sample);
    }
    int averageAmplitude = sum / buffer.length;

    // Add the average amplitude to the list
    audioDataList.add(averageAmplitude);

    // Send the data to React Native every second
    handler.post(() -> {
      sendEvent(EVENT_VISUALIZATION_CHANGED, audioDataList.toString());
      audioDataList.clear();
    });
  }

  private void sendEvent(String eventName, String data) {
    getReactApplicationContext()
      .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
      .emit(eventName, data);
  }

  @ReactMethod
  public void setSensitivity(int sensitivity, Promise promise) {
    // Implement sensitivity setting logic
    promise.resolve();
  }

  @ReactMethod
  public void addEventListener(String eventName, Promise promise) {
    if (eventName.equals(EVENT_VISUALIZATION_CHANGED)) {
      promise.resolve();
    } else {
      promise.reject("INVALID_EVENT", "Invalid event name");
    }
  }
}
