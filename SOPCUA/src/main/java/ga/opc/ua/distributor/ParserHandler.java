package ga.opc.ua.distributor;

import ga.opc.ua.distributor.models.ClientConfig;
import ga.opc.ua.distributor.models.Config;
import ga.opc.ua.distributor.models.ServerConfig;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.util.ArrayList;
import java.util.List;

public class ParserHandler extends DefaultHandler {
    /**Набор тегов*/
    private static final String TAG_MAIN_SERVER = "server_configure";
    private static final String TAG_SERVER_IP = "ip";
    private static final String TAG_SERVER_PORT = "port";

    private static final String TAG_MAIN_CLIENT = "client_configure";
    private static final String TAG_CLIENT_IP = "ip";
    private static final String TAG_CLIENT_PORT = "port";
    private static final String TAG_CLIENT_QUERY = "query_time_sec";
    private static final String TAG_CLIENT_DIR = "directory_with_file";

    private final Config config = new Config();

    private String currentTagName;

    private final List<ServerConfig> serverConfigList = new ArrayList<>();
    private final List<ClientConfig> clientConfigList = new ArrayList<>();

    private boolean isServerConfig = false;
    private boolean isClientConfig = false;

    private String ipServer, portServer;
    private String ipClient, portClient, directory;
    private int queryClient;

    public Config getConfig() {
        return config;
    }


    @Override
    public void startDocument() throws SAXException {}

    @Override
    public void endDocument() throws SAXException {
        config.setServerConfigList(serverConfigList);
        config.setClientConfigList(clientConfigList);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
        currentTagName = qName;
        switch (currentTagName){
            case TAG_MAIN_SERVER -> isServerConfig = true;
            case TAG_MAIN_CLIENT -> isClientConfig = true;
        }
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        if (qName.equals(TAG_MAIN_SERVER)){
            isServerConfig = false;
            ServerConfig serverConfig = new ServerConfig(ipServer, portServer);
            serverConfigList.add(serverConfig);
        }else if (qName.equals(TAG_MAIN_CLIENT)){
            isClientConfig = false;
            ClientConfig clientConfig = new ClientConfig(ipClient, portClient, queryClient, directory);
            clientConfigList.add(clientConfig);
        }

    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        if(currentTagName == null){
            return;
        }

        if(isServerConfig){
            switch (currentTagName){
                case TAG_SERVER_IP -> ipServer = new String(ch, start, length);
                case TAG_SERVER_PORT -> portServer = new String(ch, start, length);
            }
        }else if (isClientConfig){
            switch (currentTagName){
                case TAG_CLIENT_IP -> ipClient = new String(ch, start, length);
                case TAG_CLIENT_PORT -> portClient = new String(ch, start, length);
                case TAG_CLIENT_QUERY -> queryClient = Integer.parseInt(new String(ch, start, length));
                case TAG_CLIENT_DIR -> directory = new String(ch, start, length);
            }
        }
    }
}
