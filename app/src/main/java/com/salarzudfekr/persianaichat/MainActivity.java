package com.salarzudfekr.persianaichat;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    static final String VERSION = "1.1.0";
    static final String RELEASES_URL = "https://github.com/salarzudfekr/persian-ai-chat/releases/latest";
    static final int BLUE = Color.rgb(37, 99, 235);
    static final int GREEN = Color.rgb(22, 163, 74);
    static final int BG = Color.rgb(245, 247, 251);
    static final int CARD = Color.WHITE;
    static final int TEXT = Color.rgb(17, 24, 39);
    static final int MUTED = Color.rgb(107, 114, 128);

    LinearLayout messages;
    ScrollView scroll;
    EditText input;
    Button send;
    TextView subtitle;
    SharedPreferences prefs;
    final ExecutorService pool = Executors.newSingleThreadExecutor();
    final Handler ui = new Handler(Looper.getMainLooper());

    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("settings", MODE_PRIVATE);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(buildScreen());
        addBot("سلام! من آماده‌ام. از دکمه تنظیمات، API Key و Base URL و Model را وارد کن.");
    }

    View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(14), dp(12), dp(14), dp(8));
        header.setBackgroundColor(CARD);

        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setGravity(Gravity.RIGHT);
        TextView title = label("چت فارسی AI", 21, TEXT, true);
        subtitle = label("نسخه " + VERSION + " • OpenAI-compatible", 12, MUTED, false);
        titles.addView(title); titles.addView(subtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        Button update = smallButton("آپدیت");
        update.setOnClickListener(v -> openUpdates());
        Button settings = smallButton("تنظیمات");
        settings.setOnClickListener(v -> showSettings());
        header.addView(update); header.addView(settings);
        root.addView(header);

        scroll = new ScrollView(this);
        messages = new LinearLayout(this);
        messages.setOrientation(LinearLayout.VERTICAL);
        messages.setPadding(dp(12), dp(14), dp(12), dp(14));
        scroll.addView(messages);
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        LinearLayout composer = new LinearLayout(this);
        composer.setOrientation(LinearLayout.HORIZONTAL);
        composer.setGravity(Gravity.BOTTOM);
        composer.setPadding(dp(12), dp(8), dp(12), dp(12));
        composer.setBackgroundColor(CARD);
        input = new EditText(this);
        input.setHint("پیام بنویس...");
        input.setMinLines(1);
        input.setMaxLines(4);
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setGravity(Gravity.RIGHT | Gravity.CENTER_VERTICAL);
        input.setBackgroundColor(Color.rgb(243, 244, 246));
        input.setPadding(dp(12), dp(8), dp(12), dp(8));
        composer.addView(input, new LinearLayout.LayoutParams(0, -2, 1));
        send = smallButton("ارسال");
        send.setTextColor(Color.WHITE);
        send.setBackgroundColor(BLUE);
        send.setOnClickListener(v -> sendMessage());
        composer.addView(send);
        root.addView(composer);
        return root;
    }

    TextView label(String text, int size, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(text); t.setTextSize(size); t.setTextColor(color); t.setGravity(Gravity.RIGHT);
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    Button smallButton(String text) {
        Button b = new Button(this);
        b.setText(text); b.setTextSize(13); b.setAllCaps(false);
        b.setPadding(dp(8), 0, dp(8), 0);
        return b;
    }

    void showSettings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), 0, dp(8), 0);
        EditText key = settingInput("API Key", prefs.getString("key", ""));
        EditText url = settingInput("Base URL مثل https://example.com/v1", prefs.getString("url", ""));
        EditText model = settingInput("Model مثل gpt-5.5", prefs.getString("model", "gpt-5.5"));
        box.addView(key); box.addView(url); box.addView(model);
        new AlertDialog.Builder(this)
                .setTitle("تنظیمات اتصال")
                .setView(box)
                .setPositiveButton("ذخیره", (d, w) -> {
                    prefs.edit().putString("key", key.getText().toString().trim())
                            .putString("url", url.getText().toString().trim())
                            .putString("model", model.getText().toString().trim()).apply();
                    addBot("تنظیمات ذخیره شد.");
                })
                .setNegativeButton("لغو", null)
                .show();
    }

    EditText settingInput(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint); e.setText(value); e.setSingleLine(true);
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setGravity(Gravity.LEFT);
        return e;
    }

    void sendMessage() {
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;
        String key = prefs.getString("key", "");
        String base = prefs.getString("url", "");
        String model = prefs.getString("model", "gpt-5.5");
        if (key.isEmpty() || base.isEmpty() || model.isEmpty()) {
            addBot("اول از تنظیمات، API Key و Base URL و Model را وارد کن.");
            return;
        }
        input.setText(""); hideKeyboard(); addUser(msg);
        send.setEnabled(false);
        TextView pending = addBot("در حال نوشتن...");
        pool.execute(() -> {
            String ans = callApi(key, base, model, msg);
            ui.post(() -> { pending.setText(ans); send.setEnabled(true); scrollBottom(); });
        });
    }

    void addUser(String text) { addBubble(text, true); }
    TextView addBot(String text) { return addBubble(text, false); }

    TextView addBubble(String text, boolean user) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(user ? Gravity.LEFT : Gravity.RIGHT);
        row.setPadding(0, dp(5), 0, dp(5));
        TextView bubble = new TextView(this);
        bubble.setText(text);
        bubble.setTextSize(15);
        bubble.setTextColor(user ? Color.WHITE : TEXT);
        bubble.setTextDirection(View.TEXT_DIRECTION_RTL);
        bubble.setGravity(Gravity.RIGHT);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        bubble.setBackgroundColor(user ? BLUE : CARD);
        row.addView(bubble, new LinearLayout.LayoutParams((int)(getResources().getDisplayMetrics().widthPixels * 0.78), -2));
        messages.addView(row);
        scrollBottom();
        return bubble;
    }

    void openUpdates() {
        new AlertDialog.Builder(this)
                .setTitle("آپدیت برنامه")
                .setMessage("نسخه فعلی: " + VERSION + "\nبرای دانلود نسخه‌های جدید، صفحه Releases باز می‌شود.")
                .setPositiveButton("باز کردن", (d, w) -> startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(RELEASES_URL))))
                .setNegativeButton("بعداً", null)
                .show();
    }

    String callApi(String key, String base, String model, String msg) {
        HttpURLConnection c = null;
        try {
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            c = (HttpURLConnection)new URL(base + "/chat/completions").openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
            c.setConnectTimeout(30000); c.setReadTimeout(60000);
            c.setRequestProperty("Authorization", "Bearer " + key);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            JSONObject body = new JSONObject();
            body.put("model", model);
            body.put("messages", new JSONArray().put(new JSONObject().put("role", "user").put("content", msg)));
            try(OutputStream os = c.getOutputStream()) { os.write(body.toString().getBytes(StandardCharsets.UTF_8)); }
            int code = c.getResponseCode();
            String res = read(code >= 200 && code < 300 ? c.getInputStream() : c.getErrorStream());
            if (code < 200 || code >= 300) return "خطای API " + code + "\n" + res;
            return new JSONObject(res).getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");
        } catch(Exception e) { return "خطا: " + e.getMessage(); }
        finally { if (c != null) c.disconnect(); }
    }

    String read(InputStream s) throws Exception {
        if (s == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(s, StandardCharsets.UTF_8));
        StringBuilder b = new StringBuilder(); String line;
        while((line = r.readLine()) != null) b.append(line).append('\n');
        return b.toString();
    }

    void scrollBottom() { ui.postDelayed(() -> scroll.fullScroll(View.FOCUS_DOWN), 100); }
    void hideKeyboard() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(input.getWindowToken(), 0); }
    int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
