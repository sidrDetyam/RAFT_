package ru.nsu;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import ru.nsu.rpc.RaftRpcClientImpl;
import ru.nsu.rpc.RpcException;

public class StartClient {
  public static void main (String[] args) throws RpcException {
    RaftRpcClientImpl.requestVote(1, 0, 0, 0, 0);
  }
}
