package com.abdickers.commandcenter;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.RemoteViews;

public class RealmWidget extends AppWidgetProvider {
    private static final String SWITCH = "com.abdickers.commandcenter.SWITCH_REALM";
    static String selected(Context context) { return Realms.normalize(context.getSharedPreferences("realms",0).getString("selected",Realms.PERSONAL)); }
    static void select(Context context,String realm) { context.getSharedPreferences("realms",0).edit().putString("selected",Realms.normalize(realm)).apply(); updateAll(context); }
    static void updateAll(Context context) {
        AppWidgetManager manager=AppWidgetManager.getInstance(context);
        for(int id:manager.getAppWidgetIds(new ComponentName(context,RealmWidget.class))) update(context,manager,id);
    }
    @Override public void onUpdate(Context context,AppWidgetManager manager,int[] ids){for(int id:ids)update(context,manager,id);}
    @Override public void onAppWidgetOptionsChanged(Context c,AppWidgetManager m,int id,Bundle options){update(c,m,id);}
    @Override public void onReceive(Context context,Intent intent){
        super.onReceive(context,intent);
        if(SWITCH.equals(intent.getAction()))select(context,intent.getStringExtra("realm"));
    }
    private static PendingIntent switchIntent(Context c,String realm,int code){
        Intent intent=new Intent(c,RealmWidget.class).setAction(SWITCH).putExtra("realm",realm);
        return PendingIntent.getBroadcast(c,code,intent,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
    }
    private static void update(Context c,AppWidgetManager m,int id){
        String realm=selected(c); boolean work=Realms.WORK.equals(realm);
        RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.realm_widget);
        v.setTextViewText(R.id.widget_title,Realms.title(realm));
        v.setTextViewText(R.id.widget_subtitle,work?"Work command center":"Personal command center");
        v.setTextViewText(R.id.widget_personal,work?"PERSONAL":"✦ PERSONAL");
        v.setTextViewText(R.id.widget_work,work?"✦ WORK":"WORK");
        v.setTextColor(R.id.widget_personal,Color.parseColor(work?"#B4BBA5":"#FFE7A6"));
        v.setTextColor(R.id.widget_work,Color.parseColor(work?"#FFE7A6":"#B4BBA5"));
        v.setOnClickPendingIntent(R.id.widget_personal,switchIntent(c,Realms.PERSONAL,10));
        v.setOnClickPendingIntent(R.id.widget_work,switchIntent(c,Realms.WORK,11));
        Intent open=new Intent(c,MainActivity.class).setAction("OPEN_REALM").putExtra("realm",realm);
        PendingIntent launch=PendingIntent.getActivity(c,20,open,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
        v.setOnClickPendingIntent(R.id.widget_open,launch);v.setOnClickPendingIntent(R.id.widget_launch,launch);
        m.updateAppWidget(id,v);
    }
}
