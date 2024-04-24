package ru.nsu;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Arrays;

import ru.nsu.rpc.RaftServerImpl;

public class StartServer {

    public static void main(String[] args) {

        Arrays.stream(args)
                .forEach(System.out::println);

        int port = Integer.parseInt(args[0]);
        int rank = Integer.parseInt(args[1]);
        int size = Integer.parseInt(args[2]);

        String url = "rmi://localhost:" + port + "/S" + rank;
        RaftConfig config = new RaftConfig(size, -1);
        RaftLog log = new RaftLog();
        int lastApplied = log.getLastIndex();
        RaftResponses.init(config.getNumServers(), log.getLastTerm());

        RaftMode.initializeServer(config,
                log,
                lastApplied,
                port,
                rank);
        RaftServerImpl server = new RaftServerImpl(rank);
        RaftServerImpl.setMode(new FollowerMode());

    }
}


