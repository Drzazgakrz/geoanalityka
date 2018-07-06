package pl.gisexpert.invoice.model;

import java.util.Date;

public class Invoice extends DocumentModel {
	
	private Company company;
	private Transaction transaction;
	private Date dateOfIssue;
	private String invoiceSerialId;
	private Boolean original;
	private Boolean example;
	
	public Invoice(String invoiceSerialId) {
		this.invoiceSerialId = invoiceSerialId;
		dateOfIssue = new Date();
	}
	
	public Company getCompany() {
		return company;
	}
	public void setCompany(Company company) {
		this.company = company;
	}

	public Transaction getTransaction() {
		return transaction;
	}

	public void setTransaction(Transaction transaction) {
		this.transaction = transaction;
	}

	public Date getDateOfIssue() {
		return dateOfIssue;
	}

	public void setDateOfIssue(Date dateOfIssue) {
		this.dateOfIssue = dateOfIssue;
	}

	public String getInvoiceSerialId() {
		return invoiceSerialId;
	}

	public void setInvoiceSerialId(String invoiceSerialId) {
		this.invoiceSerialId = invoiceSerialId;
	}

	public Boolean getOriginal() {
		return original;
	}

	public void setOriginal(Boolean original) {
		this.original = original;
	}

	public Boolean getExample() {
		return example;
	}

	public void setExample(Boolean example) {
		this.example = example;
	}	
	
}
