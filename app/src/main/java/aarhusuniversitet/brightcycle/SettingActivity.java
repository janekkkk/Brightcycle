package aarhusuniversitet.brightcycle;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.Toast;

import butterknife.BindView;
import butterknife.ButterKnife;

public class SettingActivity extends AppCompatActivity {

    @BindView(R.id.autoBright)
    Switch autoBright;
    @BindView(R.id.manuellBright)
    SeekBar manuellBright;
    @BindView(R.id.btnDisconnect)
    Button disconnect;
    BluetoothConnection bluetoothConnection;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setting);

        ButterKnife.bind(this);
        disconnect.setOnClickListener(detach);

        manuellBright.setEnabled(false);

        bluetoothConnection = BluetoothConnection.getInstance(this);
        autoBright.setChecked(true);
        autoBright.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                manuellBright.setEnabled(false);
                //TODO send microController the information that it change the brightness by itself
            } else {
                manuellBright.setEnabled(true);
            }
        });

        manuellBright.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressChanged = 0;

            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressChanged = progress;
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                //TODO Auto-generated method stub
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                Toast.makeText(SettingActivity.this, "Brightness: " + progressChanged + "%", Toast.LENGTH_LONG).show();
                //TODO Send brightness data to mC
            }
        });

    }

    View.OnClickListener detach = new View.OnClickListener() {
        public void onClick(View v) {
            // TODO disconnect the Bluetooth connection
            bluetoothConnection.disconnect();
        }
    };
}
