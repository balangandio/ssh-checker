package com.comxa.universo42.sshchecker;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v7.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

import com.comxa.universo42.sshchecker.modelo.SSH;
import com.comxa.universo42.sshchecker.modelo.SSHchecker;

import java.util.List;

public class CheckerService extends Service implements ServiceControl {
    public static final String SERVICE_BROADCAST_STR = "SERVICE_SSH_CHECKER";
    public static final int NOTINICATION_ID = 4242;

    private boolean isOnForegroud;
    private Controller controller = new Controller();
    private SSHchecker checker;

    private NotificationManager notificationManager;
    private Builder notificationBuilder;

    @Override
    public void onCreate() {}

    @Override
    public void onDestroy() {
        if (checker != null && checker.isRunning())
            checker.stop();
        if (notificationManager != null)
            notificationManager.cancel(NOTINICATION_ID);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return this.controller;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (isOnForegroud)
            return START_NOT_STICKY;

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);

        notificationBuilder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.checking)
                .setContentTitle(getString(R.string.checker_service))
                .setContentText("...")
                .setContentIntent(pendingIntent);

        startForeground(NOTINICATION_ID, notificationBuilder.build());
        isOnForegroud = true;

        return START_NOT_STICKY;
    }


    @Override
    public SSHchecker getChecker() {
        return this.checker;
    }

    @Override
    public void setChecker(List<SSH> sshs, int qtdThreads) {
        checker = new SSHchecker(sshs, qtdThreads) {
            @Override
            public void onLog(String str) {
                if (notificationBuilder != null) {
                    notificationBuilder.setContentText(str);
                    notificationManager.notify(NOTINICATION_ID, notificationBuilder.build());
                }
            }

            @Override
            public void onComplete() {
                if (notificationBuilder != null) {
                    notificationBuilder.setSmallIcon(R.drawable.checked);
                    notificationManager.notify(NOTINICATION_ID, notificationBuilder.build());
                }
            }
        };
    }


    public class Controller extends Binder {
        public ServiceControl getControl() {
            return CheckerService.this;
        }
    }
}
