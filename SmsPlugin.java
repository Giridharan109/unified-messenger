package com.giri.unifiedmessenger;

import android.Manifest;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.telephony.SmsManager;
import com.getcapacitor.JSArray;
import com.getcapacitor.JSObject;
import com.getcapacitor.PermissionState;
import com.getcapacitor.Plugin;
import com.getcapacitor.PluginCall;
import com.getcapacitor.PluginMethod;
import com.getcapacitor.annotation.CapacitorPlugin;
import com.getcapacitor.annotation.Permission;
import java.util.ArrayList;

@CapacitorPlugin(
    name = "Sms",
    permissions = {
        @Permission(strings = {Manifest.permission.READ_SMS}, alias = "read"),
        @Permission(strings = {Manifest.permission.SEND_SMS}, alias = "send")
    }
)
public class SmsPlugin extends Plugin {

    @PluginMethod
    public void getInbox(PluginCall call) {
        if (getPermissionState("read") != PermissionState.GRANTED) {
            call.reject("READ_SMS not granted");
            return;
        }
        int limit = call.getInt("limit", 300);
        JSArray arr = new JSArray();
        try (Cursor c = getContext().getContentResolver().query(
                Uri.parse("content://sms"),
                new String[]{"address", "body", "date", "type"},
                null, null, "date DESC")) {
            while (c != null && c.moveToNext() && arr.length() < limit) {
                int type = c.getInt(3);
                if (type != 1 && type != 2) continue; // inbox / sent only
                JSObject o = new JSObject();
                o.put("address", c.getString(0));
                o.put("body", c.getString(1));
                o.put("date", c.getLong(2));
                o.put("type", type);
                arr.put(o);
            }
        } catch (Exception e) {
            call.reject(e.getMessage());
            return;
        }
        JSObject res = new JSObject();
        res.put("messages", arr);
        call.resolve(res);
    }

    @PluginMethod
    public void send(PluginCall call) {
        if (getPermissionState("send") != PermissionState.GRANTED) {
            call.reject("SEND_SMS not granted");
            return;
        }
        String to = call.getString("to");
        String body = call.getString("body");
        if (to == null || body == null) {
            call.reject("to and body required");
            return;
        }
        try {
            SmsManager sm = Build.VERSION.SDK_INT >= 31
                ? getContext().getSystemService(SmsManager.class)
                : SmsManager.getDefault();
            ArrayList<String> parts = sm.divideMessage(body);
            sm.sendMultipartTextMessage(to, null, parts, null, null);
            call.resolve();
        } catch (Exception e) {
            call.reject(e.getMessage());
        }
    }
}
