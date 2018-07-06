package pl.gisexpert.invoice;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperExportManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRBeanCollectionDataSource;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import pl.gisexpert.DocFactory;
import pl.gisexpert.invoice.model.Company;
import pl.gisexpert.invoice.model.DocumentModel;
import pl.gisexpert.invoice.model.Invoice;

public class InvoiceDocFactory implements DocFactory {

	public InvoiceDocFactory() {

	}

	public byte[] createPdf(DocumentModel documentModel) {

		JasperPrint invoicePrint = createInvoicePrint(documentModel);

		try {
			return JasperExportManager.exportReportToPdf(invoicePrint);
		} catch (JRException e) {
			e.printStackTrace();
		}

		return null;

	}

	public byte[] createRtf(DocumentModel documentModel) {
		JasperPrint invoicePrint = createInvoicePrint(documentModel);
		
		JRRtfExporter exporter = new JRRtfExporter();
		exporter.setExporterInput(new SimpleExporterInput(invoicePrint));
		
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		exporter.setExporterOutput(new SimpleWriterExporterOutput(byteArrayOutputStream));
		
		try {
			exporter.exportReport();
			return byteArrayOutputStream.toByteArray();
		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
		
	}

	public JasperPrint createInvoicePrint(DocumentModel documentModel) {
		Invoice invoiceModel = (Invoice) documentModel;
		
		String invoiceTemplatePath = "invoice/Invoice.jrxml";
		if (invoiceModel.getExample()) {
			invoiceTemplatePath = "invoice/Invoice-wzor.jrxml";
		}
		
		JasperReport invoiceReport;
		try {
			invoiceReport = JasperCompileManager
					.compileReport(getClass().getClassLoader().getResourceAsStream(invoiceTemplatePath));
			JRDataSource transactionItemsDS = createDataSource(invoiceModel);
			Map<String, Object> parameters = createParameters(invoiceModel);
			JasperPrint invoicePrint = JasperFillManager.fillReport(invoiceReport, parameters, transactionItemsDS);

			return invoicePrint;

		} catch (JRException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return null;
	}

	public Map<String, Object> createParameters(Invoice documentModel) {
		
		Company company = documentModel.getCompany();
		String companyAddr1 = company.getAddress().getZipcode() + " " + company.getAddress().getCity();
		String companyAddr2 = company.getAddress().getStreet() + " " + company.getAddress().getHouseNumber();
		companyAddr2 += company.getAddress().getFlatNumber() != null && !company.getAddress().getFlatNumber().equals("") ? "/" + company.getAddress().getFlatNumber(): "";
		
		Map<String, Object> parameters = new HashMap<String, Object>();
		parameters.put("logo", getClass().getClassLoader().getResourceAsStream("invoice/gis_logo_notext.png"));
		parameters.put("date", documentModel.getDateOfIssue());
		parameters.put("invoice_number", documentModel.getInvoiceSerialId());
		parameters.put("isOryginal", documentModel.getOriginal());
		
		parameters.put("bill_to__company_name", company.getCompanyName());
		parameters.put("bill_to__company_addr1", companyAddr1);
		parameters.put("bill_to__company_addr2", companyAddr2);
		
		parameters.put("ship_to__company_name", company.getCompanyName());
		parameters.put("ship_to__company_addr1", companyAddr1);
		parameters.put("ship_to__company_addr2", companyAddr2);
		
		if (company.getTaxId() != null) {
			parameters.put("bill_to__company_taxid", "NIP: " + company.getTaxId());
			parameters.put("ship_to__company_taxid", "NIP: " + company.getTaxId());
		}
		if (company.getPhone() != null) {
			parameters.put("bill_to__company_contact1", "tel. " + company.getPhone());
			parameters.put("ship_to__company_contact1", "tel. " + company.getPhone());
		}

		return parameters;
	}

	public JRDataSource createDataSource(Invoice documentModel) {
		JRDataSource transactionItemsDS = new JRBeanCollectionDataSource(documentModel.getTransaction().getItems());
		return transactionItemsDS;
	}

}
