import java.io.*;
import java.net.*;

public class BookClient {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("ERROR: Provide 2 arguments: command-file, clientId");
            System.exit(-1);
        }

        String commandFile = args[0];
        String clientId = args[1];
        String outFile = "out_" + clientId + ".txt";
        String hostAddress = "localhost";
        int tcpPort = 7500; // hardcoded -- must match the server's tcp port
        int udpPort = 8500; // hardcoded -- must match the server's udp port
        String mode = "TCP"; // Default mode

        try (BufferedReader fileReader = new BufferedReader(new FileReader(commandFile));
                PrintWriter outputWriter = new PrintWriter(new FileWriter(outFile));
                Socket socket = new Socket(hostAddress, tcpPort)) {

            // Set a 5-second timeout to prevent indefinite blocking
            socket.setSoTimeout(5000);

            try (BufferedReader serverReader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    PrintWriter serverWriter = new PrintWriter(socket.getOutputStream(), true)) {

                String command;
                while ((command = fileReader.readLine()) != null) {
                    String[] tokens = command.split(" ");

                    // Print the command before sending it
                    System.out.println("Sending command: " + command);

                    if (tokens[0].equals("set-mode")) {
                        if (tokens.length == 2) {
                            mode = tokens[1].equals("t") ? "TCP" : "UDP";
                            outputWriter.println("The communication mode is set to " + mode);
                        } else {
                            outputWriter.println("ERROR: Invalid set-mode command");
                        }
                    } else {
                        if (mode.equals("TCP")) {
                            serverWriter.println(command);
                            System.out.println("Command sent: " + command);
                            StringBuilder fullResponse = new StringBuilder();
                            String response;
                            boolean hasMoreData = true;
                            while (hasMoreData) {
                                try {
                                    response = serverReader.readLine();
                                    if (response == null) {
                                        System.out.println("No more data - connection closed");
                                        hasMoreData = false;
                                    } else if (response.isEmpty()) {
                                        System.out.println("Received empty line - stopping");
                                        hasMoreData = false;
                                    } else {
                                        System.out.println("Received line: '" + response + "'");
                                        fullResponse.append(response).append("\n");
                                    }
                                } catch (SocketTimeoutException e) {
                                    System.out.println("Socket timeout - no more data expected");
                                    hasMoreData = false;
                                }
                            }
                            // Only write non-empty, non-null responses
                            String finalResponse = fullResponse.toString().trim();
                            if (!finalResponse.isEmpty() && !finalResponse.equals("null")) {
                                outputWriter.println(finalResponse);
                                System.out.println("Full response written: " + finalResponse);
                            } else {
                                System.out.println("No response received or response empty/null - skipping write");
                            }
                        } else {
                            // Handle UDP mode (unchanged)
                            DatagramSocket udpSocket = new DatagramSocket();
                            byte[] sendData = command.getBytes();
                            InetAddress IPAddress = InetAddress.getByName(hostAddress);
                            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, IPAddress,
                                    udpPort);
                            udpSocket.send(sendPacket);

                            byte[] receiveData = new byte[1024];
                            DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                            udpSocket.receive(receivePacket);
                            String response = new String(receivePacket.getData(), 0, receivePacket.getLength());
                            outputWriter.println(response);

                            udpSocket.close();
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}