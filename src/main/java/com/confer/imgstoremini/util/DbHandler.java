package com.confer.imgstoremini.util;

import com.confer.imgstoremini.model.ImageObj;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;

public class DbHandler {

    private Session getSession() {
        hibernateUtil util = hibernateUtil.getInstance();
        SessionFactory sessionFactory = util.getSessionFactory();
        Session session = sessionFactory.getCurrentSession();
        return session;
    }

    public boolean saveImage(ImageObj imageObj) {
        try {
            Session session = getSession();
            Transaction transaction = session.beginTransaction();
            session.save(imageObj);
            transaction.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean updateImage(ImageObj imageObj) {
        try {
            Session session = getSession();
            Transaction transaction = session.beginTransaction();
            session.saveOrUpdate(imageObj);
            transaction.commit();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteImage(ImageObj imageObj) {
        try {
            Session session = getSession();
            Transaction transaction = session.beginTransaction();
            session.delete(imageObj);
            transaction.commit();
            return true;

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<ImageObj> searchByTitleOrTags(String keyword) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        String hql = "FROM ImageObj WHERE imageTitle LIKE :keyword OR imageTags LIKE :keyword";
        Query<ImageObj> query = session.createQuery(hql, ImageObj.class);
        query.setParameter("keyword", "%" + keyword + "%");
        List<ImageObj> imageList = query.getResultList();
        transaction.commit();
        return imageList;
    }

}
