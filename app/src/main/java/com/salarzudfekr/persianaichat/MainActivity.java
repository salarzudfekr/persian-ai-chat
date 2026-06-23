package com.salarzudfekr.persianaichat;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends Activity {
    static final String VERSION = "2.0.0";
    static final String REL = "https://github.com/salarzudfekr/persian-ai-chat/releases/latest";
    static final int BLUE = Color.rgb(37, 99, 235);
    static final int BG_LIGHT = Color.rgb(248, 250, 252);
    static final int BG_DARK = Color.rgb(15, 23, 42);
    static final int HEADER_LIGHT = Color.rgb(255, 255, 255);
    static final int HEADER_DARK = Color.rgb(15, 23, 42);
    static final int SURFACE_DARK = Color.rgb(30, 41, 59);
    static final int TEXT_PRIMARY_LIGHT = Color.rgb(15, 23, 42);
    static final int TEXT_PRIMARY_DARK = Color.rgb(248, 250, 252);
    static final int TEXT_SECONDARY_LIGHT = Color.rgb(100, 116, 139);
    static final int TEXT_SECONDARY_DARK = Color.rgb(148, 163, 184);

    RecyclerView recycler;
    EditText input;
    ImageButton sendBtn;
    TextView headerTitle, headerSubtitle, modelStatus;
    LinearLayout root, header, composeBar;
    SharedPreferences prefs;
    ExecutorService executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());
    Gson gson = new Gson();
    List<Message> messages = new ArrayList<>();
    ChatAdapter adapter;
    LinearLayoutManager layoutManager;
    boolean isDark = false;

    @Override
    public void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("s", 0);
        isDark = isSystemDark();
        if (isDark) setTheme(android.R.style.Theme_Material_NoActionBar);

        loadMessages();
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        setContentView(buildUI());
        if (messages.isEmpty()) {
            addWelcome();
        }
        adapter.notifyDataSetChanged();
        recycler.scrollToPosition(messages.size() - 1);
    }

    boolean isSystemDark() {
        int night = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        return night == Configuration.UI_MODE_NIGHT_YES;
    }

    // ─────────────────────────────── UI ───────────────────────────────
    View buildUI() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(isDark ? BG_DARK : BG_LIGHT);
        root.setFitsSystemWindows(true);

        root.addView(buildHeader());
        root.addView(buildChat(), new LinearLayout.LayoutParams(-1, 0, 1));
        root.addView(buildCompose());
        this.root = root;
        return root;
    }

    View buildHeader() {
        header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(12), dp(6), dp(6), dp(4));
        header.setBackgroundColor(isDark ? HEADER_DARK : HEADER_LIGHT);
        header.setElevation(dp(2));

        // Avatar icon
        LinearLayout avatarBox = new LinearLayout(this);
        avatarBox.setGravity(Gravity.CENTER);
        avatarBox.setBackground(getCircle(BLUE, dp(38)));
        avatarBox.setPadding(0, 0, 0, 0);
        TextView aiIcon = new TextView(this);
        aiIcon.setText("\uD83E\uDD16");
        aiIcon.setTextSize(20);
        aiIcon.setGravity(Gravity.CENTER);
        aiIcon.setLayoutParams(new LinearLayout.LayoutParams(dp(38), dp(38)));
        aiIcon.setGravity(Gravity.CENTER);
        avatarBox.addView(aiIcon);
        header.addView(avatarBox);

        // Title and subtitle
        LinearLayout titles = new LinearLayout(this);
        titles.setOrientation(LinearLayout.VERTICAL);
        titles.setPadding(dp(10), 0, 0, 0);
        headerTitle = new TextView(this);
        headerTitle.setText("چت فارسی AI");
        headerTitle.setTextSize(17);
        headerTitle.setTextColor(isDark ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT);
        headerTitle.setGravity(Gravity.RIGHT);
        headerTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        titles.addView(headerTitle);

        headerSubtitle = new TextView(this);
        headerSubtitle.setTextSize(11);
        headerSubtitle.setTextColor(isDark ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT);
        headerSubtitle.setGravity(Gravity.RIGHT);
        String mdl = prefs.getString("model", "gpt-5.5");
        headerSubtitle.setText(mdl + " • online");
        titles.addView(headerSubtitle);
        header.addView(titles, new LinearLayout.LayoutParams(0, -2, 1));

        // Model status dot
        modelStatus = new TextView(this);
        modelStatus.setText("●");
        modelStatus.setTextSize(10);
        modelStatus.setTextColor(Color.rgb(34, 197, 94));
        modelStatus.setPadding(dp(6), 0, dp(6), 0);
        header.addView(modelStatus);

        // Menu button
        ImageButton menuBtn = new ImageButton(this);
        menuBtn.setImageResource(android.R.drawable.ic_menu_more);
        menuBtn.setBackgroundColor(Color.TRANSPARENT);
        menuBtn.setOnClickListener(v -> showMenu(v));
        header.addView(menuBtn);

        return header;
    }

    void showMenu(View anchor) {
        PopupMenu popup = new PopupMenu(this, anchor, Gravity.END);
        popup.getMenu().add("تنظیمات");
        popup.getMenu().add(isDark ? "حالت روشن ☀️" : "حالت تیره 🌙");
        popup.getMenu().add("پاک کردن گفتگو");
        popup.getMenu().add("آپدیت برنامه");
        popup.setOnMenuItemClickListener(item -> {
            String title = item.getTitle().toString();
            if (title.startsWith("تنظیمات")) settings();
            else if (title.contains("تیره") || title.contains("روشن")) toggleTheme();
            else if (title.startsWith("پاک")) clearChat();
            else if (title.startsWith("آپدیت")) updates();
            return true;
        });
        popup.show();
    }

    void toggleTheme() {
        isDark = !isDark;
        adapter.setDark(isDark);
        root.setBackgroundColor(isDark ? BG_DARK : BG_LIGHT);
        header.setBackgroundColor(isDark ? HEADER_DARK : HEADER_LIGHT);
        headerTitle.setTextColor(isDark ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT);
        headerSubtitle.setTextColor(isDark ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT);
        composeBar.setBackgroundColor(isDark ? HEADER_DARK : HEADER_LIGHT);
        input.setTextColor(isDark ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT);
        input.setBackgroundColor(isDark ? SURFACE_DARK : Color.rgb(241, 245, 249));
        input.setHintTextColor(isDark ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT);
        sendBtn.setColorFilter(isDark ? Color.WHITE : Color.WHITE);
        adapter.notifyDataSetChanged();
    }

    View buildChat() {
        recycler = new RecyclerView(this);
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recycler.setLayoutManager(layoutManager);
        recycler.setPadding(0, dp(4), 0, dp(4));
        recycler.setClipToPadding(false);
        adapter = new ChatAdapter(this, messages, isDark);
        recycler.setAdapter(adapter);
        recycler.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (b < ob && adapter.getItemCount() > 0) {
                int lastVisible = layoutManager.findLastCompletelyVisibleItemPosition();
                if (lastVisible >= adapter.getItemCount() - 2) {
                    recycler.smoothScrollToPosition(adapter.getItemCount() - 1);
                }
            }
        });
        return recycler;
    }

    View buildCompose() {
        composeBar = new LinearLayout(this);
        composeBar.setGravity(Gravity.CENTER_VERTICAL);
        composeBar.setPadding(dp(10), dp(6), dp(10), dp(10));
        composeBar.setBackgroundColor(isDark ? HEADER_DARK : HEADER_LIGHT);
        composeBar.setElevation(dp(4));

        // Input field
        LinearLayout inputWrap = new LinearLayout(this);
        inputWrap.setOrientation(LinearLayout.HORIZONTAL);
        inputWrap.setGravity(Gravity.CENTER_VERTICAL);
        GradientDrawable inputBg = new GradientDrawable();
        inputBg.setColor(isDark ? SURFACE_DARK : Color.rgb(241, 245, 249));
        inputBg.setCornerRadius(dp(24));
        inputWrap.setBackground(inputBg);
        inputWrap.setPadding(dp(4), dp(2), dp(2), dp(2));

        input = new EditText(this);
        input.setHint("پیام خود را بنویسید...");
        input.setHintTextColor(isDark ? TEXT_SECONDARY_DARK : TEXT_SECONDARY_LIGHT);
        input.setTextColor(isDark ? TEXT_PRIMARY_DARK : TEXT_PRIMARY_LIGHT);
        input.setMinLines(1);
        input.setMaxLines(5);
        input.setGravity(Gravity.RIGHT);
        input.setTextDirection(View.TEXT_DIRECTION_RTL);
        input.setBackgroundColor(Color.TRANSPARENT);
        input.setPadding(dp(16), dp(10), dp(8), dp(10));
        input.setLayoutParams(new LinearLayout.LayoutParams(0, -2, 1));

        inputWrap.addView(input);

        sendBtn = new ImageButton(this);
        sendBtn.setImageResource(android.R.drawable.ic_menu_send);
        sendBtn.setBackground(getCircle(BLUE, dp(40)));
        sendBtn.setPadding(dp(6), dp(6), dp(6), dp(6));
        sendBtn.setColorFilter(Color.WHITE);
        sendBtn.setOnClickListener(v -> send());
        inputWrap.addView(sendBtn);

        composeBar.addView(inputWrap, new LinearLayout.LayoutParams(-1, -2));
        return composeBar;
    }

    // ─────────────────────────────── Settings ───────────────────────────────
    void settings() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(8), dp(6), dp(8), dp(6));

        TextView titleKey = lbl("API Key");
        EditText key = field("sk-...", prefs.getString("key", ""));
        TextView titleUrl = lbl("Base URL");
        EditText url = field("https://api.openai.com/v1", prefs.getString("url", ""));
        TextView titleModel = lbl("Model");
        EditText model = field("gpt-5.5", prefs.getString("model", "gpt-5.5"));

        box.addView(titleKey);
        box.addView(key);
        box.addView(titleUrl);
        box.addView(url);
        box.addView(titleModel);
        box.addView(model);

        AlertDialog.Builder dlg = new AlertDialog.Builder(this);
        dlg.setTitle("⚙️ تنظیمات اتصال");
        dlg.setView(box);
        dlg.setPositiveButton("ذخیره", (d, w) -> {
            String k = key.getText().toString().trim();
            String u = url.getText().toString().trim();
            String m = model.getText().toString().trim();
            prefs.edit().putString("key", k)
                  .putString("url", u)
                  .putString("model", m).apply();
            headerSubtitle.setText(m + " • online");
            addSystem("✔ تنظیمات ذخیره شد.");
        });
        dlg.setNegativeButton("لغو", null);
        dlg.show();
    }

    void updates() {
        new AlertDialog.Builder(this)
            .setTitle("🔄 آپدیت برنامه")
            .setMessage("نسخه فعلی: " + VERSION + "
برای دریافت نسخه جدید به صفحه Releases بروید.")
            .setPositiveButton("باز کردن", (d, w) ->
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(REL))))
            .setNegativeButton("بعداً", null)
            .show();
    }

    // ─────────────────────────────── Chat Logic ───────────────────────────────
    void send() {
        String msg = input.getText().toString().trim();
        if (msg.isEmpty()) return;

        String key = prefs.getString("key", "");
        String url = prefs.getString("url", "");
        String modelName = prefs.getString("model", "gpt-5.5");

        if (key.isEmpty() || url.isEmpty()) {
            addSystem("⚠️ ابتدا تنظیمات اتصال را تکمیل کنید.");
            return;
        }

        input.setText("");
        hideKeyboard();

        // Add user message
        Message userMsg = new Message(Message.ROLE_USER, msg);
        messages.add(userMsg);
        adapter.notifyItemInserted(messages.size() - 1);
        recycler.smoothScrollToPosition(messages.size() - 1);
        saveMessages();

        // Add loading indicator
        Message loading = new Message(Message.ROLE_LOADING, "");
        messages.add(loading);
        int loadingPos = messages.size() - 1;
        adapter.notifyItemInserted(loadingPos);
        recycler.smoothScrollToPosition(loadingPos);

        sendBtn.setEnabled(false);
        modelStatus.setTextColor(Color.rgb(251, 191, 36));

        executor.execute(() -> {
            String result = callAPI(key, url, modelName, msg);
            handler.post(() -> {
                messages.remove(loadingPos);
                adapter.notifyItemRemoved(loadingPos);

                Message botMsg = new Message(Message.ROLE_BOT, result);
                messages.add(botMsg);
                adapter.notifyItemInserted(messages.size() - 1);
                recycler.smoothScrollToPosition(messages.size() - 1);
                saveMessages();

                sendBtn.setEnabled(true);
                modelStatus.setTextColor(Color.rgb(34, 197, 94));
            });
        });
    }

    String callAPI(String key, String base, String modelName, String msg) {
        HttpURLConnection conn = null;
        try {
            if (base.endsWith("/")) base = base.substring(0, base.length() - 1);
            conn = (HttpURLConnection) new URL(base + "/chat/completions").openConnection();
            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(120000);
            conn.setRequestProperty("Authorization", "Bearer " + key);
            conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");

            JSONObject body = new JSONObject();
            body.put("model", modelName);
            JSONArray msgs = new JSONArray();
            // Send last 20 messages for context
            int start = Math.max(0, messages.size() - 21);
            for (int i = start; i < messages.size(); i++) {
                Message m = messages.get(i);
                if (m.role == Message.ROLE_LOADING) continue;
                msgs.put(new JSONObject()
                    .put("role", m.role == Message.ROLE_USER ? "user" : "assistant")
                    .put("content", m.content));
            }
            msgs.put(new JSONObject().put("role", "user").put("content", msg));
            body.put("messages", msgs);

            try (OutputStream os = conn.getOutputStream()) {
                os.write(body.toString().getBytes(StandardCharsets.UTF_8));
            }

            int code = conn.getResponseCode();
            String res = readStream(code >= 200 && code < 300 ? conn.getInputStream() : conn.getErrorStream());

            if (code < 200 || code >= 300) return "❌ خطای API " + code + "
" + res;
            return new JSONObject(res).getJSONArray("choices")
                .getJSONObject(0).getJSONObject("message").getString("content");
        } catch (Exception e) {
            return "❌ خطا: " + e.getMessage();
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    String readStream(InputStream is) throws Exception {
        if (is == null) return "";
        BufferedReader r = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = r.readLine()) != null) sb.append(line).append(n);
        return sb.toString();
    }

    // ─────────────────────────────── History ───────────────────────────────
    void loadMessages() {
        String json = prefs.getString("messages", "[]");
        Type type = new TypeToken<List<Message>>() {}.getType();
        try {
            messages = gson.fromJson(json, type);
            if (messages == null) messages = new ArrayList<>();
        } catch (Exception e) {
            messages = new ArrayList<>();
        }
    }

    void saveMessages() {
        prefs.edit().putString("messages", gson.toJson(messages)).apply();
    }

    void clearChat() {
        new AlertDialog.Builder(this)
            .setTitle("پاک کردن گفتگو")
            .setMessage("آیا مطمئن هستید که می\u200Cخواهید همه پیام\u200Cها پاک شوند؟")
            .setPositiveButton("بله", (d, w) -> {
                messages.clear();
                adapter.notifyDataSetChanged();
                saveMessages();
                addWelcome();
                Toast.makeText(this, "گفتگو پاک شد", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("لغو", null)
            .show();
    }

    void addWelcome() {
        messages.add(new Message(Message.ROLE_BOT,
            "👋 سلام! به چت فارسی AI خوش آمدید.

" +
            "✨ ویژگی\u200Cهای نسخه ۲:
" +
            "• طراحی مدرن و حرفه\u200Cای
" +
            "• پشتیبانی از حالت تیره و روشن
" +
            "• ذخیره خودکار تاریخچه
" +
            "• کپی پیام با لمس طولانی

" +
            "⚙️ از منوی بالای صفحه، تنظیمات را باز کنید و اطلاعات اتصال را وارد نمایید."));
        saveMessages();
    }

    void addSystem(String msg) {
        messages.add(new Message(Message.ROLE_BOT, msg));
        adapter.notifyItemInserted(messages.size() - 1);
        recycler.smoothScrollToPosition(messages.size() - 1);
        saveMessages();
    }

    // ─────────────────────────────── Helpers ───────────────────────────────
    TextView lbl(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(13);
        t.setTextColor(isDark ? Color.rgb(203, 213, 225) : Color.rgb(71, 85, 105));
        t.setPadding(0, dp(10), 0, dp(4));
        t.setGravity(Gravity.RIGHT);
        return t;
    }

    EditText field(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextDirection(View.TEXT_DIRECTION_LTR);
        e.setGravity(Gravity.LEFT);
        e.setPadding(dp(12), dp(10), dp(12), dp(10));
        e.setBackgroundColor(Color.rgb(241, 245, 249));
        return e;
    }

    GradientDrawable getCircle(int color, int size) {
        GradientDrawable gd = new GradientDrawable();
        gd.setShape(GradientDrawable.OVAL);
        gd.setColor(color);
        gd.setSize(size, size);
        return gd;
    }

    void hideKeyboard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(input.getWindowToken(), 0);
    }

    int dp(int v) {
        return (int) (v * getResources().getDisplayMetrics().density);
    }
}
