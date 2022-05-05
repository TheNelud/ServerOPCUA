package ga.opc.ua.client;

import com.google.common.collect.ImmutableList;
import ga.opc.ua.distributor.Distributor;
import ga.opc.ua.distributor.Seeker;
import ga.opc.ua.distributor.models.ClientConfig;
import ga.opc.ua.distributor.models.Config;
import ga.opc.ua.distributor.models.ServerConfig;
import ga.opc.ua.server.ExampleServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.MarkerManager;
import org.eclipse.milo.opcua.sdk.client.OpcUaClient;
import org.eclipse.milo.opcua.stack.core.types.builtin.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class WriteFromText implements ClientWriter{
    private final Distributor distributor = new Distributor();
    private final int query = distributor.getQuery();
    private final String path = distributor.getPath();

    private final Logger logger = LoggerFactory.getLogger(getClass());
    public static final org.apache.logging.log4j.Logger LOGGER = LogManager.getLogger(ExampleServer.class);
    public static final Marker marker_err = MarkerManager.getMarker("Crit_server");
    public static final Marker marker_inp = MarkerManager.getMarker("Input_server");


    @Override
    public void run(OpcUaClient client, CompletableFuture<OpcUaClient> future) {
        try {
            /**Создаем подключение к серверу*/
            client.connect().get();

            LOGGER.info(marker_err,"Server is working...");

            /**В бесконечном цикле мы должны:
             * 1. с помощь метода seeker.runToDirectory() мы возвращаем лист с абсолютным путем где находится тукстовый файл
             * 2. даллее парсим текстовый файл и полученные данные отправляем на сервер опс
             * 3. после того как файл был полностью прочитан, переименовываем его, и ждем новый тестовый файл.*/
            while (true) {
                LocalDateTime nowDate = LocalDateTime.now();

                File currentDirFile = new File(".");
                String absolutePath = currentDirFile.getAbsolutePath();

                Seeker seeker = new Seeker();

                SimpleDateFormat formatter1 = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
                List<String> list = Files.readAllLines(Paths.get(seeker.runToDirectory("Client", absolutePath).get(0)));

                System.out.println(list);

                boolean status_all_tags=true;
                for (String str : list) {
                    String[] preInd = str.split(",");
                    if (preInd.length == 3) {
                        List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, "TagS/ScalarTypes/" + preInd[0]));
                        Variant v;
                        try {
                            v = new Variant(Double.parseDouble(preInd[2]));
                        } catch (Exception e){
                            v = new Variant(0);
                            logger.info("error value'{}' to nodeId={}", v, nodeIds.get(0));
                            continue;
                        }
                        Date date = formatter1.parse(preInd[1]);
                        // don't write status or timestamps
                        DataValue dv;
                        dv = new DataValue(v, null, new DateTime(date));

                        // write asynchronously....
                        CompletableFuture<List<StatusCode>> f =
                                client.writeValues(nodeIds, ImmutableList.of(dv));

                        // ...but block for the results so we write in order
                        List<StatusCode> statusCodes = f.get();
                        StatusCode status = statusCodes.get(0);

                        if (status.isBad()) {
                            status_all_tags=false;
                            logger.info("not wrote tag '{}' to nodeId={}", v, nodeIds.get(0));
                        }
                    } else if (preInd.length == 4) {
                        List<NodeId> nodeIds = ImmutableList.of(new NodeId(2, "TagS/ScalarTypes/" + preInd[0]));
                        Variant v;
                        try {
                            v= new Variant((short) Integer.parseInt(preInd[3]));
                        } catch (Exception e){
                            v= new Variant((short) 0);
                            logger.info("error value'{}' to nodeId={}", v, nodeIds.get(0));
                            continue;
                        }

                        Date date = formatter1.parse(preInd[1]);
                        // don't write status or timestamps
                        DataValue dv;
                        dv = new DataValue(v, null, new DateTime(date));

                        // write asynchronously....
                        CompletableFuture<List<StatusCode>> f =
                                client.writeValues(nodeIds, ImmutableList.of(dv));

                        // ...but block for the results so we write in order
                        List<StatusCode> statusCodes = f.get();
                        StatusCode status = statusCodes.get(0);

                        if (status.isBad()) {
                            status_all_tags=false;
                            logger.info("not wrote tag '{}' to nodeId={}", v, nodeIds.get(0));
                        }
                    }


                }
                if (status_all_tags){
                    logger.info("Successful reading ALL tags cycle!   "+nowDate);
                }


                /**Переименовываем файл*/
                Path source = Paths.get(seeker.runToDirectory("Client", absolutePath).get(0));
                String rewriteNameFile = source.toString().substring(source.toString().lastIndexOf("\\") + 1) + "_";
                Files.move(source, source.resolveSibling(rewriteNameFile));

                if (seeker.runToDirectory("After file", absolutePath).get(0).isEmpty()){
                    LOGGER.info(marker_err, "Is directory not txt file");
                }


//                String CopyFilePath =source.toString().substring(0,source.toString().lastIndexOf("\\")+1)+nowDate.toString().replaceAll("[:.]","^")+".txt";
//                File prevFile = new File(seeker.runToDirectory("Client", absolutePath).get(0));
//
//                if (afterFiles.isEmpty()){
//                    File newFile = new File(CopyFilePath);
//                    Files.copy(source.resolveSibling(rewriteNameFile), newFile.toPath());
//                    LOGGER.info(marker_inp,"Отсутствуют .txt файлы в каталоге: "+path+" создание дубликата последнего найденного txt файла");
//                    logger.info("Отсутствуют .txt файлы в каталоге: "+path+" создание дубликата последнего найденного txt файла");
//                }

//                Thread.sleep(query* 1000L);
            }
            //future.complete(client);
        }
        catch (Exception e){
            logger.info(marker_err + "Undeclared error, read log, fix and restart server" );
//            LOGGER.info(marker_err, ExceptionUtils.getStackTrace(e)+"\n Undeclared error, read log, fix and restart server");
        }

    }
}
