package com.abdou.aitrader;

import android.app.Activity;
import android.content.*;
import android.media.projection.MediaProjectionManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.*;
import android.view.Gravity;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 41, CAPTURE_SCREEN = 42;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText apiKey;
    private TextView result;
    private Bitmap selectedChart;
    private Button analyzeButton;
    private BroadcastReceiver captureReceiver;

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(28),dp(20),dp(24));
        root.setBackgroundColor(0xff0c121e);

        TextView title = label("ABDOU AI TRADER", 26, 0xffffffff);
        title.setGravity(Gravity.CENTER); root.addView(title);
        TextView subtitle = label("محلل الشارت بالذكاء الاصطناعي", 17, 0xff8ed8e8);
        subtitle.setGravity(Gravity.CENTER); root.addView(subtitle);

        root.addView(label("\nالتقاط الشارت الظاهر أو اختيار صورة محفوظة، ثم تحليلها.", 15, 0xffe1e8f0));

        Button capture = new Button(this);
        capture.setText("التقاط الشاشة الآن");
        root.addView(capture);
        capture.setOnClickListener(v -> requestScreenCapture());

        Button choose = new Button(this);
        choose.setText("اختيار صورة من الهاتف");
        root.addView(choose);
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*"); i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_IMAGE);
        });

        apiKey = new EditText(this);
        apiKey.setSingleLine(true); apiKey.setHint("مفتاح OpenAI API");
        apiKey.setHintTextColor(0xff9aa8ba); apiKey.setTextColor(0xffffffff);
        apiKey.setInputType(129); apiKey.setPadding(dp(12),dp(10),dp(12),dp(10));
        root.addView(apiKey, new LinearLayout.LayoutParams(-1,dp(54)));

        analyzeButton = new Button(this);
        analyzeButton.setText("حلّل الشارت بالذكاء الاصطناعي");
        root.addView(analyzeButton);
        analyzeButton.setOnClickListener(v -> analyze());

        result = label("\nبانتظار التقاط الشارت...", 16, 0xffffffff);
        result.setPadding(0,dp(18),0,dp(18)); root.addView(result);

        TextView warning = label("التحليل احتمالي وليس ضمان ربح. لا ينفّذ التطبيق الصفقات. تداول يدوي فقط، وجرّب الحساب التجريبي. مفتاح API يتطلب فوترة لدى OpenAI ولا يشمله اشتراك ChatGPT. أدخله داخل التطبيق فقط ولا ترسله لأحد.", 13, 0xffffc477);
        root.addView(warning);

        ScrollView scroll = new ScrollView(this); scroll.addView(root); setContentView(scroll);
        captureReceiver = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent intent) {
                String error = intent.getStringExtra("error");
                String path = intent.getStringExtra(ScreenCaptureService.EXTRA_PATH);
                if (error != null) result.setText("تعذر التقاط الشاشة: " + error);
                else if (path != null) {
                    selectedChart = BitmapFactory.decodeFile(path);
                    result.setText(selectedChart == null ? "تعذر قراءة الصورة الملتقطة." : "تم التقاط الشاشة. انتقل إلى الشارت أولاً قبل الضغط على التقاط.");
                }
            }
        };
        IntentFilter filter = new IntentFilter(ScreenCaptureService.ACTION_DONE);
        if (Build.VERSION.SDK_INT >= 33) registerReceiver(captureReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        else registerReceiver(captureReceiver, filter);
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this); t.setText(text); t.setTextSize(size); t.setTextColor(color); return t;
    }

    private void requestScreenCapture() {
        MediaProjectionManager manager = (MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);
        startActivityForResult(manager.createScreenCaptureIntent(), CAPTURE_SCREEN);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == CAPTURE_SCREEN) {
            if (resultCode == RESULT_OK && data != null) {
                Intent service = new Intent(this, ScreenCaptureService.class);
                service.putExtra("result_code", resultCode);
                service.putExtra("projection_data", data);
                if (Build.VERSION.SDK_INT >= 26) startForegroundService(service); else startService(service);
                result.setText("تم منح إذن التقاط الشاشة. انتظر لحظة...");
            } else result.setText("ألغيت صلاحية التقاط الشاشة.");
        }
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                selectedChart = BitmapFactory.decodeStream(in);
                result.setText(selectedChart == null ? "تعذر فتح الصورة." : "تم اختيار الصورة. اضغط تحليل.");
            } catch (Exception e) { result.setText("تعذر فتح الصورة: " + e.getMessage()); }
        }
    }

    private void analyze() {
        String key = apiKey.getText().toString().trim();
        if (key.isEmpty()) { result.setText("أدخل مفتاح OpenAI API أولاً."); return; }
        if (selectedChart == null) { result.setText("التقط الشاشة أو اختر صورة الشارت أولاً."); return; }
        analyzeButton.setEnabled(false); result.setText("جارٍ تحليل الصورة...");
        executor.execute(() -> {
            String answer;
            try { answer = requestVision(key, selectedChart); }
            catch (Exception e) { answer = "فشل التحليل: " + e.getMessage(); }
            final String output = answer;
            runOnUiThread(() -> { result.setText(output); analyzeButton.setEnabled(true); });
        });
    }

    private String requestVision(String key, Bitmap bitmap) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 82, bytes);
        String base64 = android.util.Base64.encodeToString(bytes.toByteArray(), android.util.Base64.NO_WRAP);
        JSONObject image = new JSONObject();
        image.put("type", "input_image"); image.put("image_url", "data:image/jpeg;base64," + base64);
        JSONObject prompt = new JSONObject();
        prompt.put("type", "input_text");
        prompt.put("text", "أنت محلل رسوم بيانية حذر. حلل الصورة المرفقة فقط وبالعربية. أعطِ أولاً القرار CALL أو PUT أو WAIT. ثم الاتجاه، البنية والشموع والمؤشرات الظاهرة فقط، مستويات الدعم والمقاومة المرئية، الأسباب، المخاطر، والثقة الوصفية. لا تخترع بيانات أو أسعاراً. إذا لم تكن الصورة واضحة أو لا توجد فرصة واضحة فاختر WAIT. تداول 1-2 دقيقة شديد المخاطر ولا يوجد ضمان ربح. لا تقدم توصية مؤكدة.");
        JSONObject message = new JSONObject();
        message.put("role","user"); message.put("content",new JSONArray().put(prompt).put(image));
        JSONObject body = new JSONObject();
        body.put("model","gpt-4.1-mini"); body.put("input",new JSONArray().put(message)); body.put("max_output_tokens",600);
        HttpURLConnection c = (HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();
        c.setRequestMethod("POST"); c.setConnectTimeout(20000); c.setReadTimeout(60000); c.setDoOutput(true);
        c.setRequestProperty("Authorization","Bearer " + key); c.setRequestProperty("Content-Type","application/json");
        try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes("UTF-8")); }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n=stream.read(buffer))!=-1) response.write(buffer,0,n);
        String raw = response.toString("UTF-8"); c.disconnect();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + raw);
        JSONObject json = new JSONObject(raw); JSONArray output = json.optJSONArray("output");
        if (output != null) for (int i=0;i<output.length();i++) {
            JSONObject item=output.optJSONObject(i); JSONArray parts=item==null?null:item.optJSONArray("content");
            if (parts != null) for (int j=0;j<parts.length();j++) {
                JSONObject part=parts.optJSONObject(j);
                if (part!=null && "output_text".equals(part.optString("type"))) return part.optString("text","لا يوجد نص.");
            }
        }
        return "لم أتمكن من قراءة نتيجة التحليل: " + raw;
    }

    @Override protected void onDestroy() {
        if (captureReceiver != null) try { unregisterReceiver(captureReceiver); } catch(Exception ignored) {}
        executor.shutdownNow(); super.onDestroy();
    }
}