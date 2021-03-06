/*
 * Copyright 2015 Dejan Djurovski
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.isima;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.Vibrator;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;

public class MainActivity extends Activity {
    private final String PATH_ALARM = "/alarm";
    private final String PATH_STOP = "/stop";

    private EditText receivedMessagesEditText;
    private View buttonAlarm;
    private View buttonStop;
    private GoogleApiClient apiClient;
    private NodeApi.NodeListener nodeListener;
    private String remoteNodeId;
    private MessageApi.MessageListener messageListener;
    private Handler handler;
    private Vibrator vibrator;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);
        startService(new Intent(MainActivity.this, MyServicePhone.class));
        handler = new Handler();
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wl = pm.newWakeLock(PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.FULL_WAKE_LOCK, "My Tag");
        wl.acquire();

        buttonAlarm = findViewById(R.id.button_alarm);
        buttonStop = findViewById(R.id.button_stop);
        mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.alarm);

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, PATH_ALARM, null);
            }
        }, 500);
        vibrator = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);

        playAlarm();
        startVibrate();
        buttonAlarm.setEnabled(true);
        buttonStop.setEnabled(true);

        // Set message1Button onClickListener to send message 1
        buttonAlarm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, PATH_ALARM, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (sendMessageResult.getStatus().isSuccess()) {
                            Toast.makeText(getApplication(), getString(R.string.message1_sent), Toast.LENGTH_SHORT).show();
                            playAlarm();
                        } else
                            Toast.makeText(getApplication(), getString(R.string.error_message1), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });


        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                stopAlarm();
                stopVibrate();


                Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, PATH_STOP, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        if (sendMessageResult.getStatus().isSuccess()) {
                            Toast.makeText(getApplication(), getString(R.string.message1_sent), Toast.LENGTH_SHORT).show();

                        } else
                            Toast.makeText(getApplication(), getString(R.string.error_message1), Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });


        // Create NodeListener that enables buttons when a node is connected and disables buttons when a node is disconnected
        nodeListener = new NodeApi.NodeListener() {
            @Override
            public void onPeerConnected(Node node) {
                remoteNodeId = node.getId();
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        buttonAlarm.setEnabled(true);
                        buttonStop.setEnabled(true);
                        Toast.makeText(getApplication(), getString(R.string.peer_connected), Toast.LENGTH_SHORT).show();
                    }
                });
            }

            @Override
            public void onPeerDisconnected(Node node) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        buttonAlarm.setEnabled(false);
                        buttonStop.setEnabled(false);
                        Toast.makeText(getApplication(), getString(R.string.peer_disconnected), Toast.LENGTH_SHORT).show();
                    }
                });
            }
        };

        // Create MessageListener that receives messages sent from a wearable
        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getPath().equals(PATH_ALARM)) {
                    playAlarm();
                    startVibrate();

                } else if (messageEvent.getPath().equals(PATH_STOP)) {
                    stopAlarm();
                    stopVibrate();

                }
            }
        };

        // Create GoogleApiClient
        apiClient = new GoogleApiClient.Builder(getApplicationContext()).addConnectionCallbacks(new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(Bundle bundle) {
                // Register Node and Message listeners
                Wearable.NodeApi.addListener(apiClient, nodeListener);
                Wearable.MessageApi.addListener(apiClient, messageListener);
                // If there is a connected node, get it's id that is used when sending messages
                Wearable.NodeApi.getConnectedNodes(apiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
                    @Override
                    public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                        if (getConnectedNodesResult.getStatus().isSuccess() && getConnectedNodesResult.getNodes().size() > 0) {
                            remoteNodeId = getConnectedNodesResult.getNodes().get(0).getId();
                            buttonAlarm.setEnabled(true);
                            buttonStop.setEnabled(true);
                        }
                    }
                });
            }

            @Override
            public void onConnectionSuspended(int i) {
                buttonAlarm.setEnabled(false);
                buttonStop.setEnabled(false);
            }
        }).addOnConnectionFailedListener(new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(ConnectionResult connectionResult) {
                if (connectionResult.getErrorCode() == ConnectionResult.API_UNAVAILABLE)
                    Toast.makeText(getApplicationContext(), getString(R.string.wearable_api_unavailable), Toast.LENGTH_LONG).show();
            }
        }).addApi(Wearable.API).build();
    }


    void playAlarm() {

        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
        audioManager.adjustVolume(AudioManager.ADJUST_RAISE, AudioManager.FLAG_PLAY_SOUND);

        handler.post(new Runnable() {
            @Override
            public void run() {
                mediaPlayer.setLooping(true);
                mediaPlayer.start();
            }
        });



    }


    void stopAlarm() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();

                }

            }
        });
    }


    @Override
    protected void onResume() {
        super.onResume();

        // Check is Google Play Services available
        int connectionResult = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getApplicationContext());

        if (connectionResult != ConnectionResult.SUCCESS) {
            // Google Play Services is NOT available. Show appropriate error dialog
            GooglePlayServicesUtil.showErrorDialogFragment(connectionResult, this, 0, new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    finish();
                }
            });
        } else {
            apiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        // Unregister Node and Message listeners, disconnect GoogleApiClient and disable buttons
        Wearable.NodeApi.removeListener(apiClient, nodeListener);
        Wearable.MessageApi.removeListener(apiClient, messageListener);
        apiClient.disconnect();
        buttonAlarm.setEnabled(false);
        buttonStop.setEnabled(false);
        super.onPause();
    }

    void startVibrate() {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (vibrator.hasVibrator()) {
                    long[] pattern = {0, 400, 200};
                    vibrator.vibrate(pattern, 0);
                }
            }
        });
    }


    void stopVibrate() {
        handler.post(new Runnable() {
            @Override
            public void run() {


                vibrator.cancel();


            }
        });
    }


}
