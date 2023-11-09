package common;

public class RpcRequest {

    private String service;

    private String method;

    private String param;


    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public String getParam() {
        return param;
    }

    public void setParam(String param) {
        this.param = param;
    }

    @Override
    public String toString() {
        return "RpcRequest{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", param='" + param + '\'' +
                '}';
    }
}
