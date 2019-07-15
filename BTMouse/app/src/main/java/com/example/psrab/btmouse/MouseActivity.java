package com.example.psrab.btmouse;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.UUID;


public class MouseActivity extends AppCompatActivity {
    private static final String TAG = "MouseActivity";
    private static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter btAdapter = null;
    private BluetoothSocket btSocket = null;
    private OutputStream outStream = null;
    // Well known SPP UUID
    private static final UUID MY_UUID =
    UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    // Insert your server's MAC address
    private static String address = "";  //"9C:B6:D0:8D:01:D8"; //"0A:00:27:00:00:17"; //9CB6D08D01D8
    TextView tv;

    boolean doubleBackToExitPressedOnce = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        this.getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        address = getIntent().getStringExtra("MAC_ADDRESS");

        //To remove screen timeout
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        tv = findViewById(R.id.tp);
        tv.setOnTouchListener(new View.OnTouchListener() {

            float initX = 0, initY = 0, prevX, prevY;

            @Override
            public boolean onTouch(View v, MotionEvent e) {

                //int e = event.getActionMasked();


                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initX = e.getX();
                        initY = e.getY();
                        prevX = initX;
                        prevY = initY;
                        return true;

                    case MotionEvent.ACTION_MOVE:
                        float curX = prevX - e.getX();
                        float curY = prevY - e.getY();
                        prevX = e.getX();
                        prevY = e.getY();
                        //Toast.makeText(getBaseContext(), curX + " and " + curY, Toast.LENGTH_SHORT).show();
                        sendMSG((int)curX+"&"+(int)curY+"\n");
                        return true;
                    case MotionEvent.ACTION_UP:
                        Toast.makeText(getBaseContext(), (initX - prevX) + " and " + (initY - prevY), Toast.LENGTH_SHORT).show();
                        return true;
                }
                return false;
            }
        });

        btAdapter = BluetoothAdapter.getDefaultAdapter();
        CheckBTState();

        Button b = findViewById(R.id.lc);
        b.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMSG("Left-Click\n");
            }
        });
        Button b2 = findViewById(R.id.rc);
        b2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendMSG("Right-Click\n");
            }
        });
    }

    @Override
    public void onStart()
    {
        super.onStart();
        getConn();
    }

    public void getConn() {
        // Set up a pointer to the remote node using it's address.
        BluetoothDevice device = btAdapter.getRemoteDevice(address);
 
    // Two things are needed to make a connection:
    //   A MAC address, which we got above.
    //   A Service ID or UUID.  In this case we are using the
    //     UUID for SPP.
    try {
      btSocket = device.createRfcommSocketToServiceRecord(MY_UUID);
    } catch (IOException e) {
      AlertBox("Fatal Error", "In getConn() and socket create failed: " + e.getMessage() + ".");
    }
 
    // Discovery is resource intensive.  Make sure it isn't going on
    // when you attempt to connect and pass your message.
    btAdapter.cancelDiscovery();
 
    // Establish the connection.  This will block until it connects.
    try {
      btSocket.connect();
    } catch (IOException e) {
      try {
        btSocket.close();
      } catch (IOException e2) {
        AlertBox("Fatal Error", "In getConn() and unable to close socket during connection failure" + e2.getMessage() + ".");
      }
    }
 
    // Create a data stream so we can talk to server.
    try {
      outStream = btSocket.getOutputStream();
    } catch (IOException e) {
      AlertBox("Fatal Error", "In getConn() and output stream creation failed:" + e.getMessage() + ".");
    }
    String message = "Hello from Android\n";
    byte[] msgBuffer = message.getBytes();
    try {
    outStream.write(msgBuffer);
    } catch (IOException e) {
        String msg = "In getConn() and an exception occurred during write: " + e.getMessage();
        if (address.equals("00:00:00:00:00:00"))
        msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
        msg = msg + ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

        AlertBox("Fatal Error", msg);
    }
    DisplayMetrics displayMetrics = new DisplayMetrics();
    getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
    int height = displayMetrics.heightPixels;
    int width = displayMetrics.widthPixels;
    sendMSG(new String(Integer.toString(height))+"\n");
    Log.d(TAG, "height is " + Integer.toString(height));
    sendMSG(new String(Integer.toString(width))+"\n");
    Log.d(TAG, "width is " + Integer.toString(width));
  }
 
  @Override
  public void onDestroy() {
    super.onDestroy();

    if (outStream != null) {
      try {
          outStream.write("$$TERMINATE$$\n".getBytes());
        outStream.flush();
      } catch (IOException e) {
        AlertBox("Fatal Error", "In onPause() and failed to flush output stream: " + e.getMessage() + ".");
      }
    }

    try     {
      btSocket.close();
    } catch (IOException e2) {
      AlertBox("Fatal Error", "In onPause() and failed to close socket." + e2.getMessage() + ".");
    }
  }

   
    private void CheckBTState() {
    // Check for Bluetooth support and then check to make sure it is turned on
 
    // Emulator doesn't support Bluetooth and will return null
    if(btAdapter==null) { 
      AlertBox("Fatal Error", "Bluetooth Not supported. Aborting.");
    }
    else {
      if (!btAdapter.isEnabled()) {
        //Prompt user to turn on Bluetooth
        Intent enableBtIntent = new Intent(btAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
      }
    }
  }

  public void AlertBox( String title, String message ){
    new AlertDialog.Builder(this)
    .setTitle( title )
    .setMessage( message + " Press OK to exit." )
            .setPositiveButton("Retry",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface arg0, int arg1) {
                            getConn();
                            Toast.makeText(MouseActivity.this,"Connection Established",Toast.LENGTH_LONG).show();
                        }
                    })

    .setNegativeButton("Exit", new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface arg0, int arg1) {
          finish();
        }
    }).show();
  }

  public void sendMSG(String message)
  {
      //String message = "Hello from Android.\n";
      if(outStream == null) getConn();

      byte[] msgBuffer = message.getBytes();
      if(message.equals("")) return;
      try {
          outStream.write(msgBuffer);
          outStream.flush();
      } catch (IOException e) {
          String msg = "In sendMSG() and an exception occurred during write: " + e.getMessage();
          if (address.equals("00:00:00:00:00:00"))
              msg = msg + ".\n\nUpdate your server address from 00:00:00:00:00:00 to the correct address on line 37 in the java code";
          msg = msg +  ".\n\nCheck that the SPP UUID: " + MY_UUID.toString() + " exists on server.\n\n";

          AlertBox("Fatal Error", msg);
      }
  }
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        int keyCode = event.getKeyCode();
        switch (keyCode) {
            case KeyEvent.KEYCODE_VOLUME_UP:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    Toast.makeText(getBaseContext(), "Scroll Up", Toast.LENGTH_SHORT).show();
                    Log.d("dvb", "Scroll up clicked");
                    sendMSG("Scroll-Up\n");
                }
                return true;
            case KeyEvent.KEYCODE_VOLUME_DOWN:
                if (action == KeyEvent.ACTION_DOWN) {
                    //TODO
                    Toast.makeText(getBaseContext(), "Scroll Down", Toast.LENGTH_SHORT).show();
                    sendMSG("Scroll-Down\n");
                }
                return true;
            default:
                return super.dispatchKeyEvent(event);
        }
    }


    @Override
    public void onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed();
            return;
        }

        this.doubleBackToExitPressedOnce = true;
        Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }
}
