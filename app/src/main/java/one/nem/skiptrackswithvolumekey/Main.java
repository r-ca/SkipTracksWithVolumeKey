package one.nem.skiptrackswithvolumekey;

import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemClock;
import android.view.KeyEvent;
import android.view.ViewConfiguration;

import androidx.annotation.RequiresApi;

import java.lang.reflect.Method;
import java.security.Key;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import de.robv.android.xposed.XposedBridge;

@RequiresApi(20)
public class Main implements IXposedHookLoadPackage {
    private static boolean isLongPressing = false;
    private static AudioManager audioManager;
    // For debug
    final private static boolean isDebugMode = true;
    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {

        // 1
        Class<?> PhoneWindowManagerClass = null;
        try {
            PhoneWindowManagerClass = XposedHelpers.findClass("com.android.server.policy.PhoneWindowManager", loadPackageParam.classLoader);
        }
        catch (Throwable t) {
            XposedBridge.log("WARN  : " + t);
            return;
        }

//        if (PhoneWindowManagerClass == null) {
//            XposedBridge.log("WARN  : PhoneWindowManagerClass is null");
//        }
        XposedHelpers.findAndHookMethod(PhoneWindowManagerClass, "interceptKeyBeforeQueueing", KeyEvent.class, int.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                KeyEvent event = (KeyEvent) param.args[0];
                int keyCode = event.getKeyCode();

                // ボリュームキーじゃないなら元のメソッドに処理を戻す?
                if (KeyEvent.KEYCODE_VOLUME_UP != keyCode && KeyEvent.KEYCODE_VOLUME_DOWN != keyCode) {
                    //TODO
                    return;
                }

                Context context = (Context) XposedHelpers.getObjectField(param.thisObject, "mContext");

                // 画面が点灯していない場合は処理に干渉しない
                // PowerManagerを取得
                PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager.isInteractive()) {
                    //TODO
                    if (isDebugMode) XposedBridge.log("DEBUG: Device is Interactive.");
                    return;
                }

                // AudioManagerを取得
                audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

                // アクティブなミュージックセッションがない場合は処理に干渉しない
                if (!audioManager.isMusicActive()) {
                    //TODO
                    if (isDebugMode) XposedBridge.log("DEBUG: No active music sessions.");
                    return;
                }

                Handler mHandler = (Handler) XposedHelpers.getObjectField(param.thisObject, "mHandler");

                if (isDebugMode) XposedBridge.log("KeyEvent: " + event);
                if (isDebugMode) XposedBridge.log("KeyCode: " + keyCode);

                if (event.getAction() == KeyEvent.ACTION_DOWN) {
                    if (!isLongPressing) {
                        mHandler.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                if (!isLongPressing) {
                                    isLongPressing = true;
                                    if (isDebugMode) XposedBridge.log("DEBUG: LogPoint1");
                                    handleLongPress(event);
                                }
                            }
                        }, ViewConfiguration.getLongPressTimeout());
                    }
                } else if (event.getAction() == KeyEvent.ACTION_UP) {
                    isLongPressing = false;
                    adjustVolume(event);
                    mHandler.removeCallbacksAndMessages(null);
                    param.setResult(0);
                }
                param.setResult(0);
            }
        });
    }

    private void adjustVolume(KeyEvent event) {
        // 単押しだった場合にボリュームを変えるやつ
        audioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC,
                event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP ?
                        AudioManager.ADJUST_RAISE : AudioManager.ADJUST_LOWER, 0);
    }
    private void handleLongPress(KeyEvent event) {
        if (isDebugMode) XposedBridge.log("DEBUG: LongPress Detected!");
        Intent keyIntent = null;

        try { //Get KeyIntent
            keyIntent = new Intent(Intent.ACTION_MEDIA_BUTTON, null);
        }
        catch (Throwable t) {
            XposedBridge.log("ERROR: " + t);
        }

        if (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP) { //TODO: 効率悪すぎるのでなんとかする
            KeyEvent keyEventNext_Dn = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            if (isDebugMode) XposedBridge.log("DEBUG: " + event);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEventNext_Dn);
            audioManager.dispatchMediaKeyEvent(keyEventNext_Dn);

            KeyEvent keyEventNext_Up = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_NEXT, 0);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEventNext_Up);
            audioManager.dispatchMediaKeyEvent(keyEventNext_Up);
        }
        else {
            if (isDebugMode) XposedBridge.log("DEBUG: " + event);
            KeyEvent keyEventPrev_Dn = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEventPrev_Dn);
            audioManager.dispatchMediaKeyEvent(keyEventPrev_Dn);

            KeyEvent keyEventPrev_Up = new KeyEvent(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), KeyEvent.ACTION_UP, KeyEvent.KEYCODE_MEDIA_PREVIOUS, 0);
            keyIntent.putExtra(Intent.EXTRA_KEY_EVENT, keyEventPrev_Up);
            audioManager.dispatchMediaKeyEvent(keyEventPrev_Up);

        }
    }
}
