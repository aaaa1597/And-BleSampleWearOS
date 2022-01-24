package asia.groovelab.blesample;

import android.bluetooth.BluetoothDevice;
import android.os.Handler;
import android.widget.ListAdapter;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import asia.groovelab.blesample.model.Peripheral;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import no.nordicsemi.android.support.v18.scanner.ScanCallback;
import no.nordicsemi.android.support.v18.scanner.ScanResult;
import no.nordicsemi.android.support.v18.scanner.ScanSettings;

public class CentralViewModel  extends ViewModel {
	MutableLiveData<Boolean> failedToScan = new MutableLiveData<>(false);
	private MutableLiveData<Boolean> mUpdlist = new MutableLiveData<>(false);
	public MutableLiveData<Boolean> Updlist() { return mUpdlist; }

	private Boolean isScanning = false;
	private BluetoothLeScannerCompat getScanner() {
		return BluetoothLeScannerCompat.getScanner();
	}

	private ScanSettings getScanSetting() {
		return new ScanSettings.Builder()
								.setLegacy(false)
								.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
								.setReportDelay(400)
								.setUseHardwareBatchingIfSupported(false)
								.build();
	}

	private ScanCallback scanCallback = new ScanCallback() {
		@Override
		public void onScanResult(int callbackType, @NonNull ScanResult result) {
			Tlog.d("aaaaaaaaaaa ScanResult={0}", result);
			addPeripheral(new Peripheral(result));
			mUpdlist.postValue(true);
		}

		@Override
		public void onBatchScanResults(@NonNull List<ScanResult> results) {
			Tlog.d("aaaaaaaaaaa ScanResults={0}", results);
			if(results.size()==0) return;
			List<Peripheral> peripherals = results.stream().map(it -> new Peripheral(it)).collect(Collectors.toList());
			addPeripherals(peripherals);
			mUpdlist.postValue(true);
		}

		@Override
		public void onScanFailed(int errorCode) {
			failedToScan.postValue(true);
			isScanning = false;
		}
	};

	private void addPeripherals(List<Peripheral> peripheralList) {
		mAdapter.addPeripherals(peripheralList);
	}

	/* TODO 追加/更新が動かない。 */
	private void addPeripheral(Peripheral peripheral) {
		mAdapter.addPeripheral(peripheral);
	}

	Peripheral getPeripheral(int pos) {
		return mAdapter.getPeripheral(pos);
	}

	private void removeAllPeripherals() {
		mAdapter.removeAllPeripherals();
	}

	void startToScan() {
		if(isScanning)
			return;

		Tlog.d("aaaaaaaaaaa startScan");
		getScanner().startScan(null, getScanSetting(), scanCallback);
		isScanning = true;
	}

	void stopToScan() {
		if (!isScanning)
			return;

		Tlog.d("aaaaaaaaaaa stopScan");
		getScanner().stopScan(scanCallback);
		isScanning = false;
	}

	void reScan() {
		stopToScan();
		removeAllPeripherals();
		new Handler().postDelayed(new Runnable() {
			@Override
			public void run() {
				startToScan();
			}
		}, 400);
	}

	PeripheralListAdapter mAdapter;
	public void setAdapter(PeripheralListAdapter adapter) {
		mAdapter = adapter;
	}

	public PeripheralListAdapter getAdapter() {
		return mAdapter;
	}
}
