package alexkang.swing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NotificationBroadcastReceiver extends BroadcastReceiver {

    SwingService service = null;

    @Override
    public void onReceive(Context context, Intent intent) {
        service.stopSelf();
    }

    public void setActivityHandler(SwingService service) {
        this.service = service;
    }

}
