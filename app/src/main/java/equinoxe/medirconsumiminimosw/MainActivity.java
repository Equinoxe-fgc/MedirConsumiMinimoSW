package equinoxe.medirconsumiminimosw;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.PowerManager;
import android.support.wearable.activity.WearableActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;

import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends WearableActivity {
    final static long lPeriodoComprobacion = 120*1000;

    Timer timerGrabarDatos;
    DecimalFormat df;
    SimpleDateFormat sdf;
    FileOutputStream fOut;
    boolean bStarted = false;

    PowerManager powerManager;
    PowerManager.WakeLock wakeLock;

    private Button btnStart;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = (Button) findViewById(R.id.btnStart);

        df = new DecimalFormat("###.##");
        sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");

        checkForPermissions();

        final TimerTask timerTaskComprobarBateria = new TimerTask() {
            public void run() {
                if (bStarted)
                    grabarMedidas();
            }
        };

        timerGrabarDatos = new Timer();
        timerGrabarDatos.scheduleAtFixedRate(timerTaskComprobarBateria, lPeriodoComprobacion, lPeriodoComprobacion);

        // Enables Always-on
        setAmbientEnabled();
    }

    public void onClick(View v) {
        if (bStarted) {
            wakeLock.release();

            btnStart.setText(R.string.start);

            try {
                fOut.close();
            } catch (Exception e) {
                Toast.makeText(this, "Error al cerrar archivo.", Toast.LENGTH_LONG).show();
            }
        } else {
            powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            try {
                wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"MyApp::MyWakelockTag");
                wakeLock.acquire();
            } catch (NullPointerException e) {
                Log.e("NullPointerException", "ServiceDatos - onStartCommand");
            }

            btnStart.setText(R.string.stop);

            String sFichero = Environment.getExternalStorageDirectory() + "/" + android.os.Build.MODEL + "_Descarga.txt";
            String currentDateandTime = sdf.format(new Date());
            try {
                fOut = new FileOutputStream(sFichero, false);
                String sCadena = android.os.Build.MODEL + " " + currentDateandTime + "\n";
                fOut.write(sCadena.getBytes());
                fOut.flush();
            } catch (Exception e) {
                Toast.makeText(this, "Error al crear archivo: " + sFichero, Toast.LENGTH_LONG).show();
            }
        }

        bStarted = !bStarted;
    }

    @Override
    public void onDestroy() {
        wakeLock.release();
        timerGrabarDatos.cancel();

        timerGrabarDatos.cancel();
        grabarMedidas();
        try {
            //fLog.close();
            fOut.close();
        } catch (Exception e) {
            Log.e("Error - ", "Error cerrando fichero");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
        }

        super.onDestroy();
    }


    private void checkForPermissions() {
        String[] PERMISSIONS_STORAGE = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        };

            int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);
            if (permission != PackageManager.PERMISSION_GRANTED) {
                // We don't have permission so prompt the user
                ActivityCompat.requestPermissions(this, PERMISSIONS_STORAGE, 1);
            }
    }

    public void grabarMedidas() {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, ifilter);

        BatteryManager mBatteryManager = (BatteryManager) this.getSystemService(Context.BATTERY_SERVICE);

        try {
            String sCadena = sdf.format(new Date()) + ":" +
                    batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) + "\n";
            fOut.write(sCadena.getBytes());

        } catch (Exception e) {
            Log.e("Fichero de resultados", e.getMessage(), e);
        }
    }
}
