package wiktionary;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;

public class ExtractGerman {

    public static void main(String[] args) throws Exception {
        InputStream in = new FileInputStream(
                new File("/home/agrigorev/tmp/wikitionary-extract/data/dewiktionary-latest-pages-articles.xml.bz2"));
        BZip2CompressorInputStream input = new BZip2CompressorInputStream(new BufferedInputStream(in));
        InputStreamReader reader = new InputStreamReader(input, "UTF-8");

        SplittingIterator it = new SplittingIterator(reader, "</page>");

        PrintWriter pw = new PrintWriter(new File("results-de.txt"));

        while (it.hasNext()) {
            Optional<Page> optionalPage = extractText(it.next());
            if (!optionalPage.isPresent()) {
                continue;
            }

            Page page = optionalPage.get();

            Collection<String> verbs = getVerbs(page);
            if (verbs.isEmpty()) {
                continue;
            }

            for (String verb : verbs) {
                pw.println(verb);
            }
        }

        it.close();
        pw.close();
    }

    private static final Pattern VERBS_PATTERN = Pattern.compile("\\{\\{deutsch\\s+verb\\s+übersicht(.*?)\\}\\}",
            Pattern.DOTALL);

    private static Collection<String> getVerbs(Page page) {
        String content = page.content.toLowerCase();

        if (!content.contains("{{deutsch verb")) {
            return Collections.emptyList();
        }

        Matcher matcher = VERBS_PATTERN.matcher(content);
        matcher.find();

        Set<String> result = new LinkedHashSet<>();
        result.add(page.title.toLowerCase());

        String template = matcher.group(1);
        for (String line : template.split("[|]")) {
            if (StringUtils.isBlank(line)) {
                continue;
            }

            line = line.trim();
            if (!line.contains("=")) {
                continue;
            }

            String[] split = line.split("=");
            if (split[0].contains("bild")) {
                continue;
            }

            if (split.length < 2) {
                continue;
            }

            String word = split[1].trim();
            if (word.matches("[\\[\\]<>/]")) {
                continue;
            }

            if (word.length() <= 2 || word.length() > 15) {
                continue;
            }

            if (word.split("\\s+").length > 1) {
                continue;
            }

            result.add(word);
        }

        return result;
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
}
