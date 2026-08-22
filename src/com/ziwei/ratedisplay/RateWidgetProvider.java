package com.ziwei.ratedisplay;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.widget.RemoteViews;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RateWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_REFRESH = "com.ziwei.ratedisplay.REFRESH";
    private static final String API_URL = "https://api.frankfurter.app/latest?from=USD&to=MYR";
    private static final String PREFS = "rate_state";
    private static final Pattern MYR = Pattern.compile("\\\"MYR\\\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] ids) {
        for (int id : ids) updateOne(context, manager, id, false);
        fetchAndRefresh(context);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_REFRESH.equals(intent.getAction())) {
            setAll(context, "Fetching…", "Updating mid-market rate");
            fetchAndRefresh(context);
        }
    }

    private void fetchAndRefresh(final Context context) {
        new Thread(new Runnable() {
            @Override public void run() {
                HttpURLConnection connection = null;
                try {
                    connection = (HttpURLConnection) new URL(API_URL).openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(10000);
                    connection.setRequestMethod("GET");
                    connection.setRequestProperty("Accept", "application/json");
                    if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) throw new Exception("HTTP " + connection.getResponseCode());
                    InputStream input = connection.getInputStream();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(input, "UTF-8"));
                    StringBuilder body = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) body.append(line);
                    Matcher match = MYR.matcher(body.toString());
                    if (!match.find()) throw new Exception("MYR rate missing");
                    String rate = match.group(1);
                    String updated = new SimpleDateFormat("HH:mm, dd MMM", Locale.getDefault()).format(new Date());
                    context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
                        .putString("rate", rate).putString("updated", updated).apply();
                    setAll(context, "RM " + rate, "Updated " + updated + " · mid-market");
                } catch (Exception error) {
                    String cached = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("rate", null);
                    String updated = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("updated", null);
                    if (cached != null) {
                        setAll(context, "RM " + cached, "Cached · " + updated + " · offline");
                    } else {
                        setAll(context, "Unavailable", "Tap ↻ to retry");
                    }
                } finally {
                    if (connection != null) connection.disconnect();
                }
            }
        }).start();
    }

    private static void setAll(Context context, String rate, String subtitle) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, RateWidgetProvider.class));
        for (int id : ids) update(context, manager, id, rate, subtitle);
    }

    private static void updateOne(Context context, AppWidgetManager manager, int id, boolean ignored) {
        String rate = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("rate", "Loading…");
        String updated = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString("updated", "Fetching mid-market rate");
        update(context, manager, id, rate.equals("Loading…") ? rate : "RM " + rate, updated);
    }

    private static void update(Context context, AppWidgetManager manager, int id, String rate, String subtitle) {
        RemoteViews views = new RemoteViews(context.getPackageName(), com.ziwei.ratedisplay.R.layout.widget_rate);
        views.setTextViewText(com.ziwei.ratedisplay.R.id.widget_rate, rate);
        views.setTextViewText(com.ziwei.ratedisplay.R.id.widget_updated, subtitle);
        Intent refresh = new Intent(context, RateWidgetProvider.class).setAction(ACTION_REFRESH);
        PendingIntent pending = PendingIntent.getBroadcast(context, id, refresh,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(com.ziwei.ratedisplay.R.id.widget_refresh, pending);
        manager.updateAppWidget(id, views);
    }
}
