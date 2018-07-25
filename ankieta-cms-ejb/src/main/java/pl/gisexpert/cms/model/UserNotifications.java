package pl.gisexpert.cms.model;


import javax.persistence.*;
import java.io.Serializable;

@Entity
@Table(name = "user_notifications", indexes = {@Index(unique = true, columnList = "globalID",name = "global_id"),
        @Index(unique = true, columnList = "objectID",name = "object_id")})
@lombok.Getter
@lombok.Setter
@lombok.AllArgsConstructor
@lombok.NoArgsConstructor
public class UserNotifications implements Serializable {
    @Column(length=45, nullable=false)
    private String globalID;

    @Column(nullable = false)
    private int objectID;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    @JoinColumn(name="accounts_pkey" )
    private Account account;
    public UserNotifications(String globalID, int objectID, Account account){
        this.globalID = globalID;
        this.objectID = objectID;
        this.account = account;
    }
}
