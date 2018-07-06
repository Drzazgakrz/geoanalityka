package pl.gisexpert.invoice.model;

import java.util.ArrayList;
import java.util.List;

public class TransactionItemFactory {
	public static List<TransactionItem> createTransactionItems() {
		List<TransactionItem> transactionItems = new ArrayList<TransactionItem>();
		
		TransactionItem item1 = new TransactionItem(1, "Aktywacja planu standardowego", 50.0, 0.23, 1.0);
		
		transactionItems.add(item1);
		
		return transactionItems;
	};
}
