package nfc.omid.com.nfc;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.os.Parcelable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.nio.charset.Charset;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    NfcAdapter mNfcAdapter;

    ToggleButton toggleBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toggleBtn = (ToggleButton) findViewById(R.id.toggleBtn);
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        if (mNfcAdapter == null) {
            Toast.makeText(this, "nfc not enabled", Toast.LENGTH_SHORT).show();
            return;
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        if (intent.hasExtra(NfcAdapter.EXTRA_TAG)) {
            Toast.makeText(this, "NFCIntent", Toast.LENGTH_SHORT).show();
            if (toggleBtn.isChecked()) {
                Toast.makeText(this, "Write", Toast.LENGTH_SHORT).show();
                toggleBtn.setText("Write");
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                NdefRecord ndefRecord = createTextRecordMessage("salam 123");
                NdefMessage ndefMessage = createNdefMessage(ndefRecord);
                writeNdefMessage(tag, ndefMessage);
            } else {
                Toast.makeText(this, "Read", Toast.LENGTH_SHORT).show();
                toggleBtn.setText("Read");
                Parcelable[] parcelables = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
                if (parcelables != null && parcelables.length > 0) {
                    NdefMessage[] msgs = new NdefMessage[parcelables.length];
                    for (int i = 0; i < parcelables.length; i++) {
                        msgs[i] = (NdefMessage) parcelables[i];
                    }
                    readFromText(msgs);
                }
            }

            //


        }
    }

    private void readFromText(NdefMessage[] messages) {
        for (int i = 0; i < messages.length; i++) {
            NdefMessage message = messages[i];
            for (int j = 0; j < messages[0].getRecords().length; j++) {
                NdefRecord record = messages[i].getRecords()[j];
                String s = readFromNdefRecord(record);
                Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
            }
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        enableForegroundDispatch();
    }

    @Override
    protected void onPause() {
        super.onPause();
        disableForegroundDispatch();
    }

    private void enableForegroundDispatch() {
        Intent intent = new Intent(this, MainActivity.class).addFlags(Intent.FLAG_RECEIVER_REPLACE_PENDING);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        IntentFilter[] intentFilters = new IntentFilter[]{};
        mNfcAdapter.enableForegroundDispatch(this, pendingIntent, intentFilters, null);

    }

    private void disableForegroundDispatch() {
        mNfcAdapter.disableForegroundDispatch(this);
    }

    private void formatTag(Tag tag, NdefMessage message) {
        try {
            NdefFormatable ndefFormatable = NdefFormatable.get(tag);
            if (ndefFormatable != null) {
                ndefFormatable.connect();
                ndefFormatable.format(message);
                ndefFormatable.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "formatTag: " + e.getMessage());
        }
    }

    private void writeNdefMessage(Tag tag, NdefMessage message) {
        try {
            if (tag != null) {
                Ndef ndef = Ndef.get(tag);
                if (ndef == null) {
                    formatTag(tag, message);
                } else {
                    ndef.connect();
                    if (ndef.isWritable()) {
                        ndef.writeNdefMessage(message);
                        Toast.makeText(this, "tag written", Toast.LENGTH_SHORT).show();
                    }
                    ndef.close();
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "writeNdefMessage: " + e.getMessage());
        }
    }

    private NdefRecord createTextRecordMessage(String inputText) {
        Locale locale = new Locale("en", "US");
        byte[] langBytes = locale.getLanguage().getBytes(
                Charset.forName("US-ASCII"));
        boolean encodeInUtf8 = false;
        Charset utfEncoding = encodeInUtf8 ?
                Charset.forName("UTF-8") : Charset.forName("UTF-16");
        int utfBit = encodeInUtf8 ? 0 : (1 << 7);
        byte status = (byte) (utfBit + langBytes.length);
        byte[] textBytes = inputText.getBytes(utfEncoding);
        byte[] data = new byte[1 + langBytes.length + textBytes.length];
        data[0] = (byte) status;
        System.arraycopy(langBytes, 0, data, 1, langBytes.length);
        System.arraycopy(textBytes, 0, data, 1 + langBytes.length, textBytes.length);
        NdefRecord textRecord = new NdefRecord(NdefRecord.TNF_WELL_KNOWN,
                NdefRecord.RTD_TEXT, new byte[0], data);
        return textRecord;
    }

    private NdefMessage createNdefMessage(NdefRecord record) {
        NdefMessage message = new NdefMessage(new NdefRecord[]{record});
        return message;
    }

    //////////////////

    private String readFromNdefRecord(NdefRecord record) {

        byte statusByte = record.getPayload()[0];
        int languageCodeLength = statusByte & 0x3F;
        int isUTF8 = statusByte - languageCodeLength;
        String payload = "";
        if (isUTF8 == 0x00) {
            payload = new String(
                    record.getPayload(), 1 + languageCodeLength,
                    record.getPayload().length - 1 - languageCodeLength,
                    Charset.forName("UTF-8"));
        } else if (isUTF8 == -0x80) {
            payload = new String
                    (record.getPayload(),
                            1 + languageCodeLength,
                            record.getPayload().length - 1 - languageCodeLength,
                            Charset.forName("UTF-16")
                    );
        }
        return payload;
    }

}
