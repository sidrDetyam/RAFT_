package ru.nsu;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import ru.nsu.raftstate.statemachine.command.TestCommand;
import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;
import ru.nsu.rpc.dto.AppendRequestDto;
import ru.nsu.rpc.dto.client.ClientRequest;

public class StartClient {
    public static void main(String[] args) throws RpcException {
        System.out.println(RaftRpcClientImpl.clientRequest(1, new ClientRequest(new TestCommand(14))));
    }
}
