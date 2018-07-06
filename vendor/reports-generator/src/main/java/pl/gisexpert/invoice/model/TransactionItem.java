package pl.gisexpert.invoice.model;

public class TransactionItem {
	
	private Integer ordinal;
	private String itemName;
	private Double unitNetPrice;
	private Double unitGrossPrice;
	private Double totalNetPrice;
	private Double totalGrossPrice;
	private Double totalVatAmount;
	private Double amount;
	private Double vat;
	
	public TransactionItem(Integer ordinal, String itemName, Double unitGrossPrice, Double amount, Double vat) {
		this.ordinal = ordinal;
		this.itemName = itemName;
		this.unitGrossPrice = unitGrossPrice;
		this.amount = amount;
		this.vat = vat;
		
		this.unitNetPrice = this.unitGrossPrice / (1.0 + this.vat);
		this.totalNetPrice = this.unitNetPrice * this.amount;
		this.totalGrossPrice = this.totalNetPrice * (1.0 + this.vat);
		this.totalVatAmount = this.totalGrossPrice - this.totalNetPrice;
	}
	
	public Integer getOrdinal() {
		return ordinal;
	}
	public String getItemName() {
		return itemName;
	}
	public Double getUnitNetPrice() {
		return unitNetPrice;
	}
	public Double getUnitGrossPrice() {
		return unitGrossPrice;
	}
	public Double getTotalNetPrice() {
		return totalNetPrice;
	}
	public Double getTotalGrossPrice() {
		return totalGrossPrice;
	}
	public Double getTotalVatAmount() {
		return totalVatAmount;
	}
	public Double getAmount() {
		return amount;
	}
	public Double getVat() {
		return vat;
	}
	

	
	
}
