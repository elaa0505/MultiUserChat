package com.muc;

import org.apache.commons.lang3.StringUtils;

import javax.sound.sampled.*;
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

/**
 * Created by jim on 4/21/17.
 */
public class ChatClient {
    private final String serverName;
    private final int serverPort;
    private Socket socket;
    private InputStream serverIn;
    private OutputStream serverOut;
    private BufferedReader bufferedIn;

    private ArrayList<UserStatusListener> userStatusListeners = new ArrayList<>();
    private ArrayList<MessageListener> messageListeners = new ArrayList<>();

    public ChatClient(String serverName, int serverPort) {
        this.serverName = serverName;
        this.serverPort = serverPort;
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient("localhost", 8818);
        client.addUserStatusListener(new UserStatusListener() {
            @Override
            public void online(String login) {
                System.out.println("ONLINE: " + login);
            }

            @Override
            public void offline(String login) {
                System.out.println("OFFLINE: " + login);
            }
        });

        client.addMessageListener(new MessageListener() {
            @Override
            public void onMessage(String fromLogin, String msgBody) {
                System.out.println("You got a message from " + fromLogin + " ===>" + msgBody);
            }
        });

        if (!client.connect()) {
            System.err.println("Connect failed.");
        } else {
            System.out.println("Connect successful");

            if (client.login("guest", "guest")) {
                System.out.println("Login successful");

                client.msg("jim", "Hello World!");
            } else {
                System.err.println("Login failed");
            }

            //client.logoff();
        }
    }

    public void msg(String sendTo, String msgBody) throws IOException {
        String cmd = "msg " + sendTo + " " + msgBody + "\n";
        serverOut.write(cmd.getBytes());
    }
    public void sendaudiocmd(){
        try {
            serverOut.write("audio".getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendaudio(byte [] audioData){
        try {
            String cmd="audio ";
            serverOut.write(audioData,0,audioData.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void sendFile(String FILE_TO_SEND) {
        File myFile = new File (FILE_TO_SEND);
        byte [] mybytearray  = new byte [(int)myFile.length()];

        System.out.println("Sending " + FILE_TO_SEND + "(" + mybytearray.length + " bytes)");
        try {
            serverOut.write(mybytearray,0,mybytearray.length);
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Done.");
    }


    public boolean login(String login, String password) throws IOException {
        String cmd = "login " + login + " " + password + "\n";
        serverOut.write(cmd.getBytes());

        String response = bufferedIn.readLine();
        System.out.println("Response Line:" + response);

        if ("ok login".equalsIgnoreCase(response)) {
            startMessageReader();
            return true;
        } else {
            return false;
        }
    }

    public void logoff() throws IOException {
        String cmd = "logoff\n";
        serverOut.write(cmd.getBytes());
    }

    private void startMessageReader() {
        Thread t = new Thread() {
            @Override
            public void run() {
                readMessageLoop();
            }
        };
        t.start();
    }

    private void readMessageLoop() {
        try {
            String line;
            while ((line = bufferedIn.readLine()) != null) {
                String[] tokens = StringUtils.split(line);
                if (tokens != null && tokens.length > 0) {
                    String cmd = tokens[0];
                    if ("online".equalsIgnoreCase(cmd)) {
                        handleOnline(tokens);
                    } else if ("offline".equalsIgnoreCase(cmd)) {
                        handleOffline(tokens);
                    } else if ("msg".equalsIgnoreCase(cmd)) {
                        String[] tokensMsg = StringUtils.split(line, null, 3);
                        handleMessage(tokensMsg);
                    } else if ("audio".equalsIgnoreCase(cmd)) {
//                        String[] tokensMsg = StringUtils.split(line, null, 3);
//                        handleMessage2(tokensMsg);
                          byte audioData[] = new byte[0];
                                    AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
                                        AudioInputStream audioInputStream;
                        SourceDataLine sourceDataLine = null;
                          serverIn.read(audioData);
                        // Get an input stream on the byte array
                        // containing the data
                        InputStream byteArrayInputStream = new ByteArrayInputStream(
                                audioData);
                        audioInputStream = new AudioInputStream(byteArrayInputStream,format, audioData.length / format.getFrameSize());
                        DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
                        try {
                            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
                        } catch (LineUnavailableException e) {
                            e.printStackTrace();
                        }
                        try {
                            sourceDataLine.open(format);
                        } catch (LineUnavailableException e) {
                            e.printStackTrace();
                        }
                        sourceDataLine.start();
                        int cnt = 0;
                        byte tempBuffer[] = new byte[10000];
                        try {
                            while ((cnt = audioInputStream.read(tempBuffer, 0,tempBuffer.length)) != -1) {
                                if (cnt > 0) {
                                    // Write data to the internal buffer of
                                    // the data line where it will be
                                    // delivered to the speaker.
                                    sourceDataLine.write(tempBuffer, 0, cnt);
                                }// end if
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        // Block and wait for internal buffer of the
                        // data line to empty.
                        sourceDataLine.drain();
                        sourceDataLine.close();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            try {
                socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleMessage(String[] tokensMsg) {
        String login = tokensMsg[1];
        String msgBody = tokensMsg[2];

        for(MessageListener listener : messageListeners) {
            listener.onMessage(login, msgBody);
        }
    }
    public final static String
            FILE_TO_RECEIVED = "sample.mp4";
    public final static int FILE_SIZE = 6022386; // file size temporary hard coded

    int bytesRead;
    int current = 0;
    private void handleMessage2(String[] tokensMsg) {
//        String login = tokensMsg[1];
//        String msgBody = tokensMsg[2];
//
//        for(MessageListener listener : messageListeners) {
//            listener.onMessage(login, msgBody);
//        }
        FileOutputStream fos = null;
        BufferedOutputStream bos = null;
    }

    private void handleOffline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.offline(login);
        }
    }

    private void handleOnline(String[] tokens) {
        String login = tokens[1];
        for(UserStatusListener listener : userStatusListeners) {
            listener.online(login);
        }
    }

    public boolean connect() {
        try {
            this.socket = new Socket(serverName, serverPort);
            System.out.println("Client port is " + socket.getLocalPort());
            this.serverOut = socket.getOutputStream();
            this.serverIn = socket.getInputStream();
            this.bufferedIn = new BufferedReader(new InputStreamReader(serverIn));
            return true;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void addUserStatusListener(UserStatusListener listener) {
        userStatusListeners.add(listener);
    }

    public void removeUserStatusListener(UserStatusListener listener) {
        userStatusListeners.remove(listener);
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

}
