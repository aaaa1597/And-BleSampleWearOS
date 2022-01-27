package asia.groovelab.blesample.viewmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.os.Handler;
import android.view.View;
import android.widget.ExpandableListView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import asia.groovelab.blesample.R;
import asia.groovelab.blesample.adapter.ItemListAdapter;
import no.nordicsemi.android.ble.data.Data;
import no.nordicsemi.android.ble.observer.BondingObserver;
import no.nordicsemi.android.ble.observer.ConnectionObserver;

import asia.groovelab.blesample.TLog;
import asia.groovelab.blesample.ble.SampleBleManager;
import asia.groovelab.blesample.Functions.Func0;
import asia.groovelab.blesample.Functions.Func1;
import asia.groovelab.blesample.Functions.Func2;
import asia.groovelab.blesample.model.Item;
import asia.groovelab.blesample.model.Peripheral;
import asia.groovelab.blesample.model.Section;

public class CentralPeripheralViewModel extends ViewModel {
	private Peripheral			peripheral = null;
	private SampleBleManager	bleManager;

	private MutableLiveData<String>		appTitle = new MutableLiveData<>();
	public MutableLiveData<String>		getAppTitle() { return appTitle; }
	private MutableLiveData<String>		address = new MutableLiveData<>();
	public MutableLiveData<String>		getAddress() { return address; }
	private MutableLiveData<String>		rssi = new MutableLiveData<>();
	public MutableLiveData<String>		getRssi() { return rssi; }
	private MutableLiveData<Integer>	progressBarVisibility = new MutableLiveData<>();
	public MutableLiveData<Integer>		getProgressBarVisibility() { return progressBarVisibility; }
	private MutableLiveData<Boolean>	reconnected = new MutableLiveData<>();
	public MutableLiveData<Boolean>		getReconnected() { return reconnected; }
	private MutableLiveData<Boolean>	disconnectedFromDevice = new MutableLiveData<>();
	public MutableLiveData<Boolean>		getDisconnectedFromDevice() { return disconnectedFromDevice; }
	private MutableLiveData<Boolean>	notifyDataSetChanged = new MutableLiveData<>();
	private Func2<UUID, byte[], Object>	wroteCharacteristicHandler = null;
	public void							setWroteCharacteristicHandler(Func2<UUID, byte[], Object> func) { wroteCharacteristicHandler = func; }
	private Func2<UUID, byte[], Integer>notifiedCharacteristicHandler = null;
	public void							setNotifiedCharacteristicHandler(Func2<UUID, byte[], Integer> func) { notifiedCharacteristicHandler = func; }

	public void setContext(AppCompatActivity context) {
		notifyDataSetChanged.observe(context, ignore ->
				context.runOnUiThread(() -> {
					ExpandableListView evw = context.findViewById(R.id.exlist_view);
					for(int lpct = 0; lpct < evw.getExpandableListAdapter().getGroupCount(); lpct++)
						evw.expandGroup(lpct);
					mAdapter.notifyDataSetChanged();
		}));
		bleManager = new SampleBleManager(context);
		bleManager.setConnectionObserver(new ConnectionObserver() {
			@Override
			public void onDeviceReady(@NonNull BluetoothDevice device) {
				bleManager.readRssi(dbm -> {
					rssi.postValue(dbm + "dbm");
					return null;
				});
			}

			@Override
			public void onDeviceConnected(@NonNull BluetoothDevice device) {
				TLog.d("");
				if (bleManager.isConnecting()) {
					TLog.d("");
					if(peripheral == null) return;
					BluetoothDevice dev = peripheral.getBluetoothDevice();
					reconnected.postValue(true);
					bleManager.enqueueConnect(dev);
					return;
				}

				if (bleManager.wasConnected()) {
					TLog.d("");
					disconnect(() -> {
						TLog.e("");
						disconnectedFromDevice.postValue(true);
						return null;
					});
				}
			}

			@Override public void onDeviceConnecting(@NonNull BluetoothDevice device) { TLog.d(""); }
			@Override public void onDeviceFailedToConnect(@NonNull BluetoothDevice device, int reason) { TLog.d(""); }
			@Override public void onDeviceDisconnecting(@NonNull BluetoothDevice device) { TLog.d(""); }
			@Override public void onDeviceDisconnected(@NonNull BluetoothDevice device, int reason) { TLog.d(""); }
		});
		bleManager.setBondingObserver(new BondingObserver() {
			@Override public void onBondingRequired(@NonNull BluetoothDevice device) { }
			@Override public void onBonded(@NonNull BluetoothDevice device) { }
			@Override public void onBondingFailed(@NonNull BluetoothDevice device) {}
		});
		bleManager.setDiscoveredServicesHandler(new Func1<List<BluetoothGattService>, Object>() {
			@Override
			public Object invoke(List<BluetoothGattService> services) {
				List<Section> sectionList = services.stream().map(it -> new Section(it)).collect(Collectors.toList());
				mAdapter.setSections(sectionList);

				/* TODO ここから */
				TLog.d("TopLevel Service.size()={0}", services.size());
				int lpct = 0;
				for(Section section : sectionList) {
					TLog.d("{0}:{1}", lpct, section.getTitle());
					lpct++;
				}
				/* ここまで */

				List<List<Item>> itemList = services.stream().map(
					service -> service.getCharacteristics().stream().map(
							characteristic -> {
									bleManager.enableNotificationCallBack(characteristic, new Func1<Data, Object>() {
										@Override
										public Data invoke(Data data) {
											if(notifiedCharacteristicHandler != null)
												notifiedCharacteristicHandler.invoke(characteristic.getUuid(), data.getValue());
											return null;
										}
									});
									return new Item(characteristic);
							}
					).collect(Collectors.toList())
				).collect(Collectors.toList());
				mAdapter.setItems(itemList);

				/* TODO ここから */
				TLog.d("itemList.size()={0}", itemList.size());
				for(List<Item> items : itemList) {
					TLog.d("items.size()={0}", items.size());
					for(Item item : items)
						TLog.d("{0}:{1} {2} {3} {4}", item.getUuid(), item.getIsReadable(), item.getIsWritable(), item.getBluetoothGattCharacteristic(), item.getReadValue());
				}
				/* ここまで */

				notifyDataSetChanged.postValue(true);
				progressBarVisibility.postValue(View.GONE);
				return null;
			}
		});
	}

