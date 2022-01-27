package asia.groovelab.blesample;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

import asia.groovelab.blesample.model.Peripheral;

public class PeripheralListAdapter extends BaseAdapter {
	private final LayoutInflater mInflater;
	private List<Peripheral> peripherals = new ArrayList<>();

	public PeripheralListAdapter(Context context) {
		mInflater = LayoutInflater.from(context);
	}

	@Override
	public int getCount() {
		return peripherals.size();
	}

	@Override
	public Peripheral getItem(int i) {
		return peripherals.get(i);
	}

	@Override
	public long getItemId(int i) {
		return 0;
	}

	@Override
	public View getView(int pos, View view, ViewGroup parent) {
		if (view == null) {
			view = mInflater.inflate(R.layout.view_peripheral, parent, false);
		}

		Peripheral info = getItem(pos);

		((TextView)view.findViewById(R.id.local_name_text_view)).setText(info.getLocalName());
		((TextView)view.findViewById(R.id.rssi_text_view)).setText(String.valueOf(info.getRssi()));
		((TextView)view.findViewById(R.id.address_text_view)).setText(info.getAddress());
		((TextView)view.findViewById(R.id.service_uuid_text_view)).setText(info.getServiceUuid());

		return view;
	}

	public void addPeripherals(List<Peripheral> peripheralList) {
		for(Peripheral peripheral: peripheralList)
			addPeripheral(peripheral);
	}

	public void addPeripheral(Peripheral peripheral) {
		Peripheral lp = peripherals.stream().filter(it->it.getAddress().equals(peripheral.getAddress())).findAny().orElse(null);
		if(lp == null) {
			peripherals.add(peripheral);
			/* 並び替え。 */
			peripherals.sort((o1, o2) -> {
				/* アドレス名で並び替え */
				int compare = o1.getAddress().compareTo(o2.getAddress());
				if(compare == 0) return 0;
				return compare < 0 ? -1 : 1;
			});
		}
		else {
			lp.setLocalName(peripheral.getLocalName());
			lp.setAddress(peripheral.getAddress());
			lp.setRssi(peripheral.getRssi());
			lp.setServiceUuid(peripheral.getServiceUuid());
			lp.setBluetoothDevice(peripheral.getBluetoothDevice());
		}
	}

	public Peripheral getPeripheral(int pos) {
		return peripherals.get(pos);
	}

	public void removeAllPeripherals() {
		peripherals.clear();
	}
}
