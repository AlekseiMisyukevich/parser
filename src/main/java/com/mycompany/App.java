package com.mycompany;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.HashSet;
import java.util.Set;

public class App {

    private final static Logger logger = LogManager.getLogger(App.class);
    private HashSet<String> volumes;
    private Path pathToVolumes;
    private HashSet <String> fileNames;

    private String path = "/root/Documents/books";
    private final String booksURL = "https://einsteinpapers.press.princeton.edu/papers/";

    public App() {

        volumes = new HashSet<String>();
        this.fileNames = new HashSet<String>();
        createDir();

    }

    public static void main(String[] args) {

        App app = new App();

        try {

            app.getVolumeLinks(app.booksURL);
            app.parse();
            logger.info("Completed");

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void getVolumeLinks(String url) throws IOException {

        Document doc = Jsoup.connect(url).get();
        Elements links = doc.getElementsByAttributeValueContaining("href", "vol");

        for (Element link : links) {
            volumes.add(link.attr("abs:href"));
        }

    }

    private void parse () {

        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
        final FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(permissions);

        volumes.stream()
                .forEach(t -> {
                    try {

                        Document doc = Jsoup.connect(t).get();
                        Elements title = doc.getElementsByClass("title-toc");
                        Elements pages = doc.getElementsByAttributeValueContaining("href", "vol");

                        Path volumeFolder = Files.createDirectory(Paths.get(this.pathToVolumes.toString(), title.text()), attrs);

                        pages.stream().map( p ->
                                p.child(0).attr("class", "t-toc-title").text()
                        ).forEach( name -> this.fileNames.add(name));

                        pages.stream()
                                .map( e -> e.attr("href") )
                                .forEach( l -> {
                                    try {

                                        Document page = Jsoup.connect(l).get();

                                        Element img = page.getElementById("t-page-image");
                                        String imgUrl = img.absUrl("src");
                                        String filename = l.substring(l.lastIndexOf("/"), l.length());

                                        URL url = new URL(imgUrl);
                                        InputStream in = url.openStream();
                                        Path file = Paths.get(volumeFolder.toString(), filename);
                                        OutputStream out = Files.newOutputStream(file);

                                        int b;
                                        while ( (b = in.read()) != -1 ) {
                                            out.write(b);
                                        }

                                        out.close();
                                        in.close();

                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                });

                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    private void createDir() {

        final Set<PosixFilePermission> permissions = PosixFilePermissions.fromString("rwx------");
        final FileAttribute<Set<PosixFilePermission>> attrs = PosixFilePermissions.asFileAttribute(permissions);

        try {

            Path storage = Paths.get(path);


            if (!Files.exists(storage, LinkOption.NOFOLLOW_LINKS)) {
                pathToVolumes = Files.createDirectories(storage, attrs);
            } else {
                pathToVolumes = storage;
            }
        }

        catch (IOException | InvalidPathException e) {
            throw new RuntimeException(e);
        }

    }


}
