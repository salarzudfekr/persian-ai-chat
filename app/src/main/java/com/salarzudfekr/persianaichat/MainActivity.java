package com.salarzudfekr.persianaichat;

import android.app.Activity;
import android.content.SharedPreferences;
import android.os.*;
import android.view.*;
import android.widget.*;
import org.json.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class MainActivity extends Activity {
    EditText keyBox, urlBox, modelBox, msgBox;
    TextView output;
    Button send;
    final ExecutorService pool = Executors.newSingleThreadExecutor();
    final Handler ui = new Handler(Looper.getMainLooper());

    public void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(screen());
        SharedPreferences p = getSharedPreferences("s", MODE_PRIVATE);
        keyBox.setText(p.getString("key", ""));
        urlBox.setText(p.getString("url", ""));
        modelBox.setText(p.getString("model", "gpt-5.5"));
    }

    View screen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));
        root.setGravity(Gravity.RIGHT);
        ScrollView scroll = new ScrollView(this);
        scroll.addView(root);

        TextView title = text("چت فارسی AI", 24);
        root.addView(title);
        keyBox = input("API Key");
        urlBox = input("Base URL مثل https://example.com/v1");
        modelBox = input("Model مثل gpt-5.5");
        msgBox = input("پیام خود را بنویسید");
        msgBox.setMinLines(3);
        root.addView(keyBox); root.addView(urlBox); root.addView(modelBox); root.addView(msgBox);
        send = new Button(this);
        send.setText("ارسال");
        send.setOnClickListener(v -> sendMessage());
        root.addView(send);
        output = text("تنظیمات API را وارد کنید و پیام بفرستید.", 16);
        root.addView(output);
        return scroll;
    }

    EditText input(String hint) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setGravity(Gravity.RIGHT);
        e.setSingleLine(false);
        e.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        return e;
    }

    TextView text(String s, int size) {
        TextView t = new TextView(this);
        t.setText(s); t.setTextSize(size); t.setGravity(Gravity.RIGHT); t.setPadding(0, dp(8), 0, dp(8));
        return t;
    }

    void sendMessage() {
        String key = keyBox.getText().toString().trim();
        String base = urlBox.getText().toString().trim();
        String model = modelBox.getText().toString().trim();
        String msg = msgBox.getText().toString().trim();
        if (key.isEmpty() || base.isEmpty() || model.isEmpty() || msg.isEmpty()) {
            output.setText("همه فیلدها را کامل کنید."); return;
        }
        getSharedPreferences("s", MODE_PRIVATE).edit().putString("key", key).putString("url", base).putString("model", model).apply();
        send.setEnabled(false); output.setText("در حال دریافت پاسخ...");
        pool.execute(() -> {
            String ans = callApi(key, base, model, msg);
            ui.post(() -> { send.setEnabled(true); output.setText("شما:\n" + msg + "\n\nAI:\n" + ans); });
        });
    }

    String callApi(String key, String base, String model, String msg) {
        HttpURLConnection c = null;
        try {
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            c = (HttpURLConnection)new URL(base + "/chat/completions").openConnection();
            c.setRequestMethod("POST"); c.setDoOutput(true);
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

    int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
}
