package appchat;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ChatServer {
    private static final int PORT = 12345;
    private static ConcurrentHashMap<String, HashSet<ClientHandler>> rooms = new ConcurrentHashMap<>();

    public static void main(String[] args) {
        System.out.println("Chat Server dang chay...");
        
        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            while (true) {
                Socket clientSocket = serverSocket.accept();
                Thread clientThread = new Thread(new ClientHandler(clientSocket));
                clientThread.start();
            }
        } catch (IOException e) {
            System.out.println("Loi server: " + e.getMessage());
        }
    }

    private static class ClientHandler implements Runnable {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private String currentRoom = "Khánh";
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Yêu cầu username từ client
                out.println("/username");
                username = in.readLine();
                if (username == null) username = "User" + new Random().nextInt(1000);

                // Thêm vào phòng mặc định
                joinRoom("Phòngvjp.com");

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.startsWith("/join ")) {
                        String newRoom = message.substring(6).trim();
                        joinRoom(newRoom);
                    } else if (message.equalsIgnoreCase("/exit")) {
                        break;
                    } else {
                        broadcast(currentRoom, username + ": " + message);
                    }
                }
            } catch (IOException e) {
                System.out.println("Lỗi xử lý client: " + e.getMessage());
            } finally {
                leaveRoom();
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void joinRoom(String room) {
            leaveRoom();
            currentRoom = room;
            rooms.putIfAbsent(room, new HashSet<>());
            synchronized (rooms.get(room)) {
                rooms.get(room).add(this);
            }
            broadcast(room, "[Hệ thống] " + username + " đã tham gia phòng");
            sendUserList(room);
        }

        private void leaveRoom() {
            if (rooms.containsKey(currentRoom)) {
                synchronized (rooms.get(currentRoom)) {
                    rooms.get(currentRoom).remove(this);
                    broadcast(currentRoom, "[Hệ thống] " + username + " đã rời phòng");
                    sendUserList(currentRoom);
                    if (rooms.get(currentRoom).isEmpty()) {
                        rooms.remove(currentRoom);
                    }
                }
            }
        }

        private void broadcast(String room, String message) {
            if (rooms.containsKey(room)) {
                synchronized (rooms.get(room)) {
                    for (ClientHandler client : rooms.get(room)) {
                        client.out.println(message);
                    }
                }
            }
        }

        private void sendUserList(String room) {
            if (rooms.containsKey(room)) {
                StringBuilder userList = new StringBuilder("/userlist ");
                synchronized (rooms.get(room)) {
                    for (ClientHandler client : rooms.get(room)) {
                        userList.append(client.username).append(",");
                    }
                }
                broadcast(room, userList.toString());
            }
        }
    }
}