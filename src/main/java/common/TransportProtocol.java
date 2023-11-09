package common;

public class TransportProtocol {

    private long requestId;

    private int messageType;


    private String body;


    public long getRequestId() {
        return requestId;
    }

    public void setRequestId(long requestId) {
        this.requestId = requestId;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    @Override
    public String toString() {
        return "TransportProtocol{" +
                "requestId=" + requestId +
                ", messageType=" + messageType +
                ", body='" + body + '\'' +
                '}';
    }
}
