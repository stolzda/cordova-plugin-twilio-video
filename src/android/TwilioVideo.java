package org.apache.cordova.twiliovideo;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.LOG;
import org.apache.cordova.PluginResult;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import org.json.JSONObject;

public class TwilioVideo extends CordovaPlugin {

    public static final String TAG = "TwilioPlugin";

    public CallbackContext callbackContext;
    private CordovaInterface cordova;
    private String roomId;
    private String token;
    private CallConfig config = new CallConfig();

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        this.cordova = cordova;
    }

    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        this.callbackContext = callbackContext;
        if (action.equals("openRoom")) {
            this.registerCallListener(callbackContext);
            this.openRoom(args);
        } else if (action.equals("closeRoom")) {
            this.closeRoom(callbackContext);
        } else if (action.equals("hasRequiredPermissions")) {
            this.hasRequiredPermissions(callbackContext);
        }
        return true;
    }

    public void openRoom(final JSONArray args) {
        try {
            this.token = args.getString(0);
            this.roomId = args.getString(1);
            final CordovaPlugin that = this;
            final String token = this.token;
            final String roomId = this.roomId;
            if (args.length() > 2) {
                this.config.parse(args.getJSONObject(2));
            }

            LOG.d(TAG, "TOKEN: " + token);
            LOG.d(TAG, "ROOMID: " + roomId);

            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    Intent intentTwilioVideo = new Intent(Intent.ACTION_VIEW);
                    intentTwilioVideo.setClass(that.cordova.getActivity().getBaseContext(), TwilioVideoActivity.class);
                    intentTwilioVideo.setPackage(that.cordova.getActivity().getApplicationContext().getPackageName());
                    intentTwilioVideo.putExtra("token", token);
                    intentTwilioVideo.putExtra("roomId", roomId);
                    intentTwilioVideo.putExtra("config", config);
                    that.cordova.getActivity().startActivity(intentTwilioVideo);
                }

            });
        } catch (JSONException e) {
            Log.e(TAG, "Couldn't open room. No valid input params", e);
        }
    }

    private void registerCallListener(final CallbackContext callbackContext) {
        if (callbackContext == null) {
            return;
        }
        TwilioVideoManager.getInstance().setEventObserver(new CallEventObserver() {
            @Override
            public void onEvent(String event, JSONObject data) {
                Log.i(TAG, String.format("Event received: %s with data: %s", event, data));

                JSONObject eventData = new JSONObject();
                try {
                    eventData.putOpt("event", event);
                    eventData.putOpt("data", data);
                } catch (JSONException e) {
                    Log.e(TAG, "Failed to create event: " + event);
                    return;
                }

                PluginResult result = new PluginResult(PluginResult.Status.OK, eventData);
                result.setKeepCallback(true);
                callbackContext.sendPluginResult(result);
            }
        });
    }

    private void closeRoom(CallbackContext callbackContext) {
        if (TwilioVideoManager.getInstance().publishDisconnection()) {
            callbackContext.success();
        } else {
            callbackContext.error("Twilio video is not running");
        }
    }

    public void hasRequiredPermissions(CallbackContext callbackContext) {
        switch (ContextCompat.checkSelfPermission(cordova.getActivity(), Manifest.permission.CAMERA)) {
            case PackageManager.PERMISSION_GRANTED:
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, true));
            case PackageManager.PERMISSION_DENIED:
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, false));
            default:
                callbackContext.error("Something goes wrong on Twilio permissions!");
        }
    }

    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();
        state.putString("token", this.token);
        state.putString("roomId", this.roomId);
        state.putSerializable("config", this.config);
        return state;
    }

    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.token = state.getString("token");
        this.roomId = state.getString("roomId");
        this.config = (CallConfig) state.getSerializable("config");
        this.callbackContext = callbackContext;
    }

}
