import java.io.*;
import java.net.*;
import java.util.*;

public class BookServer {
    private static int tcpPort = 7500;
    private static int udpPort = 8500;
    private static Map<String, Integer> inventory = new HashMap<>();
    private static Map<Integer, String[]> loans = new HashMap<>();
    private static int loanIdCounter = 1;
    private static String mode = "TCP"; // Default mode

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            System.out.println("ERROR: Provide 1 argument: input file containing initial inventory");
            System.exit(-1);
        }

        System.out.println("Starting BookServer...");
        loadInventory(args[0]);

        System.out.println("Loaded inventory, starting TCP and UDP servers...");

        new Thread(() -> startTCPServer()).start();
        startUDPServer();
    }

    private static void loadInventory(String filename) throws IOException {
        System.out.println("Loading inventory from file: " + filename);
        BufferedReader reader = new BufferedReader(new FileReader(filename));
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println("Read line: " + line);
            String[] parts = line.split("\" ");
            if (parts.length == 2) {
                String bookName = parts[0] + "\"";
                int quantity = Integer.parseInt(parts[1]);
                inventory.put(bookName, quantity);
                System.out.println("Added to inventory: " + bookName + " - " + quantity);
            }
        }
        reader.close();
        System.out.println("Finished loading inventory.");
    }

    private static void startTCPServer() {
        try (ServerSocket serverSocket = new ServerSocket(tcpPort)) {
            System.out.println("TCP Server started on port " + tcpPort);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                new Thread(new ClientHandler(clientSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void startUDPServer() {
        try (DatagramSocket udpSocket = new DatagramSocket(udpPort)) {
            System.out.println("UDP Server started on port " + udpPort);
            byte[] receiveBuffer = new byte[1024];
            while (true) {
                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                udpSocket.receive(receivePacket);
                new Thread(new UDPHandler(receivePacket, udpSocket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static synchronized String processCommand(String command) {
        String[] tokens = command.split(" ", 2);
        String action = tokens[0];

        switch (action) {
            case "set-mode":
                return handleSetMode(tokens);
            case "begin-loan":
                return handleLoan(tokens[1]);
            case "end-loan":
                return handleReturn(tokens[1]);
            case "get-loans":
                return getLoans(tokens[1]);
            case "get-inventory":
                return getInventory();
            case "exit":
                saveInventoryToFile();
                return null;
            default:
                return "ERROR: Invalid command";
        }
    }

    private static synchronized String handleSetMode(String[] tokens) {
        if (tokens.length != 2) {
            return "ERROR: Invalid set-mode command";
        }
        mode = tokens[1].equals("t") ? "TCP" : "UDP";
        return "The communication mode is set to " + mode;
    }

    private static synchronized String handleLoan(String args) {
        String[] parts = args.split(" \"");
        if (parts.length < 2)
            return "ERROR: Invalid loan request";

        String user = parts[0];
        String book = "\"" + parts[1];

        System.out.println("DEBUG: Attempting loan for book: " + book); // Debugging line

        if (!inventory.containsKey(book))
            return "Request Failed - We do not have this book";
        if (inventory.get(book) == 0)
            return "Request Failed - Book not available";

        inventory.put(book, inventory.get(book) - 1);
        loans.put(loanIdCounter, new String[] { user, book });

        System.out.println("DEBUG: Book loaned successfully. Updated inventory: " + inventory); // Debugging line

        return "Your request has been approved, " + (loanIdCounter++) + " " + user + " " + book;
    }

    private static synchronized String handleReturn(String loanIdStr) {
        int loanId = Integer.parseInt(loanIdStr);
        if (!loans.containsKey(loanId))
            return loanId + " not found, no such borrow record";

        String book = loans.get(loanId)[1];
        inventory.put(book, inventory.get(book) + 1);
        loans.remove(loanId);
        return loanId + " is returned";
    }

    private static synchronized String getLoans(String user) {
        StringBuilder result = new StringBuilder();
        for (Map.Entry<Integer, String[]> entry : loans.entrySet()) {
            if (entry.getValue()[0].equals(user)) {
                result.append(entry.getKey()).append(" ").append(entry.getValue()[1]).append("\n");
            }
        }
        return result.length() == 0 ? "No record found for " + user : result.toString().trim();
    }

    private static synchronized String getInventory() {
        System.out.println("DEBUG: Inventory at time of get-inventory request: " + inventory);

        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
            result.append(entry.getKey()).append(" ").append(entry.getValue()).append("\n");
        }
        return result.toString().trim();
    }

    private static void saveInventoryToFile() {
        try (PrintWriter writer = new PrintWriter(new FileWriter("inventory.txt"))) {
            for (Map.Entry<String, Integer> entry : inventory.entrySet()) {
                writer.println(entry.getKey() + " " + entry.getValue());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class ClientHandler implements Runnable {
    private Socket clientSocket;

    public ClientHandler(Socket socket) {
        this.clientSocket = socket;
    }

    @Override
    public void run() {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
                PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true)) {

            String input;
            while ((input = in.readLine()) != null) {
                String response = BookServer.processCommand(input);
                out.println(response);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class UDPHandler implements Runnable {
    private DatagramPacket receivePacket;
    private DatagramSocket udpSocket;

    public UDPHandler(DatagramPacket packet, DatagramSocket socket) {
        this.receivePacket = packet;
        this.udpSocket = socket;
    }

    @Override
    public void run() {
        String received = new String(receivePacket.getData(), 0, receivePacket.getLength());
        String response = BookServer.processCommand(received);

        byte[] responseData = response.getBytes();
        DatagramPacket sendPacket = new DatagramPacket(responseData, responseData.length,
                receivePacket.getAddress(), receivePacket.getPort());

        try {
            udpSocket.send(sendPacket);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}