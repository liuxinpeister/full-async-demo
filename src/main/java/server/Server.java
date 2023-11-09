package server;

import server.app.ServerApp;
import server.contract.IUserCenterRpcService;
import server.io.ServerIO;

public class Server {


    public static void main(String[] args) {
        IUserCenterRpcService rpcService = new ServerApp();
        ServerIO io = new ServerIO();
        io.startAndWait(rpcService);
    }

}
