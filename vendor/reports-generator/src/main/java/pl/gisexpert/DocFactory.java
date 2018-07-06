package pl.gisexpert;

import pl.gisexpert.invoice.model.DocumentModel;

public interface DocFactory {

	public byte[] createPdf(DocumentModel documentModel);
	public byte[] createRtf(DocumentModel documentModel);
	
}
