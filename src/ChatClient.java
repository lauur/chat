import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.NoSuchElementException;
import java.util.Scanner;

public class ChatClient {
    private static final String SERVER_HOST = "localhost";
    private static final int SERVER_PORT = 8274;
    public static final String CLIENT_TITLE = "Czat";
    public static final String JOINED_TO_CHAT = "Dołączono do czatu";
    private Scanner in;
    private PrintWriter out;
    private Socket socket;
    static StringBuilder showText = new StringBuilder();
    private final JFrame frame = new JFrame();
    private final JTextArea textArea;
    private final JTextField messageField;
    static JButton buttonSend;
    private String username = null;
    private String password = null;
    private final DefaultListModel<String> usersModel = new DefaultListModel<>();

    public ChatClient() throws HeadlessException {
        frame.setMinimumSize(new Dimension(700, 500));
        frame.setResizable(false);
        frame.setTitle(CLIENT_TITLE);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JMenuBar menuBar = new JMenuBar();
        JMenu menu = new JMenu("Menu");
        JMenuItem info = new JMenuItem("Info");
        JMenuItem exit = new JMenuItem("Exit");

        menu.add(info);
        menu.add(exit);
        menuBar.add(menu);
        frame.setJMenuBar(menuBar);

        info.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                JOptionPane.showMessageDialog(null, "Aplikacja czat.\n" + "Zalogowano jako: " + username, "Info", JOptionPane.INFORMATION_MESSAGE);
            }
        });
        exit.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int i = JOptionPane.showConfirmDialog(null, "Zakończyć?", "Wyjście", JOptionPane.YES_NO_OPTION);
                if (i == 0) {
                    frame.dispose();
                    System.exit(0);
                }
            }
        });
        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        messageField = new JTextField();
        messageField.setEditable(false);

        buttonSend = new JButton("Wyślij");
        frame.getRootPane().setDefaultButton(buttonSend);
        buttonSend.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                out.println(messageField.getText());
                messageField.setText("");
            }
        });

        panel.add(messageField, BorderLayout.CENTER);
        panel.add(buttonSend, BorderLayout.EAST);

        textArea = new JTextArea();
        textArea.setEditable(false);
        JScrollPane scrollPaneMessages = new JScrollPane(textArea);
        scrollPaneMessages.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 5));

        JList<String> list = new JList<>();
        usersModel.addElement("                            ");
        list.setModel(usersModel);
        list.setLayoutOrientation(JList.VERTICAL);

        JScrollPane scrollPaneUsers = new JScrollPane();
        scrollPaneUsers.setViewportView(list);
        scrollPaneUsers.setBorder(BorderFactory.createEmptyBorder(10, 5, 20, 20));

        JPanel panelScroll = new JPanel();
        panelScroll.setLayout(new BorderLayout());
        panelScroll.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
        panelScroll.add(scrollPaneMessages, BorderLayout.CENTER);
        panelScroll.add(scrollPaneUsers, BorderLayout.EAST);

        JLabel labelUsers = new JLabel("zalogowani:");
        labelUsers.setHorizontalAlignment(SwingConstants.LEFT);
        JLabel labelTitle = new JLabel("Czat - Client");
        labelTitle.setHorizontalAlignment(SwingConstants.CENTER);

        JPanel panelLabel = new JPanel();
        panelLabel.setLayout(new BorderLayout());
        panelLabel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 20));
        panelLabel.add(labelTitle, BorderLayout.CENTER);
        panelLabel.add(labelUsers, BorderLayout.EAST);

        frame.add(panelLabel, BorderLayout.NORTH);
        frame.add(panelScroll, BorderLayout.CENTER);
        frame.add(panel, BorderLayout.SOUTH);
        frame.pack();
        frame.setVisible(true);
    }

    public void start() throws IOException {
        socket = new Socket(SERVER_HOST, SERVER_PORT);
        in = new Scanner(socket.getInputStream());
        out = new PrintWriter(socket.getOutputStream(), true);

        String response = "";

        while (!response.equals(ServerOption.ACCEPTED.toString())) {
            String[] options = {"LOGIN", "REGISTER"};
            int choice = JOptionPane.showOptionDialog(null, "Logowanie lub rejestracja",
                    "",
                    JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
            if (choice == 0) {
                new LoginDialog(null);
            } else if (choice == 1) {
                new RegisterDialog(null);
            } else {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
            }
            response = in.nextLine();
            if (ServerOption.ACCEPTED.toString().equals(response)) {
                System.out.println(JOINED_TO_CHAT);
                JOptionPane.showMessageDialog(null, JOINED_TO_CHAT);
                messageField.setEditable(true);
                messageField.setFocusable(true);
            } else if (ServerOption.NOT_ACCEPTED.toString().equals(response)) {
                response = in.nextLine();
                System.out.println(response);
                JOptionPane.showMessageDialog(null, response);
                username = null;
                password = null;
            } else {
                frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING));
                return;
            }
        }

        new Thread(new RecieveMessages()).start();
    }

    private class RecieveMessages implements Runnable {
        public void run() {
            String serverResponse;
            try {
                while ((serverResponse = in.nextLine()) != null) {
                    if (serverResponse.contains(ServerOption.SEND_USERS_LIST.toString())) {
                        String[] list = serverResponse.split("//")[1].trim().replaceAll("[\\[\\] \" ]", "").split(",");
                        usersModel.clear();
                        usersModel.addAll(Arrays.asList(list));
                    } else {
                        showText.append(serverResponse).append("\n");
                        textArea.setText(showText.toString());
                    }
                }
            } catch (IllegalStateException e) {
                System.out.println(e);
            } catch (NoSuchElementException e) {
                try {
                    System.out.println("Błąd serwera.");
                    JOptionPane.showMessageDialog(null, "Błąd serwera.", "Błąd", JOptionPane.ERROR_MESSAGE);
                    socket.close();
                    System.exit(1);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    private class LoginDialog extends JDialog {
        LoginDialog(JFrame frame) {
            super(frame, "Logowanie", true);
            JTextField fieldUsername = new JTextField(20);
            JTextField fieldPassword = new JPasswordField(20);

            JPanel loginPanel = new JPanel(new GridLayout(0, 1));
            loginPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            JLabel labelUsername = new JLabel("login:");
            JLabel labelPassword = new JLabel("hasło:");
            fieldUsername.setFocusable(true);
            loginPanel.add(labelUsername);
            loginPanel.add(fieldUsername);
            loginPanel.add(labelPassword);
            loginPanel.add(fieldPassword);

            JPanel buttonPanel = new JPanel();
            JButton bLogin = new JButton("Loguj");
            bLogin.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    username = fieldUsername.getText();
                    password = fieldPassword.getText();
                    setVisible(false);
                    out.println(ServerOption.CLIENT_LOGIN);
                    out.println(username);
                    out.println(password);
                }
            });
            buttonPanel.add(bLogin);
            getContentPane().add(loginPanel, BorderLayout.CENTER);
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            setLocationRelativeTo(null);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            getRootPane().setDefaultButton(bLogin);
            pack();
            setVisible(true);
        }
    }

    private class RegisterDialog extends JDialog {
        RegisterDialog(JFrame frame) {
            super(frame, "Rejestracja", true);
            JTextField fieldUsername = new JTextField(20);
            JTextField fieldPassword = new JPasswordField(20);
            JTextField fieldConfirmPassword = new JPasswordField(20);

            JPanel registerPanel = new JPanel(new GridLayout(0, 1));
            registerPanel.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 10));
            registerPanel.add(new JLabel("login:"));
            registerPanel.add(fieldUsername);
            registerPanel.add(new JLabel("hasło:"));
            registerPanel.add(fieldPassword);
            registerPanel.add(new JLabel("powtórz hasło:"));
            registerPanel.add(fieldConfirmPassword);

            JPanel buttonPanel = new JPanel();
            JButton bRegister = new JButton("Rejestruj");
            bRegister.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    setVisible(false);
                    username = fieldUsername.getText();
                    password = fieldPassword.getText();
                    String confirmPassword = fieldConfirmPassword.getText();
                    out.println(ServerOption.CLIENT_REGISTER);
                    out.println(username);
                    out.println(password);
                    out.println(confirmPassword);
                }
            });
            buttonPanel.add(bRegister);
            getContentPane().add(registerPanel, BorderLayout.CENTER);
            getContentPane().add(buttonPanel, BorderLayout.SOUTH);
            setLocationRelativeTo(null);
            setResizable(false);
            setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
            getRootPane().setDefaultButton(bRegister);
            pack();
            setVisible(true);
        }
    }

    public static void main(String[] args) throws IOException {
        ChatClient client = new ChatClient();
        client.start();
    }
}