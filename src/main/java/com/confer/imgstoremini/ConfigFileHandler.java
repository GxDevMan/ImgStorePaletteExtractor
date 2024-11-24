package com.confer.imgstoremini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;

import java.io.File;
import java.util.Map;
import java.io.IOException;
import java.util.HashMap;

public class ConfigFileHandler {

    public static Map<String, String> getConfig() {
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
            boolean default_db = rootNode.has("default_db");
            boolean default_pagesize = rootNode.has("default_pagesize");
            boolean default_regionspalette = rootNode.has("default_regionspalette");
            boolean default_kmeansiter = rootNode.has("default_kmeansiter");
            boolean default_meanshiftiter = rootNode.has("default_meanshiftiter");
            boolean default_convergence_threshold = rootNode.has("default_convergence_threshold");
            boolean preferred_processor = rootNode.has("preferred_processor");
            boolean default_spectraliter = rootNode.has("default_spectraliter");
            boolean default_gmmiter = rootNode.has("default_gmmiter");


            boolean fieldsCheck = default_db &&
                    default_pagesize &&
                    default_regionspalette &&
                    default_kmeansiter &&
                    default_meanshiftiter &&
                    default_convergence_threshold &&
                    preferred_processor &&
                    default_spectraliter &&
                    default_gmmiter;

            if (!fieldsCheck) {
                createDefaultConfigFile(configFile);
                return getConfig();
            }
            configData.put("default_db", rootNode.get("default_db").asText());
            configData.put("default_pagesize", rootNode.get("default_pagesize").asText());
            configData.put("default_regionspalette", rootNode.get("default_regionspalette").asText());
            configData.put("default_kmeansiter", rootNode.get("default_kmeansiter").asText());
            configData.put("default_meanshiftiter", rootNode.get("default_meanshiftiter").asText());
            configData.put("default_spectraliter", rootNode.get("default_spectraliter").asText());
            configData.put("default_convergence_threshold", rootNode.get("default_convergence_threshold").asText());
            configData.put("preferred_processor", rootNode.get("preferred_processor").asText());
            configData.put("default_gmmiter", rootNode.get("default_gmmiter").asText());

        } catch (Exception e) {
            File configFile2 = new File(filePath);
            createDefaultConfigFile(configFile2);
            return getConfig();
        }
        return configData;
    }

    private static boolean checkConfigFile() {
        String currentDir = System.getProperty("user.dir");
        String filePath = currentDir + File.separator + "config.json";

        File configFile = new File(filePath);
        return configFile.exists();
    }

    public static boolean checkDBSpecifiedInConfigFile() {
        try {
            Map<String, String> loadedConfig = getConfig();
            String dbPath = loadedConfig.get("default_db");
            File dbfile = new File(dbPath);
            return dbfile.exists();
        } catch (Exception e) {
            String currentDir = System.getProperty("user.dir");
            String filePath = currentDir + File.separator + "config.json";
            File configFile = new File(filePath);
            createDefaultConfigFile(configFile);
            return checkDBSpecifiedInConfigFile();
        }
    }

    public static void createDefaultConfigFile(File configFile) {
        Map<String, String> defaultConfig = new HashMap<>();
        defaultConfig.put("default_db", "default_imageStore.db");
        defaultConfig.put("default_pagesize", "5");
        defaultConfig.put("default_regionspalette", "4");
        defaultConfig.put("default_kmeansiter", "100");
        defaultConfig.put("default_meanshiftiter","60");
        defaultConfig.put("default_convergence_threshold","0.1");
        defaultConfig.put("preferred_processor", "CPU");
        defaultConfig.put("default_spectraliter", "100");
        defaultConfig.put("default_gmmiter", "100");
        ObjectMapper mapper = new ObjectMapper();
        ObjectWriter writer = mapper.writerWithDefaultPrettyPrinter();
        try {
            writer.writeValue(configFile, defaultConfig);
        } catch (IOException e) {
        }
    }

    public static void createCustomConfigFile(Map<String, String> customConfig) {
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
