package pl.gisexpert.rest.model;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.ToString
public class RegisterForm{
	private String firstname;
	private String lastname;
	private String username;
	private String password;
	private String confirmPassword;
	private AddressForm address;
	private String captcha;
}
