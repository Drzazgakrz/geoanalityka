package pl.gisexpert.invoice.model;

import java.io.Serializable;

public class Company implements Serializable {

	private static final long serialVersionUID = 1124895109590100127L;

	private String phone;
	private String companyName;
	private String taxId;
	private Address address;

	public String getPhone() {
		return phone;
	}

	public void setPhone(String phone) {
		this.phone = phone;
	}

	public String getCompanyName() {
		return companyName;
	}

	public void setCompanyName(String companyName) {
		this.companyName = companyName;
	}

	public String getTaxId() {
		return taxId;
	}

	public void setTaxId(String taxId) {
		this.taxId = taxId;
	}

	public Address getAddress() {
		return address;
	}

	public void setAddress(Address address) {
		this.address = address;
	}

}
