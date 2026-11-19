package com.abdou.aitrader;

import android.app.*;
import android.content.*;
import android.graphics.*;
import android.media.projection.MediaProjectionManager;
import android.os.*;
import android.speech.RecognizerIntent;
import android.text.InputType;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
 private static final int PICK=41,CAPTURE=42,VOICE=43;
 private final ExecutorService worker=Executors.newSingleThreadExecutor();
 private final ArrayList<JSONObject> chat=new ArrayList<>();
 private SharedPreferences prefs; private LinearLayout messages; private ScrollView scroll;
 private EditText input; private TextView attachment; private Bitmap image; private String apiKey="";
 private Button sendButton; private BroadcastReceiver receiver; private boolean busy=false;
 private final int bg=0xff101820,panel=0xff202b36,fg=0xfff3f5f7,muted=0xffaab7c4,accent=0xff65cbbb;
 private int dp(int n){return (int)(n*getResources().getDisplayMetrics().density+.5f);}
 private TextView text(String s,int size,int color){TextView t=new TextView(this);t.setText(s);t.setTextSize(size);t.setTextColor(color);return t;}
 private android.graphics.drawable.GradientDrawable shape(int color,int rad){android.graphics.drawable.GradientDrawable d=new android.graphics.drawable.GradientDrawable();d.setColor(color);d.setCornerRadius(dp(rad));return d;}
 private Button btn(String s){Button b=new Button(this);b.setText(s);b.setTextSize(12);b.setAllCaps(false);b.setMinWidth(0);b.setMinimumWidth(0);b.setPadding(dp(7),0,dp(7),0);return b;}
 @Override public void onCreate(Bundle b){super.onCreate(b);prefs=getSharedPreferences("abdou",0);apiKey=prefs.getString("key","");load();ui();drawChat();
  receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){String e=i.getStringExtra("error"),p=i.getStringExtra(ScreenCaptureService.EXTRA_PATH);if(e!=null)toast("فشل الالتقاط: "+e);else if(p!=null){image=BitmapFactory.decodeFile(p);updateAttach();}}};
  IntentFilter f=new IntentFilter(ScreenCaptureService.ACTION_DONE);if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,f);
 }
 private void ui(){
  LinearLayout root=new LinearLayout(this);root.setOrientation(1);root.setBackgroundColor(bg);
  LinearLayout bar=new LinearLayout(this);bar.setGravity(Gravity.CENTER_VERTICAL);bar.setPadding(dp(10),dp(6),dp(10),dp(6));bar.setBackgroundColor(panel);
  Button menu=btn("☰");bar.addView(menu);menu.setOnClickListener(v->historyDialog());
  TextView title=text("Abdou AI Trader",19,fg);title.setTypeface(null,1);title.setGravity(Gravity.CENTER);bar.addView(title,new LinearLayout.LayoutParams(0,dp(48),1));
  Button fresh=btn("＋");bar.addView(fresh);fresh.setOnClickListener(v->newChat());
  Button settings=btn("⚙");bar.addView(settings);settings.setOnClickListener(v->settings());
  root.addView(bar);
  scroll=new ScrollView(this);scroll.setFillViewport(true);messages=new LinearLayout(this);messages.setOrientation(1);messages.setPadding(dp(12),dp(10),dp(12),dp(12));scroll.addView(messages);root.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
  LinearLayout tools=new LinearLayout(this);tools.setGravity(Gravity.CENTER_VERTICAL);tools.setPadding(dp(9),dp(3),dp(9),dp(3));
  attachment=text("جاهز للمحادثة",12,accent);tools.addView(attachment,new LinearLayout.LayoutParams(0,dp(30),1));
  Button file=btn("＋ إرفاق");tools.addView(file);file.setOnClickListener(v->chooseImage());
  Button snap=btn("لقطة");tools.addView(snap);snap.setOnClickListener(v->capture());
  root.addView(tools);
  LinearLayout composer=new LinearLayout(this);composer.setGravity(Gravity.BOTTOM|Gravity.CENTER_VERTICAL);composer.setPadding(dp(9),dp(5),dp(9),dp(10));
  input=new EditText(this);input.setTextColor(fg);input.setHintTextColor(muted);input.setHint("اسأل Abdou AI أي شيء...");input.setTextSize(15);input.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);input.setMinLines(1);input.setMaxLines(5);input.setPadding(dp(14),dp(10),dp(14),dp(10));input.setBackground(shape(panel,24));composer.addView(input,new LinearLayout.LayoutParams(0,dp(52),1));
  Button mic=btn("🎙");composer.addView(mic);mic.setOnClickListener(v->voice());
  sendButton=btn("➤");sendButton.setTextColor(0xff06201b);sendButton.setBackground(shape(accent,30));LinearLayout.LayoutParams sp=new LinearLayout.LayoutParams(dp(48),dp(48));sp.leftMargin=dp(5);composer.addView(sendButton,sp);sendButton.setOnClickListener(v->send());
  root.addView(composer);setContentView(root);
 }
 private void drawChat(){messages.removeAllViews();if(chat.isEmpty()){bubble("مرحباً! أنا Abdou AI Trader. اسألني في أي موضوع أو أرسل صورة لتحليلها.",false);return;}for(JSONObject m:chat)bubble(m.optString("text"),"user".equals(m.optString("role")));}
 private void bubble(String s,boolean user){LinearLayout row=new LinearLayout(this);row.setGravity(user?Gravity.RIGHT:Gravity.LEFT);row.setPadding(0,dp(4),0,dp(4));TextView t=text(s,15,fg);t.setPadding(dp(14),dp(11),dp(14),dp(11));t.setBackground(shape(user?0xff245a55:panel,18));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels*.83),-2);row.addView(t,p);messages.addView(row);scroll.post(()->scroll.fullScroll(View.FOCUS_DOWN));}
 private void load(){try{JSONArray a=new JSONArray(prefs.getString("messages","[]"));for(int i=0;i<a.length();i++)chat.add(a.getJSONObject(i));}catch(Exception e){chat.clear();}}
 private void persist(){JSONArray a=new JSONArray();for(JSONObject o:chat)a.put(o);prefs.edit().putString("messages",a.toString()).apply();}
 private void add(String role,String s){try{chat.add(new JSONObject().put("role",role).put("text",s));persist();}catch(Exception ignored){}}
 private void toast(String s){Toast.makeText(this,s,Toast.LENGTH_LONG).show();}
 private void updateAttach(){attachment.setText(image==null?"جاهز للمحادثة":"✓ صورة مرفقة");}
 private void chooseImage(){Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("image/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,PICK);}
 private void capture(){MediaProjectionManager m=(MediaProjectionManager)getSystemService(MEDIA_PROJECTION_SERVICE);startActivityForResult(m.createScreenCaptureIntent(),CAPTURE);}
 private void voice(){try{Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);i.putExtra(RecognizerIntent.EXTRA_LANGUAGE,"ar");startActivityForResult(i,VOICE);}catch(Exception e){toast("الإملاء الصوتي غير متاح على هذا الجهاز.");}}
 @Override protected void onActivityResult(int r,int c,Intent d){super.onActivityResult(r,c,d);if(r==PICK&&c==RESULT_OK&&d!=null){try(InputStream in=getContentResolver().openInputStream(d.getData())){image=BitmapFactory.decodeStream(in);updateAttach();}catch(Exception e){toast("تعذر فتح الصورة.");}}
  if(r==VOICE&&c==RESULT_OK&&d!=null){ArrayList<String>a=d.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(a!=null&&!a.isEmpty())input.setText(a.get(0));}
  if(r==CAPTURE&&c==RESULT_OK&&d!=null){Intent s=new Intent(this,ScreenCaptureService.class);s.putExtra("result_code",c);s.putExtra("projection_data",d);if(Build.VERSION.SDK_INT>=26)startForegroundService(s);else startService(s);toast("انتقل إلى الشارت، سيتم التقاطه بعد 5 ثوانٍ.");}
 }
 private void settings(){EditText k=new EditText(this);k.setSingleLine(true);k.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_PASSWORD);k.setHint("مفتاح OpenAI API");k.setText(apiKey);new AlertDialog.Builder(this).setTitle("إعدادات Abdou AI").setMessage("أدخل مفتاح API هنا فقط. لا تشاركه. الاستخدام يحتاج رصيد API منفصل.").setView(k).setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{apiKey=k.getText().toString().trim();prefs.edit().putString("key",apiKey).apply();toast("تم حفظ الإعداد.");}).show();}
 private void historyDialog(){if(chat.isEmpty()){toast("لا توجد محادثات محفوظة بعد.");return;}new AlertDialog.Builder(this).setTitle("سجل المحادثات").setItems(new String[]{"مسح سجل المحادثة"},(d,n)->new AlertDialog.Builder(this).setMessage("حذف سجل هذه المحادثة نهائياً؟").setNegativeButton("إلغاء",null).setPositiveButton("حذف",(x,y)->{chat.clear();persist();drawChat();}).show()).setNegativeButton("إغلاق",null).show();}
 private void newChat(){if(chat.isEmpty())return;new AlertDialog.Builder(this).setTitle("محادثة جديدة").setMessage("بدء محادثة جديدة؟").setNegativeButton("إلغاء",null).setPositiveButton("بدء",(d,w)->{chat.clear();persist();drawChat();}).show();}
 private void send(){if(busy)return;String q=input.getText().toString().trim();if(q.isEmpty()&&image==null)return;if(apiKey.isEmpty()){settings();toast("أضف مفتاح API أولاً.");return;}if(q.isEmpty())q="حلل الصورة المرفقة بالتفصيل.";final String prompt=q;final Bitmap img=image;add("user",prompt+(img!=null?" [صورة مرفقة]":""));bubble(prompt+(img!=null?"\n📎 صورة مرفقة":""),true);input.setText("");image=null;updateAttach();busy=true;sendButton.setEnabled(false);bubble("يفكر Abdou AI...",false);
  worker.execute(()->{String answer;try{answer=requestAI(prompt,img);}catch(Exception e){answer="تعذر إكمال الطلب: "+e.getMessage();}final String out=answer;runOnUiThread(()->{if(messages.getChildCount()>0)messages.removeViewAt(messages.getChildCount()-1);add("assistant",out);bubble(out,false);busy=false;sendButton.setEnabled(true);});});
 }
 private String requestAI(String q,Bitmap img)throws Exception{
  JSONArray arr=new JSONArray();int from=Math.max(0,chat.size()-16);
  for(int i=from;i<chat.size();i++){JSONObject h=chat.get(i);if(i==chat.size()-1&&"user".equals(h.optString("role"))&&img!=null){ByteArrayOutputStream b=new ByteArrayOutputStream();img.compress(Bitmap.CompressFormat.JPEG,78,b);String enc=android.util.Base64.encodeToString(b.toByteArray(),android.util.Base64.NO_WRAP);JSONArray parts=new JSONArray().put(new JSONObject().put("type","input_text").put("text",q)).put(new JSONObject().put("type","input_image").put("image_url","data:image/jpeg;base64,"+enc));arr.put(new JSONObject().put("role","user").put("content",parts));}else arr.put(new JSONObject().put("role",h.optString("role")).put("content",h.optString("text")));}
  JSONObject body=new JSONObject();body.put("model","gpt-4.1-mini");body.put("instructions","أنت Abdou AI Trader، مساعد عربي قوي وسريع ومتعاون. أجب عن الأسئلة العامة بدقة ووضوح. حلل صور الشارت وفق الظاهر فقط، واذكر CALL أو PUT أو WAIT والأسباب والمخاطر، ولا تضمن الربح ولا تنفذ الصفقات. إذا لم تتوفر معلومات كافية فصرح بذلك.");body.put("input",arr);body.put("max_output_tokens",1000);
  HttpURLConnection c=(HttpURLConnection)new URL("https://api.openai.com/v1/responses").openConnection();c.setRequestMethod("POST");c.setConnectTimeout(12000);c.setReadTimeout(45000);c.setDoOutput(true);c.setRequestProperty("Authorization","Bearer "+apiKey);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes("UTF-8"));}
  int code=c.getResponseCode();InputStream stream=code>=200&&code<300?c.getInputStream():c.getErrorStream();ByteArrayOutputStream b=new ByteArrayOutputStream();byte[] buf=new byte[8192];int n;while((n=stream.read(buf))!=-1)b.write(buf,0,n);String raw=b.toString("UTF-8");c.disconnect();if(code<200||code>=300)throw new Exception("HTTP "+code+": "+raw);
  JSONArray out=new JSONObject(raw).optJSONArray("output");if(out!=null)for(int i=0;i<out.length();i++){JSONObject item=out.optJSONObject(i);JSONArray parts=item==null?null:item.optJSONArray("content");if(parts!=null)for(int j=0;j<parts.length();j++){JSONObject p=parts.optJSONObject(j);if(p!=null&&"output_text".equals(p.optString("type")))return p.optString("text");}}
  return "لم يصل رد نصي من النموذج.";
 }
 @Override protected void onDestroy(){if(receiver!=null)try{unregisterReceiver(receiver);}catch(Exception ignored){}worker.shutdownNow();super.onDestroy();}
}