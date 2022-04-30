package ga.opc.ua.distributor.models;

import java.util.List;

public class Config {
    private List<ClientConfig> clientConfigList;
    private List<ServerConfig> serverConfigList;

    public List<ClientConfig> getClientConfigList() {
        return clientConfigList;
    }

    public void setClientConfigList(List<ClientConfig> clientConfigList) {
        this.clientConfigList = clientConfigList;
    }

    public List<ServerConfig> getServerConfigList() {
        return serverConfigList;
    }

    public void setServerConfigList(List<ServerConfig> serverConfigList) {
        this.serverConfigList = serverConfigList;
    }

    @Override
    public String toString() {
        return "Config{" +
                "clientConfigList=" + clientConfigList +
                ", serverConfigList=" + serverConfigList +
                '}';
    }
}
