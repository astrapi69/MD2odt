package cz.zcu.kiv.md2odt;

import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author Patrik Harag
 * @version 2017-04-29
 */
public class MD2odtTest {
    @Test
//    @Ignore
    public void test() throws IOException {
        InputStream md = System.class.getResourceAsStream(EXAMPLE_TXT);
        Path out = Paths.get(OUTPUT);

        MD2odt.converter().setInput(md, CHARSET).setOutput(out).enableAllExtensions().convert();
    }

    @Test
    @Ignore
    public void testWithTemplate() throws IOException {
        InputStream md = System.class.getResourceAsStream(EXAMPLE_TXT);
        InputStream template = System.class.getResourceAsStream(TEMPLATE);
        Path out = Paths.get(OUTPUT);

        MD2odt.converter().setInput(md, CHARSET).setTemplate(template).setOutput(out).enableAllExtensions().convert();
    }

    @Test
    @Ignore
    public void testFromZip() throws IOException {
        InputStream zip = System.class.getResourceAsStream(EXAMPLE_ZIP);
        Path out = Paths.get(OUTPUT);

        MD2odt.converter().setInputZip(zip, CHARSET).setOutput(out).enableAllExtensions().convert();
    }

    private static final Charset CHARSET = StandardCharsets.UTF_8;
    private static final String EXAMPLE_TXT = "/example.md";
    private static final String EXAMPLE_ZIP = "/example.zip";
    private static final String TEMPLATE = "/template.odt";
    private static final String OUTPUT = "result.odt";
}
