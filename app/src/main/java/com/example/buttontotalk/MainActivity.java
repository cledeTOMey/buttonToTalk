package com.example.buttontotalk;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class MainActivity extends AppCompatActivity {

    private static final String SERVER_URI = "tcp://192.168.43.225";
    private static final String SUBSCRIPTION_TOPIC = "microphone";

    private static final String LISTEN_MESSAGE = "listen";
    private static final String STOP_LISTENING_MESSAGE = "stop";


    private MqttAndroidClient mqttAndroidClient;
    private boolean isListening = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final Button button = findViewById(R.id.speechButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final String messageToPublish = isListening ? STOP_LISTENING_MESSAGE : LISTEN_MESSAGE;
                publishMessage(messageToPublish);
                isListening = !isListening;
            }
        });

        mqttAndroidClient = createMQTTAndroidClient();
        connectToMQTTBroker();
    }

    public void publishMessage(final String messageToPublish) {

        try {
            MqttMessage message = new MqttMessage();
            message.setPayload(messageToPublish.getBytes());
            mqttAndroidClient.publish(SUBSCRIPTION_TOPIC, message);
            Log.d("MainActivity", "Message Published");
            if (!mqttAndroidClient.isConnected()) {
                Log.d("MainActivity", mqttAndroidClient.getBufferedMessageCount() + " messages in buffer.");
            }
        } catch (MqttException e) {
            System.err.println("Error Publishing: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void subscribeToTopic() {
        try {
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, 0, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d("MainActivity", "Subscribed!");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("MainActivity", "Failed to subscribe");
                }
            });

            // THIS DOES NOT WORK!
            mqttAndroidClient.subscribe(SUBSCRIPTION_TOPIC, 0, new IMqttMessageListener() {
                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    // message Arrived!
                    System.out.println("Message: " + topic + " : " + new String(message.getPayload()));
                }
            });

        } catch (MqttException ex) {
            System.err.println("Exception whilst subscribing");
            ex.printStackTrace();
        }
    }

    private MqttAndroidClient createMQTTAndroidClient() {
        final String clientId = "smartphone";

        MqttAndroidClient mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), SERVER_URI, clientId);
        mqttAndroidClient.setCallback(new MqttCallbackExtended() {
            @Override
            public void connectComplete(boolean reconnect, String serverURI) {
                if (reconnect)
                    subscribeToTopic();
            }

            @Override
            public void connectionLost(Throwable cause) {
                Log.d("MainActivity", "The Connection was lost.");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) {
                Log.d("MainActivity", "Incoming message: " + new String(message.getPayload()));
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {
                Log.d("MainActivity", "Delivery completed");
            }
        });

        return mqttAndroidClient;
    }

    private void connectToMQTTBroker() {
        MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
        mqttConnectOptions.setAutomaticReconnect(true);
        mqttConnectOptions.setCleanSession(false);

        try {
            Log.d("MainActivity", "Connecting to " + SERVER_URI);
            mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
                    subscribeToTopic();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    Log.d("MainActivity", "Failed to connect to: " + SERVER_URI);
                    Log.d("MainActivity", "Error: " + exception);
                }
            });
        } catch (MqttException ex) {
            ex.printStackTrace();
        }
    }
}
