package com.confer.imgstoremini.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

import java.nio.file.Path;
import java.nio.file.Paths;

public class hibernateUtil {
    private static hibernateUtil instance;
    private SessionFactory sessionFactory;

    private hibernateUtil(String dbURL){
        configureSessionFactory(dbURL);
    }

    public static hibernateUtil getInstance(String dbURL){
        extractPathAndFileName(dbURL);
        if (instance == null){
            synchronized (hibernateUtil.class){
                instance = new hibernateUtil(dbURL);
            }
        }
        return instance;
    }

    public static hibernateUtil getInstance(){
        return instance;
    }

    public SessionFactory getSessionFactory(){
        return sessionFactory;
    }

    private void configureSessionFactory(String dbURL){
        try {
            dbURL = String.format("jdbc:sqlite:%s", dbURL);
            org.hibernate.cfg.Configuration configuration = new Configuration().configure();
            configuration.setProperty("hibernate.connection.url", dbURL);

            configuration.addAnnotatedClass(com.confer.imgstoremini.model.ImageObj.class);

            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                    .applySettings(configuration.getProperties()).build();

            if (sessionFactory != null) {
                sessionFactory.close();
            }
            sessionFactory = configuration.buildSessionFactory(serviceRegistry);
        } catch (Throwable ex) {
            throw new ExceptionInInitializerError(ex);
        }
    }

    private static void extractPathAndFileName(String dbURL) {
        String urlWithoutProtocol = dbURL.replaceFirst("^.*://", "");
        Path dbPath = Paths.get(urlWithoutProtocol);
        String fileName = dbPath.getFileName().toString();

        DataStore dataStore = DataStore.getInstance();
        dataStore.insertObject("db_name", fileName);
    }

    public void shutdown(){
        if(sessionFactory != null){
            sessionFactory.close();
            instance = null;
        }
    }
}
