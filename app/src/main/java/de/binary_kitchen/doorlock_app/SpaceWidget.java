package de.binary_kitchen.doorlock_app;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SpaceWidget extends AppWidgetProvider {
    private final Request.Builder request_uri =
            new Request.Builder().url("https://www.binary-kitchen.de/spaceapi.php");
    private final OkHttpClient client = new OkHttpClient();
    private static boolean open, state_valid;

    private final static int c_green = android.graphics.Color.rgb(0x33, 0xe5, 0x63);
    private final static int c_red = android.graphics.Color.rgb(0xe4, 0x33, 0x5c);
    private final static int c_unknown = android.graphics.Color.rgb(0xe5, 0x33, 0xb5);


    private static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                        int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.space_widget);
        int resource = R.drawable.ic_unknown;
        int color = c_unknown;

        if (state_valid)
            if (open) {
                resource = R.drawable.ic_unlock;
                color = c_green;
            } else {
                resource = R.drawable.ic_lock;
                color = c_red;
            }

        views.setImageViewResource(R.id.state_button, resource);
        views.setInt(R.id.text, "setBackgroundColor", color);

        Intent intentUpdate = new Intent(context, SpaceWidget.class);
        intentUpdate.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);

        int[] idArray = new int[]{appWidgetId};
       intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray);

        PendingIntent pendingUpdate = PendingIntent.getBroadcast(
                context, appWidgetId, intentUpdate,
                PendingIntent.FLAG_UPDATE_CURRENT);
        views.setOnClickPendingIntent(R.id.state_button, pendingUpdate);

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        SpaceAPICallback spaceAPICallback = new SpaceAPICallback(context);
        Request request = request_uri.get().build();
        client.newCall(request).enqueue(spaceAPICallback);
    }

    @Override
    public void onEnabled(Context context) {
    }

    @Override
    public void onDisabled(Context context) {
    }

    private class SpaceAPICallback implements Callback
    {
        final private Context context;

        private SpaceAPICallback(Context context)
        {
            this.context = context;
        }

        private void update()
        {
            AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, SpaceWidget.class));

            for (int appWidgetId : ids) {
                updateAppWidget(context, widgetManager, appWidgetId);
            }
        }

        @Override
        public void onFailure(Call call, final IOException e)
        {
            state_valid = false;
            update();
        }

        @Override
        public void onResponse(Call call, Response response) throws IOException
        {
            JsonParser parser = new JsonParser();
            String json_body;

            state_valid = false;
            if (response.code() == 200) {
                json_body = response.body().string();
                JsonObject obj = parser.parse(json_body).getAsJsonObject();
                open = obj.getAsJsonObject("state").get("open").getAsBoolean();
                state_valid = true;
            }

            update();
        }
    }
}