# react-native-live-audio-visualizer

A powerful and customizable real-time audio visualizer component for React Native. This package allows developers to integrate dynamic and visually engaging audio visualizations into their React Native applications, enhancing the user experience with live audio feedback.

## Installation

```sh
npm install react-native-live-audio-visualizer
```

## Demo
[![Demo Video](demo.mp4)](demo.mp4)


## Usage

```ts
import * as React from 'react';
import { DeviceEventEmitter, StyleSheet, View } from 'react-native';
import {
  RequestAudioPermission,
  startAudioListening,
  stopAudioListening,
} from 'react-native-live-audio-visualizer';

export default function App() {
  const [result, setResult] = React.useState<number[]>([]);

  React.useEffect(() => {
    RequestAudioPermission(
      'This app needs audio permission',
      'Permission for audio',
      'OK'
    );

    startAudioListening().then(() => {
      console.log('Audio listening started');
    });

    DeviceEventEmitter.addListener('VisualizationChanged', (data) => {
      console.log('Event received:', data);

      setResult(data);
    });

    return () => {
      stopAudioListening().then(() => {
        console.log('Audio listening stopped');
      });
    };
  }, []);

  return (
    <View style={styles.container}>
      <View style={styles.waveContainer}>
        {result.map((wave, index) => (
          <View key={index} style={[styles.waveItem, { height: wave * 100 }]} />
        ))}
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    backgroundColor: 'black',
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
  waveContainer: {
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    flexDirection: 'row',
  },
  box: {
    width: 60,
    height: 60,
    marginVertical: 20,
  },
  waveItem: {
    width: 12,
    backgroundColor: '#B692F6',
    marginHorizontal: 1,
    borderRadius: 4,
    minHeight: 10,
    marginRight: 8,
  },
});
```