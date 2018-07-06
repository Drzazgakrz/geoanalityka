package pl.gisexpert.invoice.model;

import java.util.List;

public class Transaction {
	
	private List<TransactionItem> items;

	public List<TransactionItem> getItems() {
		return items;
	}

	public void setItems(List<TransactionItem> items) {
		this.items = items;
	}
}
