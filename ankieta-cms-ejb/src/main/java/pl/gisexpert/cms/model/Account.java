package pl.gisexpert.cms.model;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Set;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import org.hibernate.envers.Audited;


@Entity
@Table(name = "accounts", indexes = { @Index(name = "username_index", columnList = "username", unique = true) })
@SqlResultSetMappings({ @SqlResultSetMapping(name = "Account.sumMapping", columns = {
		@ColumnResult(name = "items_count", type = Integer.class) }) })
@NamedNativeQueries({
		@NamedNativeQuery(name = "Account.removeRole", query = "DELETE FROM account_roles WHERE username = ? AND role = ?"),
		@NamedNativeQuery(name = "Account.getRoles", query = "SELECT roles.* FROM roles, account_roles WHERE account_roles.username = :username AND roles.name = account_roles.role", resultClass = Role.class),
		@NamedNativeQuery(name = "Account.hasRole", query = "SELECT COUNT(*) as items_count FROM account_roles WHERE username = :username AND role = :role", resultSetMapping = "Account.sumMapping") })
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)

@lombok.Getter
@lombok.Setter

@lombok.EqualsAndHashCode(of = {"username"})
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
public class Account implements Serializable{

	private static final long serialVersionUID = 1033705321916453635L;

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "username", unique = true, nullable = false, length = 60)
	@NotNull
	@Size(min = 5, max = 60)
	private String username;

	@Column(name = "first_name", nullable = false, length = 30)
	@NotNull
	@Size(min = 3, max = 30)
	private String firstName;

	@Column(name = "last_name", nullable = false, length = 30)
	@NotNull
	@Size(min = 3, max = 30)
	private String lastName;

	@Column(nullable = false, length = 100)
	@NotNull
	private String password;

	@Column(length = 11)
	@Size(min = 9, max = 11)
	private String phone;

	@Audited
	@ManyToMany
	@JoinTable(name = "account_roles", joinColumns = {
			@JoinColumn(name = "username", referencedColumnName = "username") }, inverseJoinColumns = {
					@JoinColumn(name = "role", referencedColumnName = "name") }, indexes = {
							@Index(name = "role_username_index", columnList = "username", unique = false) })
	private Set<Role> roles;

	@Column(nullable = false, name = "date_registered")
	@Temporal(javax.persistence.TemporalType.TIMESTAMP)
	private Date dateRegistered;

	@Column(name = "date_last_login")
	@Temporal(javax.persistence.TemporalType.TIMESTAMP)
	private Date lastLoginDate;

	@Column(name = "account_status", nullable = false)
	@Enumerated(EnumType.STRING)
	private AccountStatus accountStatus;

	@Embedded
	private ResetPassword resetPassword;

	@Embedded
	private AccountConfirmation accountConfirmation;

	@OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.REMOVE, mappedBy = "account")
	private List<AccessToken> tokens;

	@OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.REMOVE, CascadeType.MERGE, CascadeType.PERSIST})
	@JoinColumn(name = "address_id")
	private Address address;

	public Account(String username, String firstName, String lastName, String password, String phone,
				   Set<Role> roles, Date dateRegistered, AccountStatus accountStatus, AccountConfirmation accountConfirmation, Address address) {
		this.username = username;
		this.firstName = firstName;
		this.lastName = lastName;
		this.password = password;
		this.phone = phone;
		this.roles = roles;
		this.accountStatus = accountStatus;
		this.dateRegistered = dateRegistered;
		this.accountConfirmation = accountConfirmation;
		this.address = address;
	}

	@Transient
	public String getDiscriminatorValue(){
	    DiscriminatorValue val = this.getClass().getAnnotation( DiscriminatorValue.class );

	    return val == null ? null : val.value();
	}
}