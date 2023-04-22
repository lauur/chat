import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Scanner;
import java.util.Set;

public class ChatServer {
    private static final int PORT = 8274;
    public static final String FILENAME_USERS = "users.txt";

    private static final Map<String, String> registeredClients = new HashMap<>();
    private static final Set<String> loggedClients = new HashSet<>();
    private static final Set<PrintWriter> printWriters = new HashSet<>();
    private static final StringBuilder messagesHistory = new StringBuilder();

    public static void main(String[] args) throws Exception {
        getUsersFromFile();
        System.out.println(Messages.MESSAGE_SERVER_START);
        ServerSocket serverSocket = new ServerSocket(PORT);
        try {
            while (true) {
                new ClientHandler(serverSocket.accept()).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            serverSocket.close();
        }
    }

    private static void getUsersFromFile() {
        try {
            BufferedReader br = new BufferedReader(new FileReader(FILENAME_USERS));
            String line;
            while ((line = br.readLine()) != null) {
                if (line.contains(";")) {
                    String[] split = line.split(";");
                    String username = split[0];
                    String password = split[1];
                    registeredClients.put(username, password);
                }
            }
            br.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static void addUserToFile(String username, String password) {
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(FILENAME_USERS, true));
            bw.write("\n");
            bw.write(username + ";" + password);
            bw.close();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    private static class ClientHandler extends Thread {
        private final Socket socket;
        private PrintWriter out;
        private String username;

        public ClientHandler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {
                Scanner in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                while (in.hasNextLine()) {
                    String clientResponse = in.nextLine();
                    String password;
                    if (clientResponse.equals(ServerOption.CLIENT_LOGIN.toString())) {
                        username = in.nextLine();
                        password = in.nextLine();
                        if (loggedClients.contains(username)) {
                            out.println(ServerOption.NOT_ACCEPTED);
                            out.println(Messages.MESSAGE_LOGIN_USER_ALREADY_LOGGED_IN);
                            System.out.println(username + " " + ServerOption.NOT_ACCEPTED);
                            username = null;
                        } else if (!registeredClients.containsKey(username) || !registeredClients.get(username).equals(password)) {
                            out.println(ServerOption.NOT_ACCEPTED);
                            out.println(Messages.MESSAGE_LOGIN_USER_DOES_NOT_EXIST_OR_INVALID_PASSWORD);
                            System.out.println(username + " " + ServerOption.NOT_ACCEPTED);
                            username = null;
                        } else if (username != null) {
                            System.out.println(username + " " + ServerOption.ACCEPTED);
                            break;
                        }

                    } else if (clientResponse.equals(ServerOption.CLIENT_REGISTER.toString())) {
                        username = in.nextLine();
                        password = in.nextLine();
                        String confirmPassword = in.nextLine();
                        if (username == null || username.trim().equals("") || password == null || password.trim().equals("")) {
                            out.println(ServerOption.NOT_ACCEPTED);
                            out.println(Messages.MESSAGE_REGISTER_EMPTY_FIELDS);
                            System.out.println(username + " " + ServerOption.NOT_ACCEPTED);
                        } else if (!password.equals(confirmPassword)) {
                            out.println(ServerOption.NOT_ACCEPTED);
                            out.println(Messages.MESSAGE_REGISTER_DIFFERENT_PASSWORDS);
                            System.out.println(username + " " + ServerOption.NOT_ACCEPTED);
                        } else if (registeredClients.containsKey(username)) {
                            out.println(ServerOption.NOT_ACCEPTED);
                            out.println(Messages.MESSAGE_REGISTER_USERNAME_UNAVAILABLE);
                            System.out.println(username + " " + ServerOption.NOT_ACCEPTED);
                        } else {
                            registeredClients.put(username, password);
                            addUserToFile(username, password);
                            System.out.println(username + " " + ServerOption.ACCEPTED);
                            break;
                        }
                    }
                }
                if (username != null) {
                    loggedClients.add(username);
                    printWriters.add(out);
                    out.println(ServerOption.ACCEPTED);
                    out.println(messagesHistory);

                    String messageToAll = username + Messages.MESSAGE_CLIENT_JOINED;
                    addToHistory(messageToAll);
                    sendMessageToAll(messageToAll);
                    sendUsersListToAll();
                    System.out.println(messageToAll);
                    while (true) {
                        try {
                            String message = in.nextLine();
                            if (message == null) {
                                return;
                            }
                            String chatMessage = username + ": " + message;
                            addToHistory(chatMessage);
                            sendMessageToAll(chatMessage);
                        } catch (NoSuchElementException e) {
                            break;
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                if (username != null) {
                    loggedClients.remove(username);
                    String messageToAll = username + Messages.MESSAGE_CLIENT_LEFT;
                    System.out.println(messageToAll);
                    addToHistory(messageToAll);
                    sendMessageToAll(messageToAll);
                    sendUsersListToAll();
                }
                if (out != null) {
                    printWriters.remove(out);
                }

                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private static void addToHistory(String message) {
            messagesHistory.append(message).append("\n");
        }

        private static void sendMessageToAll(String messageToAll) {
            for (PrintWriter writer : printWriters) {
                writer.println(messageToAll);
            }
        }

        private static void sendUsersListToAll() {
            for (PrintWriter writer : printWriters) {
                writer.println(ServerOption.SEND_USERS_LIST + "//" + loggedClients);
            }
        }
    }
}
