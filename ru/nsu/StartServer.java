package ru.nsu;

import ru.nsu.raftstate.AbstractRaftState;
import ru.nsu.raftstate.FollowerState;
import ru.nsu.rpc.RpcServerImpl;

public class StartServer {

    public static void main(String[] args) throws InterruptedException {
        int rank = Integer.parseInt(args[1]);
        int size = Integer.parseInt(args[2]);
        AbstractRaftState.init(rank, size);
        AbstractRaftState.switchState(new FollowerState());
        new RpcServerImpl(rank);
    }
}
