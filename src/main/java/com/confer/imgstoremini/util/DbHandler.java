package com.confer.imgstoremini.util;


import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageThumbObjDTO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;

import java.util.List;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

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

    public ImageObj getImage(ImageThumbObjDTO imageThumbObjDTO){
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        ImageObj imageObjDel = session.get(ImageObj.class, imageThumbObjDTO.getImageId());
        transaction.commit();
        return imageObjDel;
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

    public boolean deleteImage(ImageThumbObjDTO imageObj) {
        try {
            Session session = getSession();
            Transaction transaction = session.beginTransaction();
            ImageObj imageObjDel = session.get(ImageObj.class, imageObj.getImageId());
            transaction.commit();

            session = getSession();
            transaction = session.beginTransaction();
            session.delete(imageObjDel);
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

    public List<ImageObj> getFirst10Images() {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder cb = session.getCriteriaBuilder();

        CriteriaQuery<ImageObj> cq = cb.createQuery(ImageObj.class);

        Root<ImageObj> root = cq.from(ImageObj.class);

        cq.orderBy(cb.asc(root.get("imageDate")));
        Query<ImageObj> query = session.createQuery(cq);
        query.setMaxResults(10);
        List<ImageObj> objList = query.getResultList();
        transaction.commit();
        return objList;
    }

    public int calculateTotalPages(int pageSize) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();

        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<Long> cq = cb.createQuery(Long.class);

        Root<ImageObj> root = cq.from(ImageObj.class);
        cq.select(cb.count(root));

        Query<Long> query = session.createQuery(cq);
        long totalRecords = query.getSingleResult();
        transaction.commit();

        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    public List<ImageThumbObjDTO> getImagesForPageThumb(int pageNumber, int pageSize, boolean Ascending) {
        Session session = getSession();
        Transaction transaction = null;
        List<ImageThumbObjDTO> imageThumbList = null;
        try {
            transaction = session.beginTransaction();
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<ImageThumbObjDTO> cq = cb.createQuery(ImageThumbObjDTO.class);
            Root<ImageObj> root = cq.from(ImageObj.class);

            if (Ascending) {
                cq.orderBy(cb.asc(root.get("imageDate")));
            } else {
                cq.orderBy(cb.desc(root.get("imageDate")));
            }
            cq.select(cb.construct(
                    ImageThumbObjDTO.class,
                    root.get("imageId"),
                    root.get("imageTitle"),
                    root.get("imageTags"),
                    root.get("imageType"),
                    root.get("thumbnailImageByte"),
                    root.get("imageDate")
            ));
            TypedQuery<ImageThumbObjDTO> query = session.createQuery(cq);
            query.setFirstResult((pageNumber - 1) * pageSize);
            query.setMaxResults(pageSize);

            imageThumbList = query.getResultList();

            if (transaction != null) {
                transaction.commit();
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }
        return imageThumbList;
    }

    public List<ImageObj> getImagesForPage(int pageNumber, int pageSize, boolean Ascending) {
        Session session = getSession();
        Transaction transaction = session.beginTransaction();
        CriteriaBuilder cb = session.getCriteriaBuilder();
        CriteriaQuery<ImageObj> cq = cb.createQuery(ImageObj.class);
        Root<ImageObj> root = cq.from(ImageObj.class);

        if (Ascending) {
            cq.orderBy(cb.asc(root.get("imageDate")));
        } else {
            cq.orderBy(cb.desc(root.get("imageDate")));
        }

        Query<ImageObj> query = session.createQuery(cq);
        query.setFirstResult((pageNumber - 1) * pageSize);
        query.setMaxResults(pageSize);
        List<ImageObj> imageObjList = query.getResultList();
        transaction.commit();
        return imageObjList;
    }

}
