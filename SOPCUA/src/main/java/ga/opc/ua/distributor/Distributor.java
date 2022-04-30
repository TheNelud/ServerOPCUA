package ga.opc.ua.distributor;

import ga.opc.ua.distributor.models.Config;
import org.xml.sax.SAXException;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.File;
import java.io.IOException;

public class Distributor {
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

        File file = new File("config.xml");
        try {
            parser.parse(file, handler);
        }catch (SAXException | IOException e) {
            System.out.println("SAX PARSING ERROR :" + e.toString());
            return null;
        }

        return handler.getConfig();
    }
}
