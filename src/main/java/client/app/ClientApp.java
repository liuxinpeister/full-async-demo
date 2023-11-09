package client.app;

import client.io.ClientIO;
import com.alibaba.fastjson.JSON;
import common.RpcRequest;
import common.RpcResponse;
import server.contract.IUserCenterRpcService;
import server.contract.UserInfo;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class ClientApp {

    private IUserCenterRpcService userCenterRpcService;

    private ClientIO clientIO;

    public ClientApp(ClientIO clientIO) {
        this.clientIO = clientIO;
        this.userCenterRpcService = new UserCenterRpcServiceProxy(clientIO);
    }

    /**
     * 模拟一个业务逻辑吧
     *
     * @param param
     * @return
     */
    public CompletableFuture<BizBizXXX> queryUserInfoAndXXX(String param) {
        return userCenterRpcService.queryUserinfo(param)
                .thenApply(new Function<UserInfo, BizBizXXX>() {
                    @Override
                    public BizBizXXX apply(UserInfo userInfo) {
                        BizBizXXX bizBizXXX = new BizBizXXX();

                        bizBizXXX.setUserInfo(userInfo);
                        // 假设再干点别的啥业务逻辑
                        bizBizXXX.setTime(System.currentTimeMillis());
                        return bizBizXXX;
                    }
                });
    }


    /**
     * 无论如何得需要一个代理类了
     * 自动生成就免了吧，简单起见嘛
     */
    public class UserCenterRpcServiceProxy implements IUserCenterRpcService {

        public UserCenterRpcServiceProxy(ClientIO clientIO) {
            this.clientIO = clientIO;
        }

        private ClientIO clientIO;

        @Override
        public CompletableFuture<UserInfo> queryUserinfo(String param) {
            RpcRequest request = new RpcRequest();
            request.setService("IUserCenterRpcService");
            request.setMethod("queryUserinfo");
            request.setParam(param);

            CompletableFuture<UserInfo> callFuture = new CompletableFuture<>();

            CompletableFuture<RpcResponse> ioFuture = clientIO.sendRpcRequest(request);

            ioFuture.whenComplete(new BiConsumer<RpcResponse, Throwable>() {
                @Override
                public void accept(RpcResponse rpcResponse, Throwable throwable) {

                    // 异常回写，继续忽略，因为服务端也没搞。

                    String result = rpcResponse.getResult();

                    // 直接JSON吧
                    UserInfo userInfo = JSON.parseObject(result, UserInfo.class);
                    callFuture.complete(userInfo);
                }
            });

            return callFuture;
        }
    }

}
