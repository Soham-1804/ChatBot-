import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ChatBotUI {
    public static void main(String[] args) {
        JFrame f = new JFrame("ChatBot");
        f.setSize(600, 500);
        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        f.setLayout(new BorderLayout());

        JTextArea ta = new JTextArea();
        ta.setEditable(false);
        ta.setFont(new Font("Arial", Font.PLAIN, 14));
        ta.setBackground(Color.BLACK);
        ta.setForeground(Color.WHITE);
        JScrollPane sp = new JScrollPane(ta);

        JTextField tf = new JTextField(30);
        JButton btn = new JButton("Send");
        JButton resetBtn = new JButton("Reset Chat");

        JPanel bp = new JPanel();
        bp.add(tf);
        bp.add(btn);
        bp.add(resetBtn);

        f.add(sp, BorderLayout.CENTER);
        f.add(bp, BorderLayout.SOUTH);
        f.setVisible(true);

        ActionListener send = e -> {
            String msg = tf.getText().trim();
            if (msg.isEmpty()) return;
            ta.append("You: " + msg + "\n");
            tf.setText("");

            try {
                String reply = sendToPython(msg);
                ta.append("Bot: " + reply + "\n\n");
            } catch (Exception ex) {
                ta.append("Error: Cannot reach backend.\n\n");
                ex.printStackTrace();
            }
        };

        btn.addActionListener(send);
        tf.addActionListener(send);

        resetBtn.addActionListener(e -> {
            try {
                URL url = new URL("http://127.0.0.1:5000/reset");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                BufferedReader br = new BufferedReader(
                        new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
                String response = br.readLine();
                ta.append("System: " + response + "\n\n");
                br.close();
            } catch (Exception ex) {
                ta.append("Error resetting chat.\n");
                ex.printStackTrace();
            }
        });
    }

    private static String sendToPython(String message) throws IOException {
        URL url = new URL("http://127.0.0.1:5000/chat");
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);

        String jsonInput = "{\"message\":\"" + message + "\"}";
        try (OutputStream os = conn.getOutputStream()) {
            os.write(jsonInput.getBytes(StandardCharsets.UTF_8));
        }

        BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = br.readLine()) != null) {
            response.append(line.trim());
        }
        br.close();

        String res = response.toString();
        int start = res.indexOf(":") + 2;
        int end = res.lastIndexOf("\"");
        return res.substring(start, end);
    }
}
