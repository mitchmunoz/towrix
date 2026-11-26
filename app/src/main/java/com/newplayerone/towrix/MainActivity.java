package com.newplayerone.towrix;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.DisplayCutout;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.FrameLayout;
import android.window.OnBackInvokedDispatcher;

/**
 * TOWRIX — New Player One
 * Esta pantalla muestra el juego (assets/index.html) a pantalla completa.
 * No necesitas modificar este archivo.
 */
public class MainActivity extends Activity {

    private static final int REQ_FILE = 42;
    private WebView web;
    private Ads ads;
    /** Respuesta pendiente del selector de imágenes (bandera "Tu imagen"). */
    private ValueCallback<Uri[]> fileCallback;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        // Depuración remota (chrome://inspect) solo en builds de prueba, nunca en la versión publicada
        if ((getApplicationInfo().flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0) {
            WebView.setWebContentsDebuggingEnabled(true);
        }

        web = new WebView(this);
        web.setBackgroundColor(Color.BLACK);
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);          // el juego está hecho en JavaScript
        s.setDomStorageEnabled(true);          // guarda progreso, monedas y compras
        s.setMediaPlaybackRequiresUserGesture(false); // sonido desde la presentación
        s.setTextZoom(100);                    // ignora el tamaño de letra del sistema
        web.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // El juego nunca sale de sus propios archivos
                return !request.getUrl().toString().startsWith("file:///android_asset/");
            }
        });
        // Sin esto, el botón "Tu imagen" de las banderas no abre la galería dentro de la app
        web.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
                if (fileCallback != null) fileCallback.onReceiveValue(null);
                fileCallback = callback;
                try {
                    startActivityForResult(params.createIntent(), REQ_FILE);
                } catch (ActivityNotFoundException e) {
                    fileCallback = null;
                    return false;
                }
                return true;
            }
        });
        web.addJavascriptInterface(new Bridge(), "AndroidApp");
        ads = new Ads(this, web);
        web.addJavascriptInterface(ads, "AndroidAds");   // videos con recompensa (AdMob)

        root.addView(web, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT));
        setContentView(root);

        // Deja espacio para la cámara frontal (notch) del celular
        root.setOnApplyWindowInsetsListener((v, insets) -> {
            if (Build.VERSION.SDK_INT >= 28) {
                DisplayCutout c = insets.getDisplayCutout();
                if (c != null) {
                    v.setPadding(c.getSafeInsetLeft(), c.getSafeInsetTop(), c.getSafeInsetRight(), c.getSafeInsetBottom());
                } else {
                    v.setPadding(0, 0, 0, 0);
                }
            }
            return insets;
        });

        hideSystemBars();

        if (Build.VERSION.SDK_INT >= 33) {
            Api33.registerBack(this);
        }

        web.loadUrl("file:///android_asset/index.html");

        // Consentimiento (UMP) y anuncios: se piden en cada inicio, como exige Google
        ads.start();
    }

    /** Botón "atrás": el juego decide (pausa, volver al menú...). Si no lo usa, se cierra la app. */
    void handleBack() {
        web.evaluateJavascript("(window.onAndroidBack ? window.onAndroidBack() : false)", value -> {
            if (!"true".equals(value)) {
                finish();
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && Build.VERSION.SDK_INT < 33) {
            handleBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    /** Pantalla completa (oculta las barras del sistema). */
    @SuppressWarnings("deprecation")
    private void hideSystemBars() {
        if (Build.VERSION.SDK_INT >= 30) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController c = getWindow().getInsetsController();
            if (c != null) {
                c.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) hideSystemBars();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQ_FILE) {
            if (fileCallback != null) {
                fileCallback.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultCode, data));
                fileCallback = null;
            }
            return;
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Si sales de la app en plena partida, el juego se pone en pausa y el sonido se detiene
        web.evaluateJavascript("window.onAndroidPause && window.onAndroidPause()", null);
        web.onPause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        web.onResume();
        hideSystemBars();
        web.evaluateJavascript("window.onAndroidResume && window.onAndroidResume()", null);
    }

    @Override
    protected void onDestroy() {
        if (web != null) web.destroy();
        super.onDestroy();
    }

    /** Funciones que el juego puede llamar desde JavaScript. */
    class Bridge {
        @JavascriptInterface
        public void exit() {
            runOnUiThread(MainActivity.this::finish);
        }
    }

    /** Botón atrás para Android 13 o superior. */
    static class Api33 {
        static void registerBack(MainActivity a) {
            a.getOnBackInvokedDispatcher().registerOnBackInvokedCallback(
                    OnBackInvokedDispatcher.PRIORITY_DEFAULT, a::handleBack);
        }
    }
}
