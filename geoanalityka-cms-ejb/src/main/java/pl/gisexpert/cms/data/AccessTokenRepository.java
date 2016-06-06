package pl.gisexpert.cms.data;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import pl.gisexpert.cms.model.AccessToken;
import pl.gisexpert.cms.qualifier.CMSEntityManager;

@ApplicationScoped
public class AccessTokenRepository extends AbstractRepository<AccessToken> {

    @Inject
    @CMSEntityManager
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AccessTokenRepository() {
        super(AccessToken.class);
    }

    public AccessToken findByToken(String token) {
        CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<AccessToken> cq = cb.createQuery(AccessToken.class);
        Root<AccessToken> accessToken = cq.from(AccessToken.class);
        cq.select(accessToken);
        cq.where(cb.equal(accessToken.get("token"), token));
        TypedQuery<AccessToken> q = getEntityManager().createQuery(cq);

        try {
            AccessToken resultAccessToken = q.getSingleResult();
            return resultAccessToken;
        } catch (Exception e) {
            return null;
        }
    }

}
