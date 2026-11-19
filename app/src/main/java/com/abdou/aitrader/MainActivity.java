package com.abdou.aitrader;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    private static final int PICK_IMAGE = 41;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private EditText apiKey;
    private TextView result;
    private Bitmap selectedChart;
    private Button analyzeButton;

    private int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density + 0.5f); }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        ScrollView scroll = new ScrollView(this);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20),dp(28),dp(20),dp(24));
        root.setBackgroundColor(0xff0c121e);
        scroll.addView(root);

        TextView title = label("ABDOU AI TRADER", 26, 0xffffffff);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        TextView subtitle = label("محلل الشارت بالذكاء الاصطناعي", 17, 0xff8ed8e8);
        subtitle.setGravity(Gravity.CENTER);
        root.addView(subtitle);

        TextView info = label("\n1) اختر صورة واضحة لشارت Quotex.\n2) أدخل مفتاح OpenAI API الخاص بك.\n3) اضغط تحليل للحصول على CALL / PUT / WAIT مع الأسباب.", 15, 0xffe1e8f0);
        root.addView(info);

        apiKey = new EditText(this);
        apiKey.setSingleLine(true);
        apiKey.setHint("OpenAI API key (لا تشاركه مع أحد)");
        apiKey.setHintTextColor(0xff9aa8ba);
        apiKey.setTextColor(0xffffffff);
        apiKey.setInputType(129);
        apiKey.setPadding(dp(12),dp(10),dp(12),dp(10));
        root.addView(apiKey, new LinearLayout.LayoutParams(-1,dp(54)));

        Button choose = new Button(this);
        choose.setText("اختيار صورة الشارت");
        root.addView(choose);
        choose.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.setType("image/*");
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(i, PICK_IMAGE);
        });

        analyzeButton = new Button(this);
        analyzeButton.setText("حلّل الشارت بالذكاء الاصطناعي");
        root.addView(analyzeButton);
        analyzeButton.setOnClickListener(v -> analyze());

        result = label("\nبانتظار صورة الشارت...", 16, 0xffffffff);
        result.setPadding(0,dp(18),0,dp(18));
        root.addView(result);

        TextView warning = label("تنبيه: التحليل احتمالي وليس توصية مالية أو ضمان ربح. لا ينفّذ التطبيق الصفقات. نفّذ يدويًا وبعد التحقق، وابدأ بالحساب التجريبي. مفتاح API يحتاج رصيدًا/فوترة لدى مزوّد الخدمة ولا يشمله اشتراك ChatGPT.", 13, 0xffffc477);
        root.addView(warning);
        setContentView(scroll);
    }

    private TextView label(String text, int size, int color) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color);
        return t;
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode,resultCode,data);
        if (requestCode == PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) {}
            try (InputStream in = getContentResolver().openInputStream(uri)) {
                selectedChart = BitmapFactory.decodeStream(in);
                result.setText("تم اختيار الصورة. اضغط تحليل.");
            } catch (Exception e) {
                result.setText("تعذر فتح الصورة: " + e.getMessage());
            }
        }
    }

    private void analyze() {
        String key = apiKey.getText().toString().trim();
        if (key.isEmpty()) { result.setText("أدخل مفتاح OpenAI API أولاً."); return; }
        if (selectedChart == null) { result.setText("اختر صورة الشارت أولاً."); return; }
        analyzeButton.setEnabled(false);
        result.setText("جارٍ إرسال الصورة وتحليلها...");
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
        image.put("type", "input_image");
        image.put("image_url", "data:image/jpeg;base64," + base64);

        JSONObject prompt = new JSONObject();
        prompt.put("type", "input_text");
        prompt.put("text", "حلل صورة الشارت فقط. أجب بالعربية وبشكل محافظ. حدد CALL أو PUT أو WAIT، الاتجاه، مناطق الدعم والمقاومة الظاهرة، الزخم، وهل الصورة غير واضحة. لا تخترع أسعاراً أو مؤشرات غير ظاهرة. لا تدّع ضمان النجاح. اذكر أن مدة 1-2 دقيقة عالية المخاطر. إذا لا توجد فرصة واضحة فالإجابة WAIT. أعط أسباباً مختصرة ومستوى ثقة وصفي منخفض/متوسط/مرتفع، وليس ضماناً.");

        JSONArray content = new JSONArray().put(prompt).put(image);
        JSONObject message = new JSONObject();
        message.put("role","user"); message.put("content",content);
        JSONObject body = new JSONObject();
        body.put("model","gpt-4.1-mini");
        body.put("input",new JSONArray().put(message));
        body.put("max_output_tokens",500);

        HttpURLConnection c = (HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000); c.setReadTimeout(60000);
        c.setDoOutput(true);
        c.setRequestProperty("Authorization","Bearer " + key);
        c.setRequestProperty("Content-Type","application/json");
        try (OutputStream out = c.getOutputStream()) { out.write(body.toString().getBytes("UTF-8")); }
        int code = c.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream();
        ByteArrayOutputStream response = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192]; int n;
        while ((n=stream.read(buffer))!=-1) response.write(buffer,0,n);
        String raw = response.toString("UTF-8");
        c.disconnect();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code + ": " + raw);
        JSONObject json = new JSONObject(raw);
        JSONArray output = json.optJSONArray("output");
        if (output != null) {
            for (int i=0;i<output.length();i++) {
                JSONObject item = output.optJSONObject(i);
                JSONArray parts = item == null ? null : item.optJSONArray("content");
                if (parts != null) for (int j=0;j<parts.length();j++) {
                    JSONObject part = parts.optJSONObject(j);
                    if (part != null && "output_text".equals(part.optString("type")))
                        return part.optString("text","لم يصل نص التحليل.");
                }
            }
        }
        return "وصل رد غير متوقع من خدمة التحليل:\n" + raw;
    }

    @Override protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }
}
