package wiktionary;

import static org.junit.Assert.assertEquals;

import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;

public class SplittingIteratorTest {

    @Test
    public void test() throws Exception {
        String s = "123 <split> 12312 <split> asfsf <split> 111";
        Reader reader = new StringReader(s);
        SplittingIterator iterator = new SplittingIterator(reader, "<split>");

        List<String> results = Lists.newArrayList(iterator);
        iterator.close();

        assertEquals(Arrays.asList(s.split("<split>")), results);

    }

}
