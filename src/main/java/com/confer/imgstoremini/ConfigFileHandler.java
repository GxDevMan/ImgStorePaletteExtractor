package com.confer.imgstoremini;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import java.io.File;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;

public class ConfigFileHandler {

    public void checkAndCreateConfigFile() {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";

        File configFile = new File(filePath);
        if (!configFile.exists()) {
            Map<String, String> defaultConfig = new HashMap<>();
            defaultConfig.put("default_db", "my_default_database.db");
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            try {
                writer.writeValue(configFile, defaultConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }


}
