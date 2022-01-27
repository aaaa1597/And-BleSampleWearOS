package asia.groovelab.blesample;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import asia.groovelab.blesample.viewmodel.MainViewModel;
import asia.groovelab.blesample.viewmodel.MainViewModel.Action;
import static asia.groovelab.blesample.viewmodel.MainViewModel.Action.Central;
import static asia.groovelab.blesample.viewmodel.MainViewModel.Action.Peripheral;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
    private final static int	REQUEST_PERMISSIONS			= 1111;
    private final static int	REQUEST_LOCATION_SETTINGS	= 2222;
    private MainViewModel viewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewModel = new ViewModelProvider(this).get(MainViewModel.class);
        findViewById(R.id.central_button).setOnClickListener(view -> {
            viewModel.onClickCentralButton();
        });

        findViewById(R.id.peripheral_button).setOnClickListener(view -> {
            viewModel.onClickPeripheralButton();
        });

        viewModel.action().observe(this, new Observer<Action>() {
            @Override
            public void onChanged(Action action) {
                if(action==Action.None) return;

                Intent intent = null;
                switch(action) {
                    case Central:	intent = new Intent(MainActivity.this, CentralActivity.class);   break;
                    case Peripheral:intent = new Intent(MainActivity.this, PeripheralActivity.class);break;
                }
                startActivity(intent);
            }
        });

        /* 地図権限とBluetooth権限が許可されていない場合はリクエスト. */
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
            else
                requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
        }

        /* Bluetooth ON/OFF判定 */
        final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        /* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
        if (bluetoothAdapter == null)
            ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\nアプリを終了します。").show();
            /* OFFならONにするようにリクエスト */
        else if( !bluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if(result.getResultCode() != Activity.RESULT_OK) {
                            ErrDialog.create(MainActivity.this, "BluetoothがOFFです。ONにして操作してください。\n終了します。").show();
                        }
                    });
            startForResult.launch(enableBtIntent);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        /* 対象外なので、無視 */
        if (requestCode != REQUEST_PERMISSIONS) return;

        /* 権限リクエストの結果を取得する. */
        long ngcnt = Arrays.stream(grantResults).filter(value -> value != PackageManager.PERMISSION_GRANTED).count();
        if (ngcnt > 0) {
            ErrDialog.create(MainActivity.this, "このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").show();
            return;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
        switch (resultCode) {
            case Activity.RESULT_CANCELED:
                ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
                break;
        }
    }
}