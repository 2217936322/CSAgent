package com.fuckme;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Translator {
    private static Translator translator = null;
    private static Map<String, String> map = new HashMap<>();
    private static Map<Pattern, String> regex = new HashMap<>();

    public Translator() {
        try {
            Path p = Paths.get("resources/translation.txt");
            // System.out.println(p.toAbsolutePath());
            List<String> lines = Files.readAllLines(p, StandardCharsets.UTF_8);
            for (String line : lines) {
                if (line.startsWith("#")) continue;
                String[] inputs = line.split("\t+", 2);
                if (inputs.length == 2) {
                    if (inputs[0].startsWith("[regex] ")) {
                        Pattern pattern = Pattern.compile("(?m)" + inputs[0].substring(8).replace("_LF_", "\n").replace("_TAB_", "\t"));
                        regex.put(pattern, inputs[1].replace("_LF_", "\n").replace("_TAB_", "\t"));
                    } else {
                        map.put(inputs[0], inputs[1].replace("_LF_", "\n").replace("_TAB_", "\t"));
                    }
                }
            }
        } catch (Exception ex) {
            System.out.printf("[CSAgent] Translator fucked up: %s\n", ex);
        }
    }

    public static String translate(String str) {
        if (translator == null) {
            translator = new Translator();
        }
        if (str == null || str.length() == 0) {
            return str;
        }
        String result = map.get(str.replace("\n", "_LF_").replace("\t", "_TAB_"));
        return result == null ? str : result;
    }

    public static String regexTranslate(String str) {
        String trans = translate(str);
        if (!trans.equals(str)) return trans;

        for (Pattern pattern : regex.keySet()) {
            Matcher matcher = pattern.matcher(str);
            if (matcher.find()) {
                String result = regex.get(pattern);
                for (int i = 0; i <= matcher.groupCount(); i++) {
                    // System.out.printf("matcher.groupCount(%d): %s\n", i, matcher.group(i));
                    result = result.replace(String.format("${%d}", i), matcher.group(i));
                }
                return result;
            }
        }
        return str;
    }
}
