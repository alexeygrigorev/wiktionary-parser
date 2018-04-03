package wiktionary;

import java.io.IOException;
import java.io.Reader;

import com.google.common.collect.AbstractIterator;

public class SplittingIterator extends AbstractIterator<String> implements AutoCloseable {

    private final Reader reader;
    private final String separator;

    private String prevChunk = "";
    private final char[] buffer = new char[8 * 1024];

    public SplittingIterator(Reader reader, String separator) {
        this.reader = reader;
        this.separator = separator;
    }

    @Override
    protected String computeNext() {
        try {
            return doNext();
        } catch (IOException e) {
            throw new RuntimeException();
        }
    }

    private String doNext() throws IOException {
        int separatorPosition = prevChunk.indexOf(separator);
        if (separatorPosition >= 0) {
            String result = prevChunk.substring(0, separatorPosition);
            prevChunk = prevChunk.substring(separatorPosition + separator.length(), prevChunk.length());
            return result;
        }

        StringBuilder builder = new StringBuilder(100);
        builder.append(prevChunk);

        int numCharsRead;
        while ((numCharsRead = reader.read(buffer, 0, buffer.length)) != -1) {
            builder.append(buffer, 0, numCharsRead);

            separatorPosition = builder.indexOf(separator);
            if (separatorPosition >= 0) {
                String result = builder.substring(0, separatorPosition);
                prevChunk = builder.substring(separatorPosition + separator.length());
                return result;
            }
        }

        if (builder.length() > 0) {
            prevChunk = "";
            return builder.toString();
        }

        if (prevChunk.length() > 0) {
            String result = prevChunk;
            prevChunk = "";
            return result;
        }

        return endOfData();
    }

    @Override
    public void close() throws Exception {
        reader.close();
    }

}
