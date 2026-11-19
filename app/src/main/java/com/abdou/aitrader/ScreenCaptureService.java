package com.abdou.aitrader;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.hardware.display.*;
import android.media.*;
import android.media.projection.*;
import android.os.*;
import java.io.*;

public class ScreenCaptureService extends Service {
    public static final String ACTION_DONE = "com.abdou.aitrader.CAPTURE_DONE";
    public static final String EXTRA_PATH = "image_path";
    private MediaProjection projection;
    private VirtualDisplay display;
    private ImageReader reader;
    private Handler handler;

    @Override public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel ch = new NotificationChannel("capture", "Chart capture", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(ch);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26
            ? new Notification.Builder(this, "capture") : new Notification.Builder(this);
        nb.setContentTitle("Abdou AI Trader").setContentText("جارٍ التقاط الشاشة بإذنك").setSmallIcon(android.R.drawable.ic_menu_camera).setOngoing(true);
        if (Build.VERSION.SDK_INT >= 29)
            startForeground(7, nb.build(), android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PROJECTION);
        else startForeground(7, nb.build());

        int code = intent.getIntExtra("result_code", Activity.RESULT_CANCELED);
        Intent data = (Intent) intent.getParcelableExtra("projection_data");
        if (code != Activity.RESULT_OK || data == null) { stopSelf(); return START_NOT_STICKY; }

        try {
            MediaProjectionManager mgr = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
            projection = mgr.getMediaProjection(code, data);
            projection.registerCallback(new MediaProjection.Callback() {
                @Override public void onStop() { cleanup(); stopSelf(); }
            }, handler);

            android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
            int width = dm.widthPixels, height = dm.heightPixels, density = dm.densityDpi;
            reader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
            reader.setOnImageAvailableListener(r -> {
                Image image = null;
                try {
                    image = r.acquireLatestImage();
                    if (image == null) return;
                    Image.Plane plane = image.getPlanes()[0];
                    int pixelStride = plane.getPixelStride();
                    int rowStride = plane.getRowStride();
                    int rowPadding = rowStride - pixelStride * width;
                    Bitmap padded = Bitmap.createBitmap(width + rowPadding / pixelStride, height, Bitmap.Config.ARGB_8888);
                    padded.copyPixelsFromBuffer(plane.getBuffer());
                    Bitmap bitmap = Bitmap.createBitmap(padded, 0, 0, width, height);
                    File file = new File(getCacheDir(), "latest_chart.jpg");
                    try (FileOutputStream out = new FileOutputStream(file)) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 88, out);
                    }
                    Intent done = new Intent(ACTION_DONE);
                    done.setPackage(getPackageName());
                    done.putExtra(EXTRA_PATH, file.getAbsolutePath());
                    sendBroadcast(done);
                    bitmap.recycle();
                    padded.recycle();
                    handler.postDelayed(() -> { cleanup(); stopSelf(); }, 300);
                } catch (Exception e) {
                    Intent done = new Intent(ACTION_DONE);
                    done.setPackage(getPackageName());
                    done.putExtra("error", e.getMessage());
                    sendBroadcast(done);
                    cleanup();
                    stopSelf();
                } finally {
                    if (image != null) image.close();
                }
            }, handler);

            display = projection.createVirtualDisplay("AbdouChartCapture", width, height, density,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR, reader.getSurface(), null, handler);
        } catch (Exception e) {
            Intent done = new Intent(ACTION_DONE);
            done.setPackage(getPackageName());
            done.putExtra("error", e.getMessage());
            sendBroadcast(done);
            cleanup();
            stopSelf();
        }
        return START_NOT_STICKY;
    }

    private void cleanup() {
        try { if (display != null) { display.release(); display = null; } } catch(Exception ignored) {}
        try { if (reader != null) { reader.close(); reader = null; } } catch(Exception ignored) {}
        try { if (projection != null) { projection.stop(); projection = null; } } catch(Exception ignored) {}
        stopForeground(STOP_FOREGROUND_REMOVE);
    }
    @Override public void onDestroy() { cleanup(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
}