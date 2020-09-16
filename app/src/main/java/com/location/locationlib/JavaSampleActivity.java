package com.location.locationlib;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import kotlin.Unit;

import static com.location.locationlib.LocationForegroundService.STOP_SERVICE_BROADCAST_ACTON;

public class JavaSampleActivity extends AppCompatActivity {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        findViewById(R.id.btnSingleLocation).setOnClickListener(view -> SLocationLib.getCurrentLocation(JavaSampleActivity.this, resultData -> {
            if (resultData.getLocation() != null)
                Log.d("LocationUpdate java", "" + resultData.getLocation().toString());
            return Unit.INSTANCE;
        }));
        findViewById(R.id.btnStartService).setOnClickListener(view -> {
                    //This for customize for permission dailog text
                    SLocationLib.permissionTextConfigure(permissionTextConfigurations -> {
                        permissionTextConfigurations.setRationaleText("Custome Title");
                        permissionTextConfigurations.setBlockedText("Custome Text");
                        return Unit.INSTANCE;
                    });
                    //This is for customize location request
                    SLocationLib.locationConfigure(locationConfigurations -> {
                        locationConfigurations.setEnableBackgroundUpdates(true);
                        locationConfigurations.setLocationUpdateIntervalInMS(50000);
                        locationConfigurations.setFastestLocationIntervalInMS(50000);
                        return Unit.INSTANCE;
                    });
                    SLocationLib.startLocationUpdates(JavaSampleActivity.this, resultData -> {
                        if (resultData.getLocation() != null)
                            Log.d("LocationUpdate java", "" + resultData.getLocation().toString());
                        return Unit.INSTANCE;
                    });
                }
        );
        findViewById(R.id.btnStartForeService).setOnClickListener(view ->
                startService(new Intent(this, LocationForegroundService.class)));

        findViewById(R.id.btnStopForeService).setOnClickListener(view -> {
                    Log.d("stop", "btnStopForeService");
                    Intent intent = new Intent(this, LocationServiceStopReceiver.class);
                    intent.setAction(STOP_SERVICE_BROADCAST_ACTON);
                    sendBroadcast(intent);
                }
        );

    }
}
