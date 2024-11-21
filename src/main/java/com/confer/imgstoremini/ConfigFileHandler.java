package com.confer.imgstoremini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;

public class ConfigFileHandler {

    public Map<String, String> getConfig() {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";
        File configFile = new File(filePath);
        boolean configExistTest = checkConfigFile();

        if (!configExistTest) {
            createDefaultConfigFile(configFile);
            return getConfig();
        }

        Map<String, String> configData = new HashMap<>();
        ObjectMapper mapper = new ObjectMapper();
        try {
            JsonNode rootNode = mapper.readTree(configFile);
            if (rootNode.has("default_db")) {
                configData.put("default_db", rootNode.get("default_db").asText());
            }
            if (rootNode.has("default_pagesize")) {
                configData.put("default_pagesize", rootNode.get("default_pagesize").asText());
            }
            if(rootNode.has("default_regionspalette")){
                configData.put("default_regionspalette", rootNode.get("default_regionspalette").asText());
            }

        } catch (Exception e) {
            File configFile2 = new File(filePath);
            createDefaultConfigFile(configFile2);
            return getConfig();
        }
        return configData;
    }

    private boolean checkConfigFile() {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";

        File configFile = new File(filePath);
        return configFile.exists();
    }

    public boolean checkDBSpecifiedInConfigFile() {
        try {
            Map<String, String> loadedConfig = getConfig();
            String dbPath = loadedConfig.get("default_db");
            File dbfile = new File(dbPath);
            return dbfile.exists();
        } catch (Exception e){
            String currentDir = System.getProperty("user.dir");
            String filePath = currentDir + File.separator + "config.json";
            File configFile = new File(filePath);
            createDefaultConfigFile(configFile);
            return checkDBSpecifiedInConfigFile();
        }
    }

    public void createDefaultConfigFile(File configFile) {
        Map<String, String> defaultConfig = new HashMap<>();
        defaultConfig.put("default_db", "default_imageStore.db");
        defaultConfig.put("default_pagesize", "10");
        defaultConfig.put("default_regionspalette","4");
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        try {
            writer.writeValue(configFile, defaultConfig);
        } catch (IOException e) {
        }
    }

    public void createCustomConfigFile(Map<String, String> customConfig) {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";
        File configFile = new File(filePath);
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();

        try {
            writer.writeValue(configFile, customConfig);
        } catch (IOException e) {
        }
    }

}
