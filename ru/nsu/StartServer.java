package ru.nsu;

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
        RaftConfig config = new RaftConfig(size);
        RaftLog log = new RaftLog();
        int lastApplied = log.getLastIndex();
        RaftResponses.init(config.getServersNumber(), log.getLastTerm());

        RaftMode.initializeServer(config,
                log,
                lastApplied,
                rank);
        RaftServerImpl server = new RaftServerImpl(rank);
        RaftServerImpl.setMode(new FollowerMode());

    }
}


