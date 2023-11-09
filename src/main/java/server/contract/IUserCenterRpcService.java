package server.contract;

import java.util.concurrent.CompletableFuture;

public interface IUserCenterRpcService {

    CompletableFuture<UserInfo> queryUserinfo(String requestBody);

}
