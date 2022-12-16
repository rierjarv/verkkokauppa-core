package fi.hel.verkkokauppa.common.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class FileUtils {

    public static String readFileAsString(String filePath, ClassLoader classLoader) throws IOException {
        InputStream inputStream = classLoader.getResourceAsStream(filePath);

        StringBuilder sb = new StringBuilder();
        try (Reader reader = new BufferedReader(new InputStreamReader
                (inputStream, Charset.forName(StandardCharsets.UTF_8.name())))) {
            int c = 0;
            while ((c = reader.read()) != -1) {
                sb.append((char) c);
            }
        }
        return sb.toString();
    }
}
