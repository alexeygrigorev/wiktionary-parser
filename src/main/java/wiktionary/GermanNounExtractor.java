package wiktionary;

import java.io.IOException;

public class GermanNounExtractor {
    public static void main(String[] args) throws IOException {
        DumpReader reader = new DumpReader("data/dewiktionary-latest-pages-articles.xml.bz2");

        while (reader.hasNext()) {
            Page page = reader.next();
            System.out.println(page.getTitle());
            System.out.println(page.getContent());
            System.out.println();
        }

        reader.close();
    }
}
