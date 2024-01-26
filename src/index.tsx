import { NativeModules } from 'react-native';

const LiveAudioVisualizer = NativeModules.LiveAudioVisualizer;

export function startAudioListening(): Promise<void> {
  return LiveAudioVisualizer.startAudioListening();
}

export function stopAudioListening(): Promise<void> {
  return LiveAudioVisualizer.stopAudioListening();
}

export function setSensitivity(sensitivity: number): Promise<void> {
  return LiveAudioVisualizer.setSensitivity(sensitivity);
}

export function addEventListener(callback: (data: any) => void): void {
  LiveAudioVisualizer.addListener('audioStarted', callback);
  LiveAudioVisualizer.addListener('VisualizationChanged', callback);
}
