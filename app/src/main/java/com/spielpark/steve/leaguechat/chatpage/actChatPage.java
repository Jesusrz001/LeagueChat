package com.spielpark.steve.leaguechat.chatpage;

import android.app.ListActivity;
import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.spielpark.steve.leaguechat.R;
import com.spielpark.steve.leaguechat.service.ChatService;
import com.spielpark.steve.leaguechat.usersettings.Settings;

public class actChatPage extends ListActivity {
    private static ChatAdapter mAdapter;
    private static Cursor cursor;
    private static String friendName;
    private ChatReceiver receiver;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            setContentView(R.layout.layout_chatpage);
        }
        friendName = getIntent().getExtras().getString("friendName");
        cursor = ChatService.queryDB(friendName, this);
        mAdapter = new ChatAdapter(this, cursor, 0);
        setListAdapter(mAdapter);
        setUpReceiver();
        getListView().smoothScrollToPosition(mAdapter.getCount());
        findViewById(R.id.edtMessage).setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                getListView().smoothScrollToPosition(cursor.getCount());
            }
        });
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        receiver = null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        this.setTitle(friendName);
        getActionBar().setDisplayHomeAsUpEnabled(true);
        return super.onCreateOptionsMenu(menu);
    }

    private void setUpReceiver() {
        receiver = new ChatReceiver();
        IntentFilter filter = new IntentFilter();
        filter.addAction("friend_status_change");
        filter.addAction("friend_request");
        filter.addAction("message_received");
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    private void receiveMessage() {
        cursor = ChatService.queryDB(friendName, this);
        mAdapter.swapCursor(cursor);
        mAdapter.notifyDataSetChanged();
        getListView().smoothScrollToPosition(cursor.getCount());
    }

    public void sendMessage(View v) {
        MessageDB db = MessageDB.getInstance(this);
        String msg = ((EditText) findViewById(R.id.edtMessage)).getText().toString();
        if (msg.length() == 0) return;
        ((EditText) findViewById(R.id.edtMessage)).setText("");
        Intent intent = new Intent(this, ChatService.class);
        intent.setAction("SEND_MESSAGE");
        intent.putExtra("friendName", friendName);
        intent.putExtra("message", msg);
        startService(intent);
        ContentValues cv = new ContentValues();
        cv.put(MessageDB.TableEntry.COLUMN_FROM, ChatService.getUserName());
        cv.put(MessageDB.TableEntry.COLUMN_TO, friendName);
        cv.put(MessageDB.TableEntry.COLUMN_MESSAGE, msg);
        cv.put(MessageDB.TableEntry.COLUMN_TIME, System.currentTimeMillis());
        cv.put(MessageDB.TableEntry.COLUMN_PROFILE, Settings.getUserPic());
        db.getWritableDatabase().insert(MessageDB.TableEntry.TABLE_NAME, null, cv);
        cursor = ChatService.queryDB(friendName, this);
        mAdapter.swapCursor(cursor);
        mAdapter.notifyDataSetChanged();
        getListView().smoothScrollToPosition(cursor.getCount());
        Log.d("ACP/SM", "Count: " + cursor.getCount());
    }

    private class ChatReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            switch(intent.getAction()) {
                case "message_received" : {
                    receiveMessage();
                    break;
                }
                case "friend_request" : {
                    //TODO: This.
                }
            }
        }
    }
}
