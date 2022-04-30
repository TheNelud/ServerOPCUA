package ga.opc.ua.distributor.models;

public class ClientConfig {
    private String ip;
    private String port;
    private int queryTimeSec;
    private String path;

    public ClientConfig(String ip, String port, int queryTimeSec, String path) {
        this.ip = ip;
        this.port = port;
        this.queryTimeSec = queryTimeSec;
        this.path = path;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public int getQueryTimeSec() {
        return queryTimeSec;
    }

    public void setQueryTimeSec(int queryTimeSec) {
        this.queryTimeSec = queryTimeSec;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public String toString() {
        return "ClientConfig{" +
                "ip='" + ip + '\'' +
                ", port='" + port + '\'' +
                ", queryTimeSec=" + queryTimeSec +
                ", path='" + path + '\'' +
                '}';
    }
}
