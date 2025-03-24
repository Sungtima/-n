package appchat;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.net.*;
import java.util.Random;

public class ChatClient {
    private static final String SERVER_ADDRESS = "localhost";
    private static final int SERVER_PORT = 12345;
    
    private JFrame frame;
    private JTextPane messageArea;
    private JTextField inputField;
    private JTextField roomField;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private PrintWriter out;
    private BufferedReader in;
    private Socket socket;
    private String username;
    private Random random = new Random();

    public ChatClient() {
        initializeGUI();
        connectToServer();
    }

    private void initializeGUI() {
        frame = new JFrame("Chat Client - Phòngvjp.com");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(700, 500);
        frame.setLayout(new BorderLayout(10, 10));
        frame.setLocationRelativeTo(null);

        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Khu vực tin nhắn
        messageArea = new JTextPane();
        messageArea.setEditable(false);
        messageArea.setBackground(new Color(255, 255, 255));
        messageArea.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        JScrollPane messageScroll = new JScrollPane(messageArea);
        messageScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("💬 Tin nhắn"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Danh sách người dùng
        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setBackground(new Color(245, 245, 245));
        JScrollPane userScroll = new JScrollPane(userList);
        userScroll.setPreferredSize(new Dimension(150, 0));
        userScroll.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createTitledBorder("👥 Người dùng"),
            BorderFactory.createEmptyBorder(5, 5, 5, 5)
        ));

        // Panel phía dưới
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        inputField = new JTextField();
        inputField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true));
        JButton sendButton = new JButton("Gửi 📤");
        sendButton.setBackground(new Color(70, 130, 180));
        sendButton.setForeground(Color.blue );
        sendButton.setFocusPainted(false);
        
        // Panel biểu tượng cảm xúc
        JPanel emojiPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        String[] emojis = {"😊", "😂", "😍", "😢", "👍", "👎"};
        for (String emoji : emojis) {
            JButton emojiButton = new JButton(emoji);
            emojiButton.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 16));
            emojiButton.setMargin(new Insets(2, 2, 2, 2));
            emojiButton.addActionListener(e -> inputField.setText(inputField.getText() + emoji));
            emojiPanel.add(emojiButton);
        }
        
        bottomPanel.add(emojiPanel, BorderLayout.NORTH);
        bottomPanel.add(inputField, BorderLayout.CENTER);
        bottomPanel.add(sendButton, BorderLayout.EAST);

        // Panel phía trên
        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        JLabel roomLabel = new JLabel("🏠 Phòng: ");
        roomLabel.setFont(new Font("Segoe UI", Font.BOLD, 14));
        roomField = new JTextField("phòngvjp.com");
        roomField.setBorder(BorderFactory.createLineBorder(new Color(100, 100, 100), 1, true));
        JButton joinButton = new JButton("Tham gia 🚪");
        joinButton.setBackground(new Color(60, 179, 113));
        joinButton.setForeground(Color.green );
        joinButton.setFocusPainted(false);
        
        topPanel.add(roomLabel, BorderLayout.WEST);
        topPanel.add(roomField, BorderLayout.CENTER);
        topPanel.add(joinButton, BorderLayout.EAST);
        topPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        // Sắp xếp layout
        frame.add(topPanel, BorderLayout.NORTH);
        frame.add(messageScroll, BorderLayout.CENTER);
        frame.add(userScroll, BorderLayout.EAST);
        frame.add(bottomPanel, BorderLayout.SOUTH);
        frame.getContentPane().setBackground(new Color(240, 248, 255));

        // Sự kiện
        sendButton.addActionListener(e -> sendMessage());
        inputField.addActionListener(e -> sendMessage());
        joinButton.addActionListener(e -> joinRoom());
        roomField.addActionListener(e -> joinRoom());

        frame.setVisible(true);
    }

    private void connectToServer() {
        try {
            socket = new Socket(SERVER_ADDRESS, SERVER_PORT);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            new Thread(() -> {
                try {
                    String message;
                    while ((message = in.readLine()) != null) {
                        if (message.startsWith("/username")) {
                            // Tạo hộp thoại nhập tên rõ ràng hơn
                            JTextField nameField = new JTextField(15);
                            JPanel panel = new JPanel(new BorderLayout(5, 5));
                            panel.add(new JLabel("👤 Nhập tên của bạn:"), BorderLayout.NORTH);
                            panel.add(nameField, BorderLayout.CENTER);
                            
                            int result = JOptionPane.showConfirmDialog(
                                frame, 
                                panel, 
                                "Đăng nhập", 
                                JOptionPane.OK_CANCEL_OPTION, 
                                JOptionPane.PLAIN_MESSAGE
                            );
                            
                            if (result == JOptionPane.OK_OPTION) {
                                username = nameField.getText().trim();
                                if (username.isEmpty()) {
                                    username = "User" + random.nextInt(1000);
                                }
                            } else {
                                username = "User" + random.nextInt(1000);
                            }
                            out.println(username);
                            appendColoredMessage("👋 Chào mừng " + username + " đến với chat!");
                        } else if (message.startsWith("/userlist")) {
                            updateUserList(message.substring(10));
                        } else {
                            appendColoredMessage(message);
                        }
                    }
                } catch (IOException e) {
                    appendColoredMessage("⚠ Lỗi kết nối: " + e.getMessage());
                }
            }).start();

        } catch (IOException e) {
            appendColoredMessage("❌ Không thể kết nối đến server: " + e.getMessage());
        }
    }

    private void appendColoredMessage(String message) {
        StyledDocument doc = messageArea.getStyledDocument();
        Style style = messageArea.addStyle("ColorStyle", null);
        
        if (message.startsWith("[Hệ thống]") || message.startsWith("⚠") || message.startsWith("❌")) {
            StyleConstants.setForeground(style, new Color(220, 20, 60)); // Crimson
            StyleConstants.setBold(style, true);
        } else if (message.startsWith("👋")) {
            StyleConstants.setForeground(style, new Color(0, 128, 0)); // Green
            StyleConstants.setBold(style, true);
        } else {
            Color randomColor = new Color(
                random.nextInt(200),
                random.nextInt(200),
                random.nextInt(200)
            );
            StyleConstants.setForeground(style, randomColor);
        }

        try {
            doc.insertString(doc.getLength(), message + "\n", style);
            messageArea.setCaretPosition(doc.getLength());
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void updateUserList(String users) {
        userListModel.clear();
        String[] userArray = users.split(",");
        for (String user : userArray) {
            if (!user.isEmpty()) {
                userListModel.addElement("👤 " + user);
            }
        }
    }

    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            if (message.equalsIgnoreCase("/exit")) {
                try {
                    out.println(message);
                    socket.close();
                    frame.dispose();
                } catch (IOException e) {
                    appendColoredMessage("❌ Lỗi khi thoát: " + e.getMessage());
                }
            } else {
                out.println(message);
                inputField.setText("");
            }
        }
    }

    private void joinRoom() {
        String room = roomField.getText().trim();
        if (!room.isEmpty()) {
            out.println("/join " + room);
            frame.setTitle("Chat Client - Phòng: " + room);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ChatClient::new);
    }
}