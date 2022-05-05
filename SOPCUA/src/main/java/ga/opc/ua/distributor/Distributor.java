package ga.opc.ua.distributor;

import ga.opc.ua.distributor.models.ClientConfig;
import ga.opc.ua.distributor.models.Config;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Distributor {
    private String ip;
    private String port;
    private int query;
    private String path;


    public String getIp() {
        for (ClientConfig clientConfig : clientConfigList())
            ip =  clientConfig.getIp();
        return ip;
    }

    public String getPort() {
        for (ClientConfig clientConfig : clientConfigList())
            port =  clientConfig.getPort();
        return port;
    }

    public int getQuery() {
        for (ClientConfig clientConfig : clientConfigList())
            query = clientConfig.getQueryTimeSec();
        return query;
    }

    public String getPath() {
        for (ClientConfig clientConfig : clientConfigList())
            path =  clientConfig.getPath();
        return path;
    }

    private List<ClientConfig> clientConfigList(){
        Distributor distributor = new Distributor();
        Config config = distributor.parse();
        List<ClientConfig> configList = new ArrayList<>(config.getClientConfigList());
        return configList;
    }

    public Config parse(){
        SAXParserFactory factory = SAXParserFactory.newInstance();
        ParserHandler handler = new ParserHandler();

        SAXParser parser = null;
        try {
            parser = factory.newSAXParser();
        }catch (Exception e){
            System.out.println("Start parsing error" + e.toString());
            return null;
        }

        File file = new File("src/main/resources/config.xml");
        try {
            parser.parse(file, handler);
        }catch (SAXException | IOException e) {
            System.out.println("SAX PARSING ERROR :" + e.toString());
            return null;
        }

        return handler.getConfig();
    }
}
