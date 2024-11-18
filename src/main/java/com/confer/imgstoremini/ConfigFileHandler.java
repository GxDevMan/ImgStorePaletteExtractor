package com.confer.imgstoremini;
import com.fasterxml.jackson.databind.JsonNode;
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
            defaultConfig.put("default_db", "default_imageStore.db");
            ObjectMapper mapper = new ObjectMapper();
            ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
            try {
                writer.writeValue(configFile, defaultConfig);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void checkAndCreateConfigFile(String dbName) {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";

        File configFile = new File(filePath);

        Map<String, String> defaultConfig = new HashMap<>();
        defaultConfig.put("default_db", "default_imageStore.db");
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        try {
            writer.writeValue(configFile, defaultConfig);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public Map<String, String> getConfig() {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";
        File configFile = new File(filePath);

        Map<String, String> configData = new HashMap<>();

        if (configFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            try {
                JsonNode rootNode = mapper.readTree(configFile);
                if (rootNode.has("default_db")) {
                    configData.put("default_db", rootNode.get("default_db").asText());
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("Config file does not exist.");
        }

        return configData;
    }

}
