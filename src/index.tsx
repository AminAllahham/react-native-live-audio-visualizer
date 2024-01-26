import { NativeModules, PermissionsAndroid, Platform } from 'react-native';

const LiveAudioVisualizer = NativeModules.LiveAudioVisualizer;

if (!LiveAudioVisualizer) {
  throw new Error('LiveAudioVisualizer native module is not available.');
}

export const LiveAudioVisualizerModal = LiveAudioVisualizer;
export function startAudioListening(): Promise<void> {
  return LiveAudioVisualizer.startAudioListening();
}

export function stopAudioListening(): Promise<void> {
  return LiveAudioVisualizer.stopAudioListening();
}

export function setSensitivity(sensitivity: number): Promise<void> {
  return LiveAudioVisualizer.setSensitivity(sensitivity);
}

export async function RequestAudioPermission(
  message: string,
  title: string,
  buttonPositive: string
) {
  if (Platform.OS === 'android') {
    try {
      const granted = await PermissionsAndroid.request(
        'android.permission.RECORD_AUDIO',
        {
          title,
          message,
          buttonPositive,
        }
      );
      if (granted === PermissionsAndroid.RESULTS.GRANTED) {
        console.log('Audio permission granted');
      } else {
        console.log('Audio permission denied');
      }
    } catch (err) {
      console.warn(err);
    }
  }
}
