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
// Arrays
import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import android.util.Log;
import java.util.Collections;

@ReactModule(name = LiveAudioVisualizerModule.NAME)
public class LiveAudioVisualizerModule extends ReactContextBaseJavaModule {
  public static final String NAME = "LiveAudioVisualizer";
  public static final String TAG = "LiveAudioVisualizer";
  private static final String EVENT_AUDIO_STARTED = "audioStarted";
  private static final String EVENT_VISUALIZATION_CHANGED = "VisualizationChanged";
  private long lastUpdateTime = 0;
  private static final double SECONDS_PER_UPDATE = 0.1;
  private AudioRecord audioRecord;
  private boolean isListening = false;
  private List<Double> audioDataList = Collections.synchronizedList(new ArrayList<>());
  private Handler handler = new Handler(Looper.getMainLooper());
  private static final int ITEMS_PER_UPDATE = 10;

  // last 6 items before the current item
  private static final int HISTORY_SIZE = 6;
  private double[] history = new double[HISTORY_SIZE];
  private int historyIndex = 0;


  
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

  private void processData(short[] buffer) {
      Log.d(TAG, "Processing data, buffer length: " + buffer.length);

      int sum = 0;

      for (short sample : buffer) {
          sum += Math.abs(sample);
      }

      int averageAmplitude = sum / buffer.length;

      // Add the normalized amplitude to the list (scaled to the range [0, 1])
      double normalizedAmplitude = normalizeAmplitude(averageAmplitude);
      audioDataList.add(normalizedAmplitude);

      // Add the normalized amplitude to the history array (scaled to the range [0, 1])
      history[historyIndex] = normalizedAmplitude;

      // Move to the next index in the history array
      historyIndex = (historyIndex + 1) % HISTORY_SIZE;

      // Send the data to React Native every half second
      long currentTime = System.currentTimeMillis();
      if (currentTime - lastUpdateTime >= SECONDS_PER_UPDATE * 1000) {
          lastUpdateTime = currentTime;

          // Synchronize on the list to avoid race conditions
          synchronized (audioDataList) {
              // Convert amplitude values to a double array
              double[] amplitudeArray = new double[audioDataList.size() + HISTORY_SIZE];
              
              // Add the last 6 items from history to amplitudeArray
              int historyStartIndex = (historyIndex + 1) % HISTORY_SIZE;
              for (int i = 0; i < HISTORY_SIZE; i++) {
                  amplitudeArray[i] = history[(historyStartIndex + i) % HISTORY_SIZE];
              }

              // Add the items from audioDataList to amplitudeArray
              for (int i = 0; i < audioDataList.size(); i++) {
                  amplitudeArray[HISTORY_SIZE + i] = audioDataList.get(i);
              }

              // Send the amplitude array to React Native
              handler.post(() -> {
                  Log.d(TAG, "Sending event");
                  sendVisualizationEvent(amplitudeArray);
              });

              // Clear amplitude values for the next half second
              audioDataList.clear();
          }
      }
  }

  // Normalize amplitude to the range [0, 1]
  private double normalizeAmplitude(int amplitude) {
      // Choose a suitable range based on the expected maximum amplitude
      int maxAmplitude = 4000; // Adjust this based on your specific use case

      // Ensure the amplitude is within the range [0, maxAmplitude]
      amplitude = Math.max(0, Math.min(amplitude, maxAmplitude));

      // Normalize to the range [0, 1]
      return (double) amplitude / maxAmplitude;
  }


  private void sendEvent(String eventName, double[] data) {
    WritableArray writableArray = Arguments.createArray();

    if (data != null) {
        for (double value : data) {
            writableArray.pushDouble(value);
        }
    }

    getReactApplicationContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(eventName, writableArray);
}


  @ReactMethod
  public void setSensitivity(int sensitivity, Promise promise) {
    // Implement sensitivity setting logic
    promise.resolve("Sensitivity set");
  }

  private void sendVisualizationEvent(double[] data) {
    WritableArray writableArray = Arguments.createArray();
    for (double value : data) {
      writableArray.pushDouble(value);
    }

    getReactApplicationContext()
        .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
        .emit(EVENT_VISUALIZATION_CHANGED, writableArray);
  }

  @ReactMethod
  public void addEventListener(String eventName, Promise promise) {
    if (eventName.equals(EVENT_VISUALIZATION_CHANGED)) {
      promise.resolve("Event listener added");
    } else {
      promise.reject("INVALID_EVENT", "Invalid event name");
    }
  }
}
