package com.example.alexis.clientstream;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.net.wifi.p2p.WifiP2pConfig;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pDeviceList;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;


public class WifiActivity extends AppCompatActivity {

    private WifiP2pManager mManager;
    private WifiP2pManager.Channel mChannel;
    private WiFiDirectBroadcastReceiver mReceiver;
    private IntentFilter mIntentFilter;
    private ListView peersList;
    protected Object mActionMode;
    public int selectedItem = -1;
    public VideoView vidView;
    MediaPlayer mp;
    boolean isConnected = false;
    private Socket clientSocket;
    private Handler handler = new Handler();
    private SurfaceHolder holder;
    private SurfaceView mPreview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi);


        mManager = (WifiP2pManager) getSystemService(Context.WIFI_P2P_SERVICE);
        mChannel = mManager.initialize(this, getMainLooper(), null);
        mReceiver = new WiFiDirectBroadcastReceiver(mManager, mChannel, this);

        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        mp = new MediaPlayer();
        //holder = mPreview.getHolder();
        //holder.addCallback((SurfaceHolder.Callback) this);


    }

    @Override
    protected void onPause() {
        super.onPause();
        //unregisterReceiver(mReceiver);
    }
    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, mIntentFilter);

        //ubdate action for receive changes
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
        mIntentFilter.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);


        final TextView text = new TextView(this);

        peersList = (ListView)findViewById(R.id.listView);

        final ArrayAdapter<String> deviceAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, mReceiver.getConnectedDevicesName());



        peersList.setAdapter(deviceAdapter);

        peersList.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                if (mActionMode != null) {
                    return false;
                }

                selectedItem = position;
                Toast.makeText(WifiActivity.this, "Connecting to " + deviceAdapter.getItem(position), Toast.LENGTH_SHORT).show();
                final WifiP2pConfig config = new WifiP2pConfig();
                config.deviceAddress = mReceiver.getConnectedDevices().get((int) id).deviceAddress;
                mManager.connect(mChannel, config, new WifiP2pManager.ActionListener() {

                    @Override
                    public void onSuccess() {
                        isConnected = true;
                        Thread t = new Thread() {
                            public void run() {
                                try {
                                    clientSocket = new Socket(config.deviceAddress, 8888);

                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                ParcelFileDescriptor pfd = ParcelFileDescriptor.fromSocket(clientSocket);
                                                pfd.getFileDescriptor().sync();
                                                //mp.setDataSource(pfd.getFileDescriptor());
                                                //pfd.close();
                                                //mp.setDisplay(holder);
                                                //mp.prepareAsync();
                                                //mp.start();
                                            } catch (IOException e) {
                                                // TODO Auto-generated catch block
                                                e.printStackTrace();
                                            }

                                        }
                                    });

                                } catch (UnknownHostException e) {
                                    e.printStackTrace();
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            public void onFailure(int reason) {
                                Toast.makeText(WifiActivity.this, "failed connection to" + config.deviceAddress, Toast.LENGTH_SHORT).show();
                                isConnected = false;
                            }
                        };
                        // Start the CAB using the ActionMode.Callback defined above
                        //mActionMode = WifiActivity.this.startActionMode((ActionMode.Callback) WifiActivity.this);
                    }
                    @Override
                    public void onFailure(int reason) {

                    }
                });
                return true;
            }

        });



        mManager.discoverPeers(mChannel, new WifiP2pManager.ActionListener() {
            @Override
            public void onSuccess() {
                // case connected to direct wifi
            }

            @Override
            public void onFailure(int reasonCode) {
                // case not
            }
        });
                }








    public class WiFiDirectBroadcastReceiver extends BroadcastReceiver {

        private WifiP2pManager mManager;
        private WifiP2pManager.Channel mChannel;
        private WifiActivity mActivity;
        private ArrayList<WifiP2pDevice> mDeviceList = new ArrayList<WifiP2pDevice>();
        private ArrayList<String> nameDeviceList = new ArrayList<String>();

        public ArrayAdapter mAdapter;
        int flag;

        public WiFiDirectBroadcastReceiver(WifiP2pManager manager, WifiP2pManager.Channel channel,
                                           WifiActivity activity) {
            super();
            this.mManager = manager;
            this.mChannel = channel;
            this.mActivity = activity;

        }

        public ArrayList<WifiP2pDevice> getConnectedDevices(){
            return mDeviceList;
        }

        public ArrayList<String> getConnectedDevicesName(){
            return nameDeviceList;
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            final TextView text = new TextView(mActivity);

            String action = intent.getAction();


            // Case wifi is changing
            if (WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION.equals(action)) {
                int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -1);
                if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
                    String title = "";
                    title += "   MAC["  + "]";
                    Toast.makeText(mActivity, "Wi-Fi Direct is enabled." + title, Toast.LENGTH_SHORT).show();
                    System.out.println("ok");

                } else {
                    Toast.makeText(mActivity, "Wi-Fi Direct is disabled.", Toast.LENGTH_SHORT).show();
                }

            }

            // Get a list of current peers
            else if (WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION.equals(action)) {
                System.out.println("ok");
                nameDeviceList.removeAll(nameDeviceList);
                mDeviceList.removeAll(mDeviceList);
                if (mManager != null) {
                    mManager.requestPeers(mChannel, new WifiP2pManager.PeerListListener() {

                        @Override
                        public void onPeersAvailable(WifiP2pDeviceList peers) {

                            if (peers != null) {
                                mDeviceList.addAll(peers.getDeviceList());
                                ArrayList<String> deviceNames = new ArrayList<String>();
                                for (WifiP2pDevice device : mDeviceList) {
                                    nameDeviceList.add(device.deviceName);
                                }
                                if (deviceNames.size() > 0) {
                                    mAdapter = new ArrayAdapter<String>(mActivity, android.R.layout.simple_list_item_1, deviceNames);

                                    if (flag == 0) {
                                        flag = 1;

                                        // In case see a new device, we connects to the new device
                                        WifiP2pConfig config = new WifiP2pConfig();
                                        config.deviceAddress = mDeviceList.get(mDeviceList.size()-1).deviceAddress;


                                    }
                                } else {
                                }
                            }
                        }
                    });
                }

                // Call WifiP2pManager.requestPeers() to get a list of current peers
            } else if (WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION.equals(action)) {


                // Respond to new connection or disconnections
            } else if (WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION.equals(action)) {
                // Respond to this device's wifi state changing
            }


        }

    }



     class Client extends AsyncTask{


        private Context context;
        private TextView statusText;
        Socket client ;
        private InputStream inputStream;
        private WifiP2pDevice host;
        private int port;
        Socket socket = new Socket();
        WifiActivity wifiActivity;


        public Client(Context context, View statusText, WifiActivity wifiActivity) throws IOException {
            this.context = context;
            this.statusText = (TextView) statusText;
            this.wifiActivity = wifiActivity;
        }

        public void connectSocket(){
            try {
                client = new Socket(host.deviceAddress,8888);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }

        @Override
        protected InputStream doInBackground(Object[] params) {
            try {
                inputStream = client.getInputStream();
                String total = convertStreamToString(inputStream);
                Uri uri = Uri.parse(total);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }


            return null;

        }

        public void closeSocket(){
            try {
                socket.close();
                inputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

      class Server {
        private InputStream inputStream;
        private WifiP2pDevice host;
        private int port;
        Socket socket = new Socket();

        private OutputStream outputStream;


        public void connectSocket(){
            try {
                socket = new Socket(host.deviceAddress, 8888);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }



        public void closeSocket(){
            try {
                socket.close();
                outputStream.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }


    }
     String convertStreamToString(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();
        String line = null;

        while ((line = reader.readLine()) != null) {
            sb.append(line);
        }

        is.close();

        return sb.toString();
    }


}







