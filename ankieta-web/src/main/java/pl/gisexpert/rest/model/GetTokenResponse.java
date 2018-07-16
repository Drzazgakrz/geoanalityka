package pl.gisexpert.rest.model;

import java.util.Date;
import javax.ws.rs.core.Response.Status;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode(callSuper = true)
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.ToString
public class GetTokenResponse extends BaseResponse {

	private String firstname;
	private String lastname;
	private String token;
	private Date expires;

	public GetTokenResponse(String firstname, String lastname, String token, Date expires ,Status responseStatus,String message) {
		super(responseStatus,message);
		this.firstname = firstname;
		this.lastname = lastname;
		this.token = token;
		this.expires = expires;
	}
}
