package com.salarzudfekr.persianaichat;

import android.animation.ValueAnimator;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int VIEW_TYPE_USER = 0;
    private static final int VIEW_TYPE_BOT = 1;
    private static final int VIEW_TYPE_LOADING = 2;

    private static final int USER_BUBBLE = Color.rgb(37, 99, 235);
    private static final int BOT_BUBBLE_LIGHT = Color.rgb(255, 255, 255);
    private static final int BOT_BUBBLE_DARK = Color.rgb(30, 41, 59);
    private static final int TEXT_USER = Color.rgb(255, 255, 255);
    private static final int TEXT_BOT_LIGHT = Color.rgb(15, 23, 42);
    private static final int TEXT_BOT_DARK = Color.rgb(226, 232, 240);
    private static final int TIME_LIGHT = Color.rgb(148, 163, 184);
    private static final int TIME_DARK = Color.rgb(100, 116, 139);

    private Context ctx;
    private List<Message> messages;
    private boolean isDark;

    public ChatAdapter(Context ctx, List<Message> messages, boolean isDark) {
        this.ctx = ctx;
        this.messages = messages;
        this.isDark = isDark;
    }

    public void setDark(boolean dark) { this.isDark = dark; notifyDataSetChanged(); }

    @Override
    public int getItemViewType(int pos) {
        Message m = messages.get(pos);
        if (m.role == Message.ROLE_USER) return VIEW_TYPE_USER;
        if (m.role == Message.ROLE_LOADING) return VIEW_TYPE_LOADING;
        return VIEW_TYPE_BOT;
    }

    @NonNull @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_USER) return new UserHolder(new ItemView(ctx));
        if (viewType == VIEW_TYPE_LOADING) return new LoadingHolder(new ItemView(ctx));
        return new BotHolder(new ItemView(ctx));
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int pos) {
        Message m = messages.get(pos);
        boolean dark = isDark;
        if (holder instanceof UserHolder) ((UserHolder) holder).bind(m, dark);
        else if (holder instanceof BotHolder) ((BotHolder) holder).bind(m, dark);
        else ((LoadingHolder) holder).bind(dark);
    }

    @Override public int getItemCount() { return messages.size(); }

    static class ItemView extends FrameLayout {
        ItemView(Context c) {
            super(c);
            setPadding(dp(12), dp(4), dp(12), dp(4));
            setLayoutDirection(LAYOUT_DIRECTION_RTL);
        }
        int dp(int v) { return (int)(v * getResources().getDisplayMetrics().density); }
    }

    class UserHolder extends RecyclerView.ViewHolder {
        LinearLayout row; TextView bubble, time;
        UserHolder(View v) {
            super(v);
            row = new LinearLayout(ctx);
            row.setGravity(Gravity.START);
            row.setLayoutDirection(LAYOUT_DIRECTION_LTR);
            ((FrameLayout) v).addView(row);

            LinearLayout wrap = new LinearLayout(ctx);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.setGravity(Gravity.END);
            bubble = new TextView(ctx);
            bubble.setTextSize(15);
            bubble.setTextColor(TEXT_USER);
            bubble.setTextDirection(TEXT_DIRECTION_RTL);
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
            bubble.setMaxWidth((int)(v.getResources().getDisplayMetrics().widthPixels * 0.78));
            wrap.addView(bubble);

            time = new TextView(ctx);
            time.setTextSize(10);
            time.setTextColor(TIME_LIGHT);
            time.setPadding(dp(8), dp(2), dp(8), 0);
            wrap.addView(time);
            row.addView(wrap);
        }
        void bind(Message m, boolean dark) {
            bubble.setText(m.content);
            time.setText(formatTime(m.timestamp));
            time.setTextColor(dark ? TIME_DARK : TIME_LIGHT);
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(USER_BUBBLE);
            bg.setCornerRadii(new float[]{dp(18), dp(18), dp(4), dp(18)});
            bubble.setBackground(bg);
        }
        int dp(int v) { return (int)(v * ctx.getResources().getDisplayMetrics().density); }
    }

    class BotHolder extends RecyclerView.ViewHolder implements View.OnLongClickListener {
        LinearLayout row; TextView avatar, bubble, time; ImageButton copy;
        BotHolder(View v) {
            super(v);
            row = new LinearLayout(ctx);
            row.setGravity(Gravity.END);
            row.setLayoutDirection(LAYOUT_DIRECTION_LTR);
            ((FrameLayout) v).addView(row);

            LinearLayout wrap = new LinearLayout(ctx);
            wrap.setOrientation(LinearLayout.VERTICAL);
            wrap.setGravity(Gravity.START);

            LinearLayout top = new LinearLayout(ctx);
            top.setGravity(Gravity.CENTER_VERTICAL);
            avatar = new TextView(ctx);
            avatar.setText(\"🤖\");
            avatar.setTextSize(20);
            avatar.setPadding(0, 0, dp(8), 0);
            avatar.setGravity(Gravity.CENTER);
            top.addView(avatar);

            bubble = new TextView(ctx);
            bubble.setTextSize(15);
            bubble.setTextColor(TEXT_BOT_LIGHT);
            bubble.setTextDirection(TEXT_DIRECTION_RTL);
            bubble.setPadding(dp(14), dp(10), dp(14), dp(10));
            bubble.setMaxWidth((int)(v.getResources().getDisplayMetrics().widthPixels * 0.7));
            top.addView(bubble);

            copy = new ImageButton(ctx);
            copy.setImageResource(android.R.drawable.ic_menu_share);
            copy.setBackgroundColor(Color.TRANSPARENT);
            copy.setPadding(dp(4), dp(4), dp(4), dp(4));
            copy.setVisibility(GONE);
            copy.setOnClickListener(cv -> copyText(messages.get(getAdapterPosition()).content));
            top.addView(copy);
            wrap.addView(top);

            time = new TextView(ctx);
            time.setTextSize(10);
            time.setTextColor(TIME_LIGHT);
            time.setPadding(dp(36), dp(2), dp(8), 0);
            wrap.addView(time);
            row.addView(wrap);

            bubble.setOnLongClickListener(this);
        }
        void bind(Message m, boolean dark) {
            bubble.setText(m.content);
            time.setText(formatTime(m.timestamp));
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(dark ? BOT_BUBBLE_DARK : BOT_BUBBLE_LIGHT);
            bg.setCornerRadii(new float[]{dp(4), dp(18), dp(18), dp(18)});
            bg.setStroke(dark ? 0 : 1, dark ? 0 : Color.rgb(226, 232, 240));
            bubble.setBackground(bg);
            bubble.setTextColor(dark ? TEXT_BOT_DARK : TEXT_BOT_LIGHT);
            time.setTextColor(dark ? TIME_DARK : TIME_LIGHT);
        }
        @Override public boolean onLongClick(View v) {
            copy.setVisibility(VISIBLE);
            return true;
        }
        void copyText(String txt) {
            ((ClipboardManager) ctx.getSystemService(Context.CLIPBOARD_SERVICE))
                .setPrimaryClip(ClipData.newPlainText(\"chat\", txt));
            Toast.makeText(ctx, \"کپی شد ✓\", Toast.LENGTH_SHORT).show();
        }
        int dp(int v) { return (int)(v * ctx.getResources().getDisplayMetrics().density); }
    }

    class LoadingHolder extends RecyclerView.ViewHolder {
        LinearLayout row; TextView avatar, dots; TextView bubble;
        LoadingHolder(View v) {
            super(v);
            row = new LinearLayout(ctx);
            row.setGravity(Gravity.END);
            row.setLayoutDirection(LAYOUT_DIRECTION_LTR);
            ((FrameLayout) v).addView(row);
            avatar = new TextView(ctx);
            avatar.setText(\"🤖\");
            avatar.setTextSize(20);
            avatar.setPadding(0, 0, dp(8), 0);
            row.addView(avatar);
            bubble = new TextView(ctx);
            bubble.setTextSize(15);
            bubble.setPadding(dp(16), dp(12), dp(16), dp(12));
            row.addView(bubble);
        }
        void bind(boolean dark) {
            GradientDrawable bg = new GradientDrawable();
            bg.setColor(dark ? BOT_BUBBLE_DARK : BOT_BUBBLE_LIGHT);
            bg.setCornerRadii(new float[]{dp(4), dp(18), dp(18), dp(18)});
            bg.setStroke(dark ? 0 : 1, dark ? 0 : Color.rgb(226, 232, 240));
            bubble.setBackground(bg);
            bubble.setTextColor(dark ? TEXT_BOT_DARK : TEXT_BOT_LIGHT);
            bubble.setText(\"● ○ ○\");
            animateDots();
        }
        void animateDots() {
            final String[] frames = {\"● ○ ○\", \"○ ● ○\", \"○ ○ ●\"};
            ValueAnimator anim = ValueAnimator.ofInt(0, 2);
            anim.setDuration(600);
            anim.setRepeatCount(ValueAnimator.INFINITE);
            anim.setRepeatMode(ValueAnimator.RESTART);
            anim.addUpdateListener(a -> bubble.setText(frames[(int) a.getAnimatedValue()]));
            anim.start();
        }
        int dp(int v) { return (int)(v * ctx.getResources().getDisplayMetrics().density); }
    }

    String formatTime(long ts) {
        return new SimpleDateFormat(\"HH:mm\", Locale.getDefault()).format(new Date(ts));
    }
}
