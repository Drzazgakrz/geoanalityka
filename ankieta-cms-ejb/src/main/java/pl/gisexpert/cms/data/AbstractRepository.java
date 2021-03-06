package pl.gisexpert.cms.data;

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.transaction.Transactional;

import org.primefaces.model.SortOrder;

public abstract class AbstractRepository<T> {

	private Class<T> entityClass;

	AbstractRepository(Class<T> entityClass) {
		this.entityClass = entityClass;
	}

	protected abstract EntityManager getEntityManager();

	@Transactional
	public void create(T entity) {
		getEntityManager().persist(entity);
	}

	@Transactional
	public void edit(T entity) {
		getEntityManager().merge(entity);
	}

	@Transactional
	public void remove(T entity) {
		getEntityManager().remove(getEntityManager().merge(entity));
	}

	public T find(Object id) {
		return getEntityManager().find(entityClass, id);
	}

	public List findAll() {
		CriteriaQuery<Object> cq = getEntityManager().getCriteriaBuilder().createQuery();
		cq.select(cq.from(entityClass));
		return getEntityManager().createQuery(cq).getResultList();
	}

	public int count() {
		CriteriaQuery<Object> cq = getEntityManager().getCriteriaBuilder().createQuery();
		Root<T> rt = cq.from(entityClass);
		cq.select(getEntityManager().getCriteriaBuilder().count(rt));
		javax.persistence.Query q = getEntityManager().createQuery(cq);
		return ((Long) q.getSingleResult()).intValue();
	}

	public List<T> loadLazy(int first, int pageSize, String sortField, SortOrder sortOrder,
			Map<String, Object> filters) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<T> cq = cb.createQuery(entityClass);
		Root<T> myObj = cq.from(entityClass);

		if (filters != null && !filters.isEmpty()) {
			cq.where(getFilterCondition(cb, myObj, filters));
		}

		if (sortField != null) {
			if (sortOrder.equals(SortOrder.ASCENDING)) {
				cq.orderBy(cb.asc(myObj.get(sortField)));
			} else if (sortOrder.equals(SortOrder.DESCENDING)) {
				cq.orderBy(cb.desc(myObj.get(sortField)));
			}
		}
		return getEntityManager().createQuery(cq).setFirstResult(first).setMaxResults(pageSize).getResultList();
	}

	private Predicate getFilterCondition(CriteriaBuilder cb, Root<T> root, Map<String, Object> filters) {
		Predicate filterCondition = cb.conjunction();
		String wildCard = "%";
		for (Map.Entry<String, Object> filter : filters.entrySet()) {
			String value = wildCard + filter.getValue() + wildCard;
			if (!filter.getValue().equals("")) {
				javax.persistence.criteria.Path<String> path = root.get(filter.getKey());// order.get(filter.getKey());
				filterCondition = cb.and(filterCondition, cb.like(path, value));

			}
		}
		return filterCondition;
	}

	public int count(Map<String, Object> filters) {
		CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
		CriteriaQuery<Long> cq = cb.createQuery(Long.class);
		Root<T> myObj = cq.from(entityClass);
		cq.where(getFilterCondition(cb, myObj, filters));
		cq.select(cb.count(myObj));
		return getEntityManager().createQuery(cq).getSingleResult().intValue();
	}

}
