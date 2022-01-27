package asia.groovelab.blesample;

import java.util.Arrays;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import asia.groovelab.blesample.model.Peripheral;
import static asia.groovelab.blesample.CentralPeripheralActivity.PERIPHERAL_EXTRA;

public class CentralActivity extends AppCompatActivity {
	private final static int	REQUEST_PERMISSIONS			= 1111;
	private final static int	REQUEST_LOCATION_SETTINGS	= 2222;
	private CentralViewModel viewModel;
	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_central);

		viewModel = new ViewModelProvider(this).get(CentralViewModel.class);

		/* BLEデバイスリストの初期化 */
		ListView lvw = findViewById(R.id.list_view);
		viewModel.setAdapter(new PeripheralListAdapter(this));
		lvw.setAdapter(viewModel.getAdapter());
		lvw.setOnItemClickListener((adapterView, view, pos, l) -> {
			Peripheral peripheral = viewModel.getPeripheral(pos);
			Intent intent = new Intent(CentralActivity.this, CentralPeripheralActivity.class);
			intent.putExtra(PERIPHERAL_EXTRA, peripheral);
			startActivity(intent);
		});


		viewModel.Updlist().observe(this, isUpd -> {
			if( !isUpd) return;
			viewModel.getAdapter().notifyDataSetChanged();
		});

		viewModel.failedToScan.observe(this, isScanFailed -> {
			if(isScanFailed)
				Toast.makeText(this, getString(R.string.error_scan), Toast.LENGTH_LONG).show();
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
			ErrDialog.create(CentralActivity.this, "Bluetooth未サポートの端末です。\nアプリを終了します。").show();
			/* OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
					result -> {
						if(result.getResultCode() != Activity.RESULT_OK) {
							ErrDialog.create(CentralActivity.this, "BluetoothがOFFです。ONにして操作してください。\n終了します。").show();
						}
					});
			startForResult.launch(enableBtIntent);
		}

		viewModel.startToScan();
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		/* 対象外なので、無視 */
		if (requestCode != REQUEST_PERMISSIONS) return;

		/* 権限リクエストの結果を取得する. */
		long ngcnt = Arrays.stream(grantResults).filter(value -> value != PackageManager.PERMISSION_GRANTED).count();
		if (ngcnt > 0) {
			ErrDialog.create(CentralActivity.this, "このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").show();
			return;
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
		switch (resultCode) {
			case Activity.RESULT_CANCELED:
				ErrDialog.create(CentralActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
				break;
		}
	}

	@Override
	protected void onRestart() {
		super.onRestart();
		viewModel.reScan();
	}

	@Override
	protected void onStop() {
		super.onStop();
		viewModel.stopToScan();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_central, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(@NonNull MenuItem item) {
		switch(item.getItemId()) {
			case R.id.scan_button:
				viewModel.reScan();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
