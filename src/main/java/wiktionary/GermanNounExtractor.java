package wiktionary;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GermanNounExtractor {
    public static void main(String[] args) throws IOException {
        DumpReader reader = new DumpReader("data/dewiktionary-latest-pages-articles.xml.bz2");

        Pattern genusPattern = Pattern.compile("\\|genus=(\\w+)");
        //Pattern singularPattern = Pattern.compile("\\|nominativ singular=(\\w+)");

        PrintWriter pw = new PrintWriter("results.txt");

        while (reader.hasNext()) {
            Page page = reader.next();

            String content = page.getContent().toLowerCase();
            if (!content.contains("{{deutsch substantiv")) {
                continue;
            }

            Matcher genusMatcher = genusPattern.matcher(content);
            if (!genusMatcher.find()) {
                continue;
            }

            String genus = genusMatcher.group(1);
            String artikel = toArtikel(genus);

            if (artikel == null) {
                continue;
            }

            System.out.println(artikel + " " + page.getTitle());
            pw.println(artikel + "\t" + page.getTitle());
        }

        pw.flush();
        pw.close();

        reader.close();
    }

    private static String toArtikel(String genus) {
        if ("m".equals(genus)) {
            return "der";
        } else if ("f".equals(genus)) {
            return "die";
        } else if ("n".equals(genus)) {
            return "das";
        }

        return null;
    }
}
