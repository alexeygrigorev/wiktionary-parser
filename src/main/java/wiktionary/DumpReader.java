package wiktionary;

import com.google.common.collect.AbstractIterator;
import com.google.common.io.Closer;
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;
import org.apache.commons.lang3.text.translate.AggregateTranslator;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.EntityArrays;
import org.apache.commons.lang3.text.translate.LookupTranslator;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DumpReader extends AbstractIterator<Page> implements AutoCloseable {

    private static final Pattern TITLE_PATTERN = Pattern.compile("(?:<title>)(.*?)(?:</title>)");
    private static final Pattern NAMESPACE_PATTERN = Pattern.compile("(?:<ns>)(.*?)(?:</ns>)");
    private static final Pattern TEXT_PATTERN = Pattern.compile("(?:<text.*?>)(.*?)(?:</text>)", Pattern.DOTALL);

    private static final CharSequenceTranslator TRANSLATOR = new AggregateTranslator(
            new LookupTranslator(EntityArrays.ISO8859_1_UNESCAPE()),
            new LookupTranslator(EntityArrays.BASIC_UNESCAPE()),
            new LookupTranslator(EntityArrays.HTML40_EXTENDED_UNESCAPE()));

    private final Closer closer;
    private final SplittingIterator iterator;

    public DumpReader(String path) throws IOException {
        closer = Closer.create();

        InputStream in = closer.register(new FileInputStream(new File(path)));
        BufferedInputStream bin = closer.register(new BufferedInputStream(in));
        BZip2CompressorInputStream bz = closer.register(new BZip2CompressorInputStream(bin));
        InputStreamReader reader = closer.register(new InputStreamReader(bz, StandardCharsets.UTF_8));

        iterator = new SplittingIterator(reader, "</page>");
    }

    @Override
    protected Page computeNext() {
        while (iterator.hasNext()) {
            String next = iterator.next();
            Optional<Page> page = extractText(next);
            if (page.isPresent()) {
                return page.get();
            }
        }

        return endOfData();
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

    @Override
    public void close() throws IOException {
        closer.close();
    }
}
