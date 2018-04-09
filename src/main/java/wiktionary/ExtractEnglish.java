package wiktionary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class ExtractEnglish {

    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream(new File(
                "/home/agrigorev/tmp/wikitionary-extract/" + "data/enwiktionary-latest-pages-articles.xml.bz2"));
        BZip2CompressorInputStream input = new BZip2CompressorInputStream(new BufferedInputStream(in));
        InputStreamReader reader = new InputStreamReader(input, "UTF-8");

        SplittingIterator it = new SplittingIterator(reader, "</page>");

        PrintWriter pw = new PrintWriter(new File("results-en.txt"));

        while (it.hasNext()) {
            Optional<Page> optionalPage = extractText(it.next());
            if (!optionalPage.isPresent()) {
                continue;
            }

            Page page = optionalPage.get();
            if (isGoodVerb(page)) {
                System.out.println(page.title.toLowerCase());
                pw.println(page.title.toLowerCase());
            }
        }

        it.close();
        pw.close();
    }

    private static boolean isGoodVerb(Page page) {
        String content = page.content.toLowerCase();
        Multimap<String, String> titles = extractTitles(content);

        if (!titles.containsKey("english")) {
            return false;
        }

        Collection<String> english = titles.get("english");
        if (english.contains("noun")) {
            return false;
        }

        if (!english.contains("verb")) {
            return false;
        }

        String title = page.title.toLowerCase();
        if (title.length() <= 3 || title.length() > 15) {
            return false;
        }

        int noTokens = title.split("\\s+").length;
        if (noTokens > 1) {
            return false;
        }

        return true;
    }

    private static final Pattern TITLE_PATTERN = Pattern.compile("(?:<title>)(.*?)(?:</title>)");
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("(?:<ns>)(.*?)(?:</ns>)");
    private static final Pattern TEXT_PATTERN = Pattern.compile("(?:<text.*?>)(.*?)(?:</text>)", Pattern.DOTALL);

    private static final CharSequenceTranslator TRANSLATOR = new AggregateTranslator(
            new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
            new LookupTranslator(EntityArrays.BASIC_UNESCAPE()),
            new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()));

    static class Page {
        private final String title;
        private final String content;

        public Page(String title, String content) {
            this.title = title;
            this.content = content;
        }
    }

    private static Optional<Page> extractText(String content) {
        Matcher titleMatcher = TITLE_PATTERN.matcher(content);
        if (!titleMatcher.find()) {
            return Optional.empty();
        }

        String title = titleMatcher.group(1);

        Matcher namespaceMatcher = NAMESPACE_PATTERN.matcher(content);
        if (!namespaceMatcher.find()) {
            return Optional.empty();
        }

        int ns = Integer.parseInt(namespaceMatcher.group(1));
        if (ns != 0) {
            return Optional.empty();
        }

        // parse text
        Matcher textMatcher = TEXT_PATTERN.matcher(content);
        if (!textMatcher.find()) {
            return Optional.empty();
        }

        String rawText = textMatcher.group(1);
        String text = TRANSLATOR.translate(rawText);

        return Optional.of(new Page(title, text));
    }

    private static final Pattern TITLE_H2 = Pattern.compile("==(.+)==");
    private static final Pattern TITLE_H3 = Pattern.compile("===(.+)===");

    private static Multimap<String, String> extractTitles(String content) {
        Multimap<String, String> res = HashMultimap.create();

        String current = null;
        for (String line : content.split("\n")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }

            Matcher h3 = TITLE_H3.matcher(line);
            if (h3.find()) {
                String title = h3.group(1).trim();
                if (current == null) {
                    continue;
                }

                if (title.startsWith("=")) {
                    continue;
                }

                res.put(current, title);
                continue;
            }

            Matcher h2 = TITLE_H2.matcher(line);
            if (h2.find()) {
                String title = h2.group(1).trim();
                if (title.startsWith("=")) {
                    continue;
                }

                current = title;
                continue;
            }

        }

        return res;
    }
}
