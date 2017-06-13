package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;

import android.content.ContentValues;
import android.content.Context;

import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataInputStream;
import java.io.DataOutputStream;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import java.net.UnknownHostException;

import static android.content.ContentValues.TAG;


/**
 * GroupMessengerActivity is the main Activity for the assignment.
 *
 * @author stevko
 */
public class GroupMessengerActivity extends Activity {


    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String KEY_FIELD = "key";
    static final String VALUE_FIELD = "value";
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    static final int SERVER_PORT = 10000;
    JSONObject js = new JSONObject();
    int sequence_counter = -1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);


        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }


        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        final EditText editText = (EditText) findViewById(R.id.editText1);
        final Button button = (Button) findViewById(R.id.button4);
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));



        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = editText.getText().toString() + "\n";
                editText.setText("");
                TextView editText = (TextView) findViewById(R.id.textView1);
                editText.append("\t" + msg);
                try {
                    js.put("Message", msg);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, js.toString(), getMyPort());
            }
        });


    }

    String getMyPort() {
        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        final String myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        return myPort;

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override

        protected Void doInBackground(ServerSocket... sockets) {
            while (true) {
                ServerSocket serverSocket = sockets[0];


                try {

                    Socket socket = serverSocket.accept();
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    String msg_content = input.readUTF();

                    publishProgress(msg_content);

                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF("Acknowledgement");
                    System.out.flush();
                    input.close();
                    output.close();
                    socket.close();


                } catch (IOException e) {
                    e.printStackTrace();

                }
            }
        }


        protected void onProgressUpdate(String... strings) {


            String strReceived = strings[0].trim();
            TextView test = (TextView) findViewById(R.id.textView1);
            test.append(strReceived + "\t\n");


            ContentValues keyValueToInsert = new ContentValues();
            keyValueToInsert.put(KEY_FIELD, ++sequence_counter);
            keyValueToInsert.put(VALUE_FIELD, strReceived);

            getContentResolver().insert(GroupMessengerProvider.URI, keyValueToInsert);
            System.out.print(GroupMessengerProvider.URI);


        }
    }

    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            try {

                String msgToSend = msgs[0];
                JSONObject j = new JSONObject(msgToSend);
                String msg = j.getString("Message");
                String remotePort[] = {REMOTE_PORT0, REMOTE_PORT1, REMOTE_PORT2, REMOTE_PORT3, REMOTE_PORT4};

                for (int i = 0; i < 5; i++) {

                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort[i]));

                    DataOutputStream output = new DataOutputStream(socket.getOutputStream());
                    output.writeUTF(msg);
                    DataInputStream input = new DataInputStream(socket.getInputStream());
                    String string = input.readUTF();
                    if (string.equals("Acknowledgement")) {

                        socket.close();
                        input.close();
                        output.close();
                        System.out.flush();
                    }
                }


            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "ClientTask socket IOException");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return null;
        }
    }
}








