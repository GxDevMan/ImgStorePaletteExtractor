package com.confer.imgstoremini.util;

import java.util.HashMap;

public class DataStore {
    private static DataStore instance;
    private HashMap<String, Object> appDataStore = new HashMap<>();

    public static DataStore getInstance(){
        if (instance == null){
            synchronized (DataStore.class){
                if (instance == null){
                    instance = new DataStore();
                }
            }
        }
        return instance;
    }

    public boolean insertObject(String key, Object object){
        try{
            this.appDataStore.put(key,object);
            return true;
        } catch (Exception e){
            return false;
        }
    }

    public Object getObject(String key){
        try{
            return this.appDataStore.get(key);
        } catch (Exception e){
            return null;
        }
    }

    public boolean deleteObject(String key){
        try{
            this.appDataStore.remove(key);
            return true;
        }
        catch (Exception e){
            return false;
        }
    }

    public void clearData(){
        this.appDataStore.clear();
    }

    public void DestroyStore(){
        instance = null;
    }
}

