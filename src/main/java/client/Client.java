package client;

import client.app.BizBizXXX;
import client.app.ClientApp;
import client.io.ClientIO;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class Client {


    private static ClientApp clientApp;

    public static void main(String[] args) throws InterruptedException {

        ClientIO clientIO = new ClientIO();
        clientIO.startAndConnect();

        clientApp = new ClientApp(clientIO);



        while (true) {

            List<CompletableFuture<BizBizXXX>> waitAllFuture = new ArrayList<>();

            for (int i = 0; i < 300; i++) {
                CompletableFuture<BizBizXXX> future = clientApp.queryUserInfoAndXXX(String.valueOf(System.currentTimeMillis()));
                waitAllFuture.add(future);
            }

            for (CompletableFuture<BizBizXXX> future : waitAllFuture) {
                try {
                    future.get();
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                //ignore
            }

            System.out.println("300");

        }

    }


}
