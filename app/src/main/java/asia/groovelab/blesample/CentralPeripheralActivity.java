package asia.groovelab.blesample;

import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spanned;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ExpandableListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Locale;

import asia.groovelab.blesample.model.Item;
import asia.groovelab.blesample.model.Peripheral;
import asia.groovelab.blesample.viewmodel.CentralPeripheralViewModel;
import asia.groovelab.blesample.adapter.ItemListAdapter;

public class CentralPeripheralActivity extends AppCompatActivity {
	public static final String PERIPHERAL_EXTRA = "peripheral";
	private CentralPeripheralViewModel viewModel;

	@Override
	protected void onCreate(@Nullable Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_central_peripheral);

		viewModel = new ViewModelProvider(this).get(CentralPeripheralViewModel.class);
		viewModel.setContext(this);
		ExpandableListView elv = findViewById(R.id.exlist_view);
		viewModel.setAdapter(new ItemListAdapter(this));
		elv.setAdapter(viewModel.getAdapter());
		elv.setOnGroupClickListener((expandableListView, view, grpidx, id) -> true);
		elv.setOnChildClickListener((elView, view, grpidx, childidx, id) -> {
			Item item = viewModel.getItem(grpidx, childidx);
			if(item.getIsWritable() && item.getIsReadable()) {
				showAlertDialogForReadWrite(item);
			}
			else if(item.getIsWritable()) {
				showAlertDialogForWrite(item);
			}
			else if(item.getIsReadable()) {
				viewModel.readCharacteristic(item);
			}
			return true;
		});

		viewModel.getReconnected().observe(this, aBoolean -> {
			Toast.makeText(this, R.string.connect_again, Toast.LENGTH_LONG).show();
		});
		viewModel.getDisconnectedFromDevice().observe(this, aBoolean -> {
			Toast.makeText(this, R.string.disconnected, Toast.LENGTH_LONG).show();
			finish();
		});
		viewModel.setWroteCharacteristicHandler((uuid, bytes) -> {
			Toast.makeText(this, "success to write", Toast.LENGTH_LONG).show();
			return null;
		});

		viewModel.setNotifiedCharacteristicHandler((uuid, bytes) -> {
			if(bytes != null) {
				String showstr = MessageFormat.format("notified\n{0}:{1}", uuid, Arrays.toString(bytes));
				Toast.makeText(this, showstr, Toast.LENGTH_LONG).show();
			}
			return null;
		});
		viewModel.getAppTitle().observe(this, apptitle -> {
			((Toolbar)findViewById(R.id.toolbar)).setTitle(apptitle);
		});
		viewModel.getAddress().observe(this, address -> {
			((TextView)findViewById(R.id.address_text_view)).setText(address);
		});
		viewModel.getRssi().observe(this, rssi -> {
			((TextView)findViewById(R.id.rssi_text_view)).setText(rssi);
		});
		viewModel.getProgressBarVisibility().observe(this, visibility -> {
			findViewById(R.id.progress_bar).setVisibility(visibility);
		});

		(findViewById(R.id.disconnect_button)).setOnClickListener(view -> {
			disconnectedAndDismiss();
		});

		/*  初期データ実行 */
		if (savedInstanceState == null) {
			Peripheral peripheral = getIntent().getParcelableExtra(PERIPHERAL_EXTRA);
			viewModel.connect(peripheral);
		}
	}

	@Override
	public void onBackPressed() {
		disconnectedAndDismiss();
	}

	private void disconnectedAndDismiss() {
		viewModel.disconnect(() -> {
			finish();
			return null;
		});
	}

	private void showAlertDialogForReadWrite(Item item) {
		new AlertDialog.Builder(this)
				.setItems(new CharSequence[]{"Read", "Write", "Cancel"}, (dialogInterface, idx) -> {
					switch(idx) {
						case 0: viewModel.readCharacteristic(item);	break;
						case 1: showAlertDialogForWrite(item);		break;
					}
				}).show();
	}

	private void showAlertDialogForWrite(Item item) {
		EditText editText = new EditText(this);
		editText.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
		editText.setFilters(new InputFilter[]{new InputFilter() {
			@Override
			public CharSequence filter(CharSequence src, int srcspos, int srcepos, Spanned dst, int dstspo, int dstepos) {
				String tmp = src.toString().toUpperCase(Locale.ROOT);
				if(tmp.matches("^[0-9A-F]+$"))
					return src;
				else
					return "";
			}
		}});

		AlertDialog diag = new AlertDialog.Builder(this)
				.setMessage(MessageFormat.format("Write to {0}\n 0-9a-fを入力してください。", item.getUuid()))
				.setView(editText)
				.setPositiveButton("Write", (dialogInterface, i) -> {
					viewModel.writeCharacteristic(item, editText.getText().toString());
				})
	            .setNeutralButton("Cancel", (dialogInterface, i) -> {/* 何もしない */})
				.create();
		diag.setOnShowListener(dialogInterface -> {
			editText.requestFocus();
			editText.setOnFocusChangeListener((view, b) -> getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE));
		});
		diag.show();
	}
}
