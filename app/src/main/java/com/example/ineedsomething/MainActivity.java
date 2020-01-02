package com.example.ineedsomething;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import java.io.ByteArrayOutputStream;
import java.util.Properties;

public class MainActivity extends AppCompatActivity {

    private static Context context;
    private static String sshUser = "";
    private static String sshPassword = "";
    private static String sshHost = "";
    private static int sshPort = 22;

    String wordsText = "";
    TextView words;
    TextView volumeProgress;
    SeekBar seekBar;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        MainActivity.context = getApplicationContext();
        setContentView(R.layout.activity_main);
        volumeProgress = findViewById((R.id.volumeText));
        words = findViewById(R.id.editText);
        seekBar = findViewById(R.id.seekBar);
        getVolume();
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, final int vol, boolean b) {
                int val = (vol * (seekBar.getWidth() - 2 * seekBar.getThumbOffset())) / seekBar.getMax();
                volumeProgress.setText(vol + "");
                volumeProgress.setX(seekBar.getX() + val + seekBar.getThumbOffset() / 2);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(final SeekBar seekBar) {
                final int seekBarProgress = seekBar.getProgress();
                final AsyncTask<Integer, Void, Void> execute = new AsyncTask<Integer, Void, Void>() {
                    @Override
                    protected Void doInBackground(Integer... params) {
                        try {
                            executeRemoteCommand("setVolume", null, seekBarProgress);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return null;
                    }
                }.execute(1);
            }
        });
    }

    private void getVolume() {
        final AsyncTask<Integer, Void, Void> execute = new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    String currentVolume =  executeRemoteCommand("getVolume", "", 0);
                    System.out.println("rtn vol is " + currentVolume);
                    int volume = Integer.parseInt(currentVolume.trim(), 10);
                    System.out.println("conv vol is " + volume);
                    setVolumeProgress(volume);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }

    private void setVolumeProgress(int volume) {
        seekBar.setProgress(volume);
    }

    public void saySomething(View view) {
        wordsText =  words.getText().toString();
        System.out.println(wordsText);
        new AsyncTask<Integer, Void, Void>(){
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeRemoteCommand("say", wordsText, 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;

            }
        }.execute(1);
    }

    public void restart(View view) {
        new AsyncTask<Integer, Void, Void>(){
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeRemoteCommand("restart", "", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }
    public void shutdown(View view) {
        new AsyncTask<Integer, Void, Void>(){
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeRemoteCommand("shutdown", "", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }
    public void sleep(View view) {
        new AsyncTask<Integer, Void, Void>(){
            @Override
            protected Void doInBackground(Integer... params) {
                try {
                    executeRemoteCommand("sleep",  "", 0);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);
    }

    public static String executeRemoteCommand(String action, String wordsToSay, int volume)
            throws Exception {
        String username = MainActivity.sshUser;
        String password = MainActivity.sshPassword;
        String hostname = MainActivity.sshHost;
        int port = MainActivity.sshPort;

        try {


            JSch jsch = new JSch();
            Session session = jsch.getSession(username, hostname, port);
            session.setPassword(password);

            // Avoid asking for key confirmation
            Properties prop = new Properties();
            prop.put("StrictHostKeyChecking", "no");
            session.setConfig(prop);

            session.connect();

            // SSH Channel
            ChannelExec channelssh = (ChannelExec)
                    session.openChannel("exec");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            channelssh.setOutputStream(baos);
            switch (action) {
                case "say":
                    channelssh.setCommand("say " + wordsToSay);
                    break;
                case "getVolume":
                    channelssh.setCommand("osascript -e 'set ovol to output volume of (get volume settings)'");
                    break;
                case "setVolume":
                    channelssh.setCommand("osascript -e \"set volume output volume " + volume + "\"");
                    break;
                case "restart":
                    channelssh.setCommand("osascript -e 'tell app \"System Events\" to restart'");
                    break;
                case "sleep":
                    channelssh.setCommand("osascript -e 'tell app \"System Events\" to sleep'");
                    break;
                case "shutdown":
                    channelssh.setCommand("osascript -e 'tell app \"System Events\" to shut down'");
                    break;
                default:
                    Toast.makeText(
                            MainActivity.context,
                            "Invalid option specified",
                            Toast.LENGTH_SHORT
                    ).show();
                    break;

            }
            channelssh.connect();
            Thread.sleep(1000);
            String rtnString = new String(baos.toByteArray());
            channelssh.disconnect();
            return rtnString;
        } catch(Exception exception) {
            System.out.println(exception.getMessage());
            Toast.makeText(MainActivity.context, exception.getMessage(), Toast.LENGTH_LONG);
            throw exception;
        }
    }
}