	public void connect(Peripheral peripheral) {
		TLog.d("");
		appTitle.postValue(peripheral.getLocalName());
		address.postValue(peripheral.getAddress());
		progressBarVisibility.postValue(View.VISIBLE);

		this.peripheral = peripheral;

		if(peripheral.getBluetoothDevice() != null)
			bleManager.enqueueConnect(peripheral.getBluetoothDevice());
	}

	public void disconnect(Func0<Object> doneHandler) {
		progressBarVisibility.postValue(View.VISIBLE);

		bleManager.enqueueDisconnect(() -> { new Handler().postDelayed(new Runnable() {
												@Override
												public void run() {
													doneHandler.invoke();
												}
											}, 200);
											return null;
		});
	}

	public void readCharacteristic(Item item) {
		if (!item.getIsReadable())
			return;

		bleManager.readCharacteristic(item.getBluetoothGattCharacteristic(), new Func1<Data,Object>() {
			@Override
			public Object invoke(Data data) {
				if(data != null && data.getValue() != null) {
					byte[] readValue = data.getValue();
					Item litem = getItem(item.getBluetoothGattCharacteristic());
					if(litem != null) {
						litem.setReadValue(Arrays.toString(readValue));
						mAdapter.notifyDataSetChanged();
					}
				}
				return null;
			}
		});
	}

	public void writeCharacteristic(Item item, String writeText) {
		if (!item.getIsWritable())
			return;

		String tmpstr = (writeText.length()%2==0) ? writeText : "0" + writeText;
		List<String> tmplist = new ArrayList<>();
		for(int idx = 0; idx < tmpstr.length(); idx+=2)
			tmplist.add(tmpstr.substring(idx,idx+2));

		List<Byte> tmpBytes = tmplist.stream().map(it -> (Byte)(byte)Integer.parseInt(it, 16)).collect(Collectors.toList());
		byte[] bytes = new byte[tmpBytes.size()];
		for (int idx = 0; idx < tmpBytes.size(); idx++)
			bytes[idx] = tmpBytes.get(idx);

		bleManager.writeCharacteristic(item.getBluetoothGattCharacteristic(), new Data(bytes), new Func1<Data, Object>() {
			@Override
			public Object invoke(Data data) {
				if(wroteCharacteristicHandler!=null)
					wroteCharacteristicHandler.invoke(item.getBluetoothGattCharacteristic().getUuid(), data.getValue());
				return null;
			}
		});
	}

	public Item getItem(Integer sectionPosition, Integer itemPosition) {
		if(mAdapter.getItems().get(sectionPosition) == null)
			return null;
		return mAdapter.getItems().get(sectionPosition).get(itemPosition);
	}

	private Item getItem(BluetoothGattCharacteristic characteristic) {
		for(List<Item> item : mAdapter.getItems()) {
			Item findit = item.stream().filter(it -> it.getUuid().equals(characteristic.getUuid().toString())).findAny().orElse(null);
			if(findit != null)
				return findit;
		}
		return null;
	}

	private ItemListAdapter mAdapter;
	public void setAdapter(ItemListAdapter adapter) {
		this.mAdapter = adapter;
	}
	public ItemListAdapter getAdapter() {
		return mAdapter;
	}
}
