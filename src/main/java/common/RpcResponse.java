package common;

public class RpcResponse {

    private String service;

    private String method;

    private String result;

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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    @Override
    public String toString() {
        return "RpcResponse{" +
                "service='" + service + '\'' +
                ", method='" + method + '\'' +
                ", result='" + result + '\'' +
                '}';
    }
}
