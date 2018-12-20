package com.muc;

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.*;

/**
 * Created by jim on 4/21/17.
 */
public class MessagePane extends JPanel implements MessageListener {

    private final ChatClient client;
    private final String login;

    AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
    DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
    TargetDataLine targetDataLine ;

    private DefaultListModel<String> listModel = new DefaultListModel<>();
    private JList<String> messageList = new JList<>(listModel);
    private JTextField inputField = new JTextField();
    private JButton  recordbutton = new JButton ("record");

    public MessagePane(ChatClient client, String login) {
        this.client = client;
        this.login = login;

        client.addMessageListener(this);

        setLayout(new BorderLayout());
        add(new JScrollPane(messageList), BorderLayout.CENTER);
        JPanel subPanel = new JPanel();
        inputField.setColumns(30);
        inputField.setUI(new JTextFieldHintUI("Press enter to send or Record a voice message", Color.gray));
        subPanel.add( inputField);
        subPanel.add( recordbutton);

        add(subPanel, BorderLayout.SOUTH);

        inputField.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    String text = inputField.getText();
                    client.msg(login, text);
                    listModel.addElement("You: " + text);
                    inputField.setText("");
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        });

        final JFrame frame = new JFrame("Voice Recording");
        recordbutton.addActionListener(new ActionListener( ) {
            public void actionPerformed(ActionEvent ev) {
//                JOptionPane.showMessageDialog(frame,
//                        "Action Event"
//                );
                 byte[] audioData= voiceRecord();
                new Thread(){
                    public void run(){
//                        audioData = voiceRecord();
//                        capture(30000);
                    }
                }.start();



//                AudioInputStream audioInputStream;
//                SourceDataLine sourceDataLine = null;
//                AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
                int n = JOptionPane.showConfirmDialog(
                        frame, "Recording Press OK to send",
                        "Voice Record",
                        JOptionPane.OK_CANCEL_OPTION);
                if (n == JOptionPane.OK_OPTION) {
                    System.out.println("send recording");
                    client.sendaudiocmd();
                    client.sendaudio(audioData);
//                    stop();
//                    client.sendFile("sample.mp4");
                } else if (n == JOptionPane.CANCEL_OPTION) {
                    System.out.println("cancel recording");
                } else {
                    System.out.println("no selection");
                }

            }
        });

try{
        targetDataLine = (TargetDataLine)AudioSystem.getLine(info);
}catch (Exception e){

    e.printStackTrace();
}
    }

    @Override
    public void onMessage(String fromLogin, String msgBody) {
        if (login.equalsIgnoreCase(fromLogin)) {
            String line = fromLogin + ": " + msgBody;
            listModel.addElement(line);
        }
    }

    public byte[] voiceRecord(){
        AudioFormat format = new AudioFormat(8000.0f, 16, 1, true, true);
        TargetDataLine microphone;
        AudioInputStream audioInputStream;
        SourceDataLine sourceDataLine;
        byte audioData[]=null;
        try {
            microphone = AudioSystem.getTargetDataLine(format);

            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            microphone = (TargetDataLine) AudioSystem.getLine(info);
            microphone.open(format);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            int numBytesRead;
            int CHUNK_SIZE = 1024;
            byte[] data = new byte[microphone.getBufferSize() / 5];
            microphone.start();

            int bytesRead = 0;

            try {
                while (bytesRead < 100000) { // Just so I can test if recording
                    // my mic works...
                    numBytesRead = microphone.read(data, 0, CHUNK_SIZE);
                    bytesRead = bytesRead + numBytesRead;
                    System.out.println(bytesRead);
                    out.write(data, 0, numBytesRead);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
             audioData = out.toByteArray();
            microphone.close();
            return audioData;
//            // Get an input stream on the byte array
//            // containing the data
//            InputStream byteArrayInputStream = new ByteArrayInputStream(
//                    audioData);
//            audioInputStream = new AudioInputStream(byteArrayInputStream,format, audioData.length / format.getFrameSize());
//            DataLine.Info dataLineInfo = new DataLine.Info(SourceDataLine.class, format);
//            sourceDataLine = (SourceDataLine) AudioSystem.getLine(dataLineInfo);
//            sourceDataLine.open(format);
//            sourceDataLine.start();
//            int cnt = 0;
//            byte tempBuffer[] = new byte[10000];
//            try {
//                while ((cnt = audioInputStream.read(tempBuffer, 0,tempBuffer.length)) != -1) {
//                    if (cnt > 0) {
//                        // Write data to the internal buffer of
//                        // the data line where it will be
//                        // delivered to the speaker.
//                        sourceDataLine.write(tempBuffer, 0, cnt);
//                    }// end if
//                }
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
//            // Block and wait for internal buffer of the
//            // data line to empty.
//            sourceDataLine.drain();
//            sourceDataLine.close();
//            microphone.close();
        } catch (LineUnavailableException e) {
            e.printStackTrace();
        }
        return audioData;
    }

    public void stop(){
        try{
            System.out.println("stop :: in stop");
//            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
//            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if(!AudioSystem.isLineSupported(info))
            {
                System.out.println("Line not supported");}

//            targetDataLine = (TargetDataLine)AudioSystem.getLine(info);
            System.out.println("stop Recording....");

            System.out.println("targetDataLine.getLineInfo() in stop:::::::"+targetDataLine.getLineInfo());
            System.out.println("targetDataLine.isActive() in stop:::::::"+targetDataLine.isActive());
            System.out.println("targetDataLine.isOpen() in stop:::::::"+targetDataLine.isOpen());

            targetDataLine.stop();
            targetDataLine.close();
        }
        catch (Exception e) {
            System.err.println("Line unavailable: " + e);
        }
    }

    public void capture(int record_time) {
        try {
//            AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 2, 4, 44100, false);
//            DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
            if(!AudioSystem.isLineSupported(info))
            {
                System.out.println("Line not supported");}

//             targetDataLine =        (TargetDataLine)AudioSystem.getLine(info);

            targetDataLine.open();
            targetDataLine.start();
            Thread thread = new Thread()
            {
                @Override public void run()

                {
                    AudioInputStream audioStream = new AudioInputStream(targetDataLine);
                    File audioFile = new File("sample.mp4");

                    System.out.println("Recording is going to get saved in path :::: "+ audioFile.getAbsolutePath());
                    try{
                        AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, audioFile);
                    }
                    catch (IOException e){
                        e.printStackTrace();
                    }
                    System.out.println("Stopped Recording");
                }
            };
            thread.start();
            Thread.sleep(record_time*1000);
            targetDataLine.stop();
            targetDataLine.close();
            System.out.println("Ended recording test");
        } catch (LineUnavailableException e) {
            System.err.println("Line unavailable: " + e);
            System.exit(-2);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}
