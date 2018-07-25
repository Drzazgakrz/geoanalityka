package pl.gisexpert.cms.data;

import pl.gisexpert.cms.model.UserNotifications;
import pl.gisexpert.cms.qualifier.CMSEntityManager;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.transaction.Transactional;

@ApplicationScoped
@lombok.Setter
@lombok.Getter
public class NotificationRepository extends AbstractRepository<UserNotifications> {
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    @Inject
    @CMSEntityManager
    private EntityManager em;

    public NotificationRepository(){
        super(UserNotifications.class);
    }
    @Transactional
    public void create(UserNotifications notification){
        em.persist(notification);
    }
}
