package ru.pokrasko.accountservice;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.*;
import java.util.concurrent.*;

public class Client {
    private static ExecutorService getService;
    private static ExecutorService setService;

    private static List<Integer> parseIntervals(String s) {
        List<Integer> result = new ArrayList<>();
        String[] commaSeparated = s.split(",");
        for (String commaElement : commaSeparated) {
            String[] dashSeparated = commaElement.split("-");
            if (dashSeparated.length == 1) {
                Integer a;
                try {
                    a = Integer.parseInt(dashSeparated[0]);
                } catch (NumberFormatException e) {
                    return null;
                }
                if (!result.contains(a)) {
                    result.add(a);
                }
            } else if (dashSeparated.length == 2) {
                Integer a, b;
                try {
                    a = Integer.parseInt(dashSeparated[0]);
                    b = Integer.parseInt(dashSeparated[1]);
                } catch (NumberFormatException e) {
                    return null;
                }
                for (int i = a; i <= b; i++) {
                    if (!result.contains(i)) {
                        result.add(i);
                    }
                }
            } else {
                return null;
            }
        }
        return result;
    }

    private static void usage() {
        System.err.println("Usage: Client <host>:<port> <rCount> <wCount> <value> <idList>");
    }

    private static <T> T tryPoll(ExecutorCompletionService<T> service) throws ExecutionException, InterruptedException {
        Future<T> result = service.poll();
        if (result == null) {
            return null;
        }

        try {
            return result.get();
        } catch (ExecutionException e) {
            System.err.println("Remote exception: " + e.getCause());
            getService.shutdownNow();
            setService.shutdownNow();
            throw e;
        } catch (InterruptedException e) {
            System.err.println("The thread was interrupted");
            getService.shutdownNow();
            setService.shutdownNow();
            throw e;
        }
    }

    public static void main(String[] args) throws RemoteException {
        AccountService service;
        final String host;
        final int port;
        final int rCount;
        final int wCount;
        final long delta;
        final List<Integer> idList;

        if (args.length != 6 || args[0] == null || args[1] == null || args[2] == null || args[3] == null
                || args[4] == null || args[5] == null) {
            usage();
            return;
        }

        idList = parseIntervals(args[5]);
        if (idList == null) {
            usage();
            return;
        }
        try {
            host = args[0];
            port = Integer.parseInt(args[1]);
            rCount = Integer.parseInt(args[2]);
            wCount = Integer.parseInt(args[3]);
            delta = Integer.parseInt(args[4]);
        } catch (NumberFormatException e) {
            usage();
            return;
        }

        try {
            service = (AccountService) Naming.lookup("rmi://" + host + ":" + port + "/accountservice");
        } catch (NotBoundException e) {
            System.err.println("Account service is not bound");
            return;
        } catch (MalformedURLException e) {
            System.err.println("Account service URL is invalid");
            return;
        }

        getService = Executors.newFixedThreadPool(rCount);
        setService = Executors.newFixedThreadPool(wCount);
        final ExecutorCompletionService<Map.Entry<Integer, Long>> getResults = new ExecutorCompletionService<>(getService);
        final ExecutorCompletionService<Integer> setResults = new ExecutorCompletionService<>(setService);
        Map.Entry<Integer, Long> getResult;
        Integer setResult;
        int size = idList.size();
        Random random = new Random();

        for (; ; ) {
            getResults.submit(() -> {
                int id = idList.get(random.nextInt(size));
                return new AbstractMap.SimpleEntry<>(id, service.getAmount(id));
            });
            try {
                while ((getResult = tryPoll(getResults)) != null) {
                    System.out.println("Reader: value by id " + getResult.getKey() + " is " + getResult.getValue());
                }
            } catch (Exception e) {
                break;
            }
            setResults.submit(() -> {
                int id = idList.get(random.nextInt(size));
                service.addAmount(id, delta);
                return null;
            });
            try {
                while ((setResult = tryPoll(setResults)) != null) {
                    System.out.println("Writer: value by id " + setResult + " is added by " + delta);
                }
            } catch (Exception e) {
                break;
            }
        }
    }
}