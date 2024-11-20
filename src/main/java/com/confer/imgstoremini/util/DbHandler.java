package com.confer.imgstoremini.util;

import com.confer.imgstoremini.model.ImageObj;
import com.confer.imgstoremini.model.ImageThumbObjDTO;
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.Transaction;
import org.hibernate.query.Query;
import com.confer.SearchCriteriaExtractor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
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

    public int calculateTotalPagesRegex(int pageSize, String searchQuery) {
        Session session = getSession();
        Transaction transaction = null;
        long totalRecords = 0;

        try {
            transaction = session.beginTransaction();
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<ImageObj> root = countQuery.from(ImageObj.class);
            Map<String, List<String>> searchCriteria = SearchCriteriaExtractor.extractSearchCriteria(searchQuery);

            List<Predicate> predicates = new ArrayList<>();

            if (searchCriteria.containsKey("Title")) {
                for (String title : searchCriteria.get("Title")) {
                    predicates.add(cb.like(cb.lower(root.get("imageTitle")), "%" + title.toLowerCase() + "%"));
                }
            }

            if (searchCriteria.containsKey("Tag")) {
                for (String tag : searchCriteria.get("Tag")) {
                    predicates.add(cb.like(cb.lower(root.get("imageTags")), "%" + tag.toLowerCase() + "%"));
                }
            }
            Predicate finalPredicate = cb.or(predicates.toArray(new Predicate[0]));
            countQuery.select(cb.count(root)).where(finalPredicate);

            totalRecords = session.createQuery(countQuery).getSingleResult();

            if (transaction != null) {
                transaction.commit();
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }

        return (int) Math.ceil((double) totalRecords / pageSize);
    }

    public int calculateTotalPages(int pageSize, String searchQuery) {
        Session session = getSession();
        Transaction transaction = null;
        long totalRecords = 0;

        try {
            transaction = session.beginTransaction();
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
            Root<ImageObj> root = countQuery.from(ImageObj.class);

            Predicate filter = cb.or(
                    cb.like(cb.lower(root.get("imageTitle")), "%" + searchQuery.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("imageTags")), "%" + searchQuery.toLowerCase() + "%")
            );
            countQuery.select(cb.count(root)).where(filter);

            totalRecords = session.createQuery(countQuery).getSingleResult();

            if (transaction != null) {
                transaction.commit();
            }

        } catch (Exception e) {
            if (transaction != null) {
                transaction.rollback();
            }
            e.printStackTrace();
        }

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

    public List<ImageThumbObjDTO> getImagesForPageThumb(int pageNumber, int pageSize, boolean ascending, String searchQuery) {
        Session session = getSession();
        Transaction transaction = null;
        List<ImageThumbObjDTO> imageThumbList = null;

        try {
            transaction = session.beginTransaction();
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<ImageThumbObjDTO> cq = cb.createQuery(ImageThumbObjDTO.class);
            Root<ImageObj> root = cq.from(ImageObj.class);

            Predicate filter = cb.or(
                    cb.like(cb.lower(root.get("imageTitle")), "%" + searchQuery.toLowerCase() + "%"),
                    cb.like(cb.lower(root.get("imageTags")), "%" + searchQuery.toLowerCase() + "%")
            );
            cq.where(filter);

            if (ascending) {
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

    public List<ImageThumbObjDTO> getImagesForPageThumbRegex(int pageNumber, int pageSize, boolean ascending, String searchQuery) {
        Session session = getSession();
        Transaction transaction = null;
        List<ImageThumbObjDTO> imageThumbList = null;

        try {
            transaction = session.beginTransaction();
            CriteriaBuilder cb = session.getCriteriaBuilder();
            CriteriaQuery<ImageThumbObjDTO> cq = cb.createQuery(ImageThumbObjDTO.class);
            Root<ImageObj> root = cq.from(ImageObj.class);

            Map<String, List<String>> searchCriteria = SearchCriteriaExtractor.extractSearchCriteria(searchQuery);

            List<Predicate> predicates = new ArrayList<>();

            if (searchCriteria.containsKey("Title")) {
                for (String title : searchCriteria.get("Title")) {
                    predicates.add(cb.like(cb.lower(root.get("imageTitle")), "%" + title.toLowerCase() + "%"));
                }
            }

            if (searchCriteria.containsKey("Tag")) {
                for (String tag : searchCriteria.get("Tag")) {
                    predicates.add(cb.like(cb.lower(root.get("imageTags")), "%" + tag.toLowerCase() + "%"));
                }
            }

            Predicate finalPredicate = cb.and(predicates.toArray(new Predicate[0]));
            cq.where(finalPredicate);

            if (ascending) {
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

}
