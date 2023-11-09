package server.app;

import server.contract.IUserCenterRpcService;
import server.contract.UserInfo;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class ServerApp implements IUserCenterRpcService {

    /**
     * 用timer模拟一个对db 或 其他耗时的io请求吧
     */
    private ScheduledExecutorService timer = new ScheduledThreadPoolExecutor(1);


    @Override
    public CompletableFuture<UserInfo> queryUserinfo(String requestBody) {

        CompletableFuture<UserInfo> future = new CompletableFuture<>();

        // 假设下游接口耗时是 1秒
        timer.schedule(new Runnable() {
            @Override
            public void run() {
                UserInfo userInfo = new UserInfo();
                userInfo.setUserName("DemoDemoDemo " + requestBody);
                future.complete(userInfo);
            }
        },1, TimeUnit.SECONDS);


        return future;
    }


}
