package com.example.homker.audioplayer;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.JsResult;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;

import java.io.IOException;

public class MainActivity extends AppCompatActivity {

    public WebView webView;
    public static final String ACTION_PLAY = "com.example.action.PLAY";
    public Intent intent;
    public String audio_url = "http://wx.ecjtu.net/wmkj/mp3/audio/20141206/1417879706.mp3";
    Context context;
    Service audioService;
    ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            audioService = ((AudioPlayerService.audioBinder)service).getService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        intent = new Intent(this, AudioPlayerService.class);
        intent.setPackage(this.getPackageName());
        intent.setAction(ACTION_PLAY);
        //bindService(intent,conn, Context.BIND_AUTO_CREATE );

        webView = (WebView) findViewById(R.id.webview);
        WebSettings webSettings = webView.getSettings();
        webSettings.setAppCacheEnabled(true);
        webSettings.setJavaScriptEnabled(true);
        webView.loadUrl("file:///android_asset/audiohtml/index.html");
        webView.addJavascriptInterface(new RxJavaScriptInterface(), "rx");
        webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onJsAlert(WebView view, String url, String message, JsResult result) {
                result.confirm();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        unbindService(conn);
        super.onDestroy();
    }

    final class RxJavaScriptInterface {

        public RxJavaScriptInterface() {

        }

        @JavascriptInterface
        public void play() {
            context.startService(intent);
            Log.i("webview", "play");
        }

        @JavascriptInterface
        public void pause() {
            Log.i("webview", "pause");
        }

        @JavascriptInterface
        public int progress() {
            Log.i("webview", "process");
            webView.loadUrl("javascript::progress()");
            return 0;
        }

        @JavascriptInterface
        public void stop() {
            Log.i("webview", "stop");
        }
    }

    public static class AudioPlayerService extends Service {

        public static final String ACTION_PLAY = "com.example.action.PLAY";
        MediaPlayer mediaPlayer = null;

        @Override
        public void onCreate() {
            super.onCreate();
            Log.i("webview","service start");
        }

        @Override
        public int onStartCommand(Intent intent, int flags, int startId) {
            final WifiManager.WifiLock wifiLock = ((WifiManager) getSystemService(getApplicationContext().WIFI_SERVICE)).createWifiLock(WifiManager.WIFI_MODE_FULL, "audioPlay");
            wifiLock.acquire();
            //通知开始转菊花
            Log.i("webview",intent.getAction());
            if (intent.getAction().equals(ACTION_PLAY)) {
                String url = "http://wx.ecjtu.net/wmkj/mp3/audio/20141206/1417879706.mp3";
                //String url = intent.getStringExtra("url");
                mediaPlayer = new MediaPlayer();
                mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
                try {
                    mediaPlayer.setDataSource(url);
                    mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                        @Override
                        public void onPrepared(MediaPlayer mp) {
                            //停止转菊花,开始跑进度条
                            Log.i("webview","audio start");
                            mediaPlayer.start();
                        }
                    });
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            //释放内存和WiFi锁
                            wifiLock.release();
                            mediaPlayer.release();
                        }
                    });
                    mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
                        @Override
                        public void onBufferingUpdate(MediaPlayer mp, int percent) {
                            //进度条
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            return super.onStartCommand(intent, flags, startId);
        }

        @Nullable
        @Override
        public IBinder onBind(Intent intent) {
            return new audioBinder();
        }

        public class audioBinder extends Binder {
            public AudioPlayerService getService() {
                return AudioPlayerService.this;
            }
        }
    }


}