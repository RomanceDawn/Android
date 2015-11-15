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
import android.content.DialogInterface;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.support.wearable.activity.ConfirmationActivity;
import android.view.View;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

public class MainActivity extends Activity {
    private final String PATH_ALARM = "/alarm";
    private final String PATH_STOP = "/stop";

    private GoogleApiClient apiClient;
    private View buttonAlarm;
    private View buttonStop;
    private NodeApi.NodeListener nodeListener;
    private MessageApi.MessageListener messageListener;
    private String remoteNodeId;
    private Handler handler;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_activity);

        handler = new Handler();


        buttonAlarm = findViewById(R.id.button_alarm);
        buttonStop = findViewById(R.id.button_stop);
        //mediaPlayer = MediaPlayer.create(getApplicationContext(), R.raw.alarm);
        vibrator = (Vibrator) getSystemService(getApplicationContext().VIBRATOR_SERVICE);


        // Set message1Button onClickListener to send message 1
         buttonAlarm.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, PATH_ALARM, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                     @Override
                     public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                         Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                         if (sendMessageResult.getStatus().isSuccess()) {
                             intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                             intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.message1_sent));
                             startVibrate();
                         } else {
                             intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                             intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.error_message1));
                         }
                         startActivity(intent);
                     }
                 });
             }
         });

        // Set message1Button onClickListener to send message 1
        buttonStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Wearable.MessageApi.sendMessage(apiClient, remoteNodeId, PATH_STOP, null).setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                    @Override
                    public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                        Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                        if (sendMessageResult.getStatus().isSuccess()) {
                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.message2_sent));
                            stopVibrate();
                        } else {
                            intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                            intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.error_message2));
                        }
                        startActivity(intent);
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
                    }
                });
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.SUCCESS_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.peer_connected));
                startActivity(intent);
            }

            @Override
            public void onPeerDisconnected(Node node) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                         buttonAlarm.setEnabled(false);
                         buttonStop.setEnabled(false);
                    }
                });
                Intent intent = new Intent(getApplicationContext(), ConfirmationActivity.class);
                intent.putExtra(ConfirmationActivity.EXTRA_ANIMATION_TYPE, ConfirmationActivity.FAILURE_ANIMATION);
                intent.putExtra(ConfirmationActivity.EXTRA_MESSAGE, getString(R.string.peer_disconnected));
                startActivity(intent);
            }
        };

        // Create MessageListener that receives messages sent from a mobile
        messageListener = new MessageApi.MessageListener() {
            @Override
            public void onMessageReceived(MessageEvent messageEvent) {
                if (messageEvent.getPath().equals(PATH_ALARM)) {
                    startVibrate();
                }
                else if (messageEvent.getPath().equals(PATH_STOP)) {
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
        }).addApi(Wearable.API).build();
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


    void startVibrate()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {

                if (vibrator.hasVibrator()) {
                    long[] pattern = {0, 500, 600};
                    vibrator.vibrate(pattern, 1);
                }
            }
        });
    }


    void stopVibrate()
    {
        handler.post(new Runnable() {
            @Override
            public void run() {


                    vibrator.cancel();


            }
        });
    }
}
