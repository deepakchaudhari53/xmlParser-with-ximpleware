package com.deep.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class ResourceUtil {

    public static String loadResourceAsString(String resourcePath) throws Exception {
        InputStreamReader ir = new InputStreamReader(ResourceUtil.class.getResourceAsStream(resourcePath));
        BufferedReader reader = new BufferedReader(ir);
        String line;
        StringBuilder sb = new StringBuilder();
        while ((line = reader.readLine()) != null) {
            sb.append(line.trim());
            sb.append("\n");
        }
        reader.close();
        return sb.toString();
    }

}
