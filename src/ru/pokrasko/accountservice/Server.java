package ru.pokrasko.accountservice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.sql.SQLException;

public class Server {
    static final String SERVICE_NAME = "account-service";

    private static void printHelp() {
        System.out.println("Commands:");
        System.out.println("running - the number of requests running at the moment;");
        System.out.println("total - the total number of requests started since the program" +
                "beginning or the last command reset;");
        System.out.println("reset - set the total number of requests to 0.");
        System.out.println();
    }

    public static void main(String[] args) {
        if (args.length < 2 || args.length > 3 || args[0] == null || args[1] == null
                || args.length == 3 && args[2] == null) {
            System.err.println("Usage: Server <port> [database port] <database name>");
            return;
        }

        final int port = Integer.parseInt(args[0]);
        final String dbUrl;
        if (args.length == 3) {
            dbUrl = "jdbc:mysql://localhost:" + args[1] + "/" + args[2];
        } else {
            dbUrl = "jdbc:mysql://localhost:3306/" + args[1];
        }
        try (DatabaseHelper helper = new DatabaseHelper(dbUrl)) {
            AccountServiceImpl service = new AccountServiceImpl(helper);

            try {
                AccountService serviceStub = (AccountService) UnicastRemoteObject.exportObject(service, 0);
                LocateRegistry.createRegistry(port).rebind(SERVICE_NAME, serviceStub);
            } catch (RemoteException e) {
                System.err.println("Cannot export object. " + e);
                return;
            }

            printHelp();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(System.in))) {
                while (true) {
                    String line = in.readLine();
                    try {
                        switch (line) {
                            case "running":
                                System.out.println("" + service.runningRequests() + " requests" +
                                        " are running now.");
                                break;
                            case "total":
                                System.out.println("" + service.totalRequests() + " requests" +
                                        " have been started since the last reset.");
                                break;
                            case "reset":
                                service.resetStats();
                                System.out.println("Stats have been reset successfully.");
                                break;
                            case "help":
                                printHelp();
                                break;
                            default:
                                System.out.println("Wrong command. Print \"help\" to see" +
                                        "the list of the commands.");
                        }
                    } catch (Exception e) {
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("Couldn't read from console (" + e + ")");
            }
        } catch (SQLException e) {
            System.err.println("SQL Exception at start (" + e + ")");
        }
    }
}
