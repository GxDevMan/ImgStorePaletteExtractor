package com.confer.imgstoremini.model;

import org.hibernate.SessionFactory;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Configuration;
import org.hibernate.service.ServiceRegistry;

public class hibernateUtil {
    private static hibernateUtil instance;
    private SessionFactory sessionFactory;

    private hibernateUtil(String dbURL){
        configureSessionFactory(dbURL);
    }

    public static hibernateUtil getInstance(String dbURL){
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

    public void shutdown(){
        if(sessionFactory != null){
            sessionFactory.close();
            instance = null;
        }
    }
}
