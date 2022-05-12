package ga.opc.ua.distributor;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Seeker {



    public List<String> runToDirectory(String whereis, String path){
        List<String> listTXTFiles = new ArrayList<>();
        try {
            Files.walkFileTree(Paths.get(path),new HashSet<FileVisitOption>(),1,new SimpleFileVisitor<Path>(){
                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    System.err.println(whereis + " "+ file);
                    if (file.toString().endsWith(".txt")) {
                        listTXTFiles.add(file.toString());
                        System.out.println("Detect file -> "+file);
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                    if (exc == null) {

                        return FileVisitResult.CONTINUE;
                    } else {
                        // directory iteration failed
                        throw exc;
                    }

                }
            });
        } catch (IOException e) {
            //TODO Доработать : действия при ошибке чтения файла
            e.printStackTrace();
            try {
                Thread.sleep(10000);
                runToDirectory(whereis, path);
            } catch (InterruptedException ex) {
                throw new RuntimeException(ex);
            }

        }
        return listTXTFiles;
    }

}
