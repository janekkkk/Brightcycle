package aarhusuniversitet.brightcycle;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.Switch;

import aarhusuniversitet.brightcycle.Controller.DrivingInformation;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class SettingActivity extends AppCompatActivity {

    @BindView(R.id.autoBright)
    Switch autoBright;
    @BindView(R.id.manuellBright)
    SeekBar manualBrightness;
    @BindView(R.id.btnDisconnect)
    Button disconnect;
    BluetoothConnection bluetoothConnection;
    DrivingInformation drivingInformation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ButterKnife.bind(this);

        manualBrightness.setEnabled(false);

        bluetoothConnection = BluetoothConnection.getInstance(this);
        drivingInformation = DrivingInformation.getInstance(this, bluetoothConnection);

        autoBright.setChecked(true);
        autoBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                manualBrightness.setEnabled(false);
                drivingInformation.turnOnLightsAutomatically();
            } else {
                manualBrightness.setEnabled(true);
            }
        });

        manualBrightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                drivingInformation.setBrightnessLights(progressChanged);
            }
        });

    }

    @OnClick(R.id.btnDisconnect)
    public void disconnectButtonClicked(View button) {
        bluetoothConnection.disconnect();
    }

}
