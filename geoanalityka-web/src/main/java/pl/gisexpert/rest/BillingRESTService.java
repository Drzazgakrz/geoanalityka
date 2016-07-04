/*
 * JBoss, Home of Professional Open Source
 * Copyright 2013, Red Hat, Inc. and/or its affiliates, and individual
 * contributors by the @authors tag. See the copyright.txt in the
 * distribution for a full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package pl.gisexpert.rest;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.slf4j.Logger;

import pl.gisexpert.cms.data.AccountRepository;
import pl.gisexpert.cms.data.OrderRepository;
import pl.gisexpert.cms.model.Account;
import pl.gisexpert.cms.model.Order;
import pl.gisexpert.cms.model.OrderStatus;
import pl.gisexpert.cms.service.BillingService;
import pl.gisexpert.payu.client.PayUClient;
import pl.gisexpert.payu.model.Buyer;
import pl.gisexpert.payu.model.CreateOrderNotify;
import pl.gisexpert.payu.model.OrderBase;
import pl.gisexpert.payu.model.Product;
import pl.gisexpert.rest.model.AddCreditForm;
import pl.gisexpert.rest.model.BaseResponse;
import pl.gisexpert.service.GlobalConfigService;
import pl.gisexpert.service.GlobalConfigService.PayU;

@Path("/billing")
public class BillingRESTService {

	@Inject
	AccountRepository accountRepository;

	@Inject
	OrderRepository orderRepository;

	@Inject
	BillingService billingService;

	@Inject
	GlobalConfigService appConfig;

	@Inject
	PayUClient payuClient;

	@Inject
	Logger log;

	@POST
	@Path("/add_credit")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	public Response registerAccount(@Context HttpServletRequest request, AddCreditForm formData) {

		PayU payUSettings = appConfig.getPayu();

		Account buyerAccount = accountRepository.findByUsername((String) SecurityUtils.getSubject().getPrincipal(), true);

		OrderBase createOrderForm = new OrderBase();
		createOrderForm.setCurrencyCode("PLN");
		
		String customerIpAddress = request.getHeader("X-Forwarded-For");  
        if (customerIpAddress == null || customerIpAddress.length() == 0 || "unknown".equalsIgnoreCase(customerIpAddress)) {  
            customerIpAddress = request.getHeader("Proxy-Client-IP");  
        }  
        if (customerIpAddress == null || customerIpAddress.length() == 0 || "unknown".equalsIgnoreCase(customerIpAddress)) {  
            customerIpAddress = request.getHeader("WL-Proxy-Client-IP");  
        }  
        if (customerIpAddress == null || customerIpAddress.length() == 0 || "unknown".equalsIgnoreCase(customerIpAddress)) {  
            customerIpAddress = request.getHeader("HTTP_CLIENT_IP");  
        }  
        if (customerIpAddress == null || customerIpAddress.length() == 0 || "unknown".equalsIgnoreCase(customerIpAddress)) {  
            customerIpAddress = request.getHeader("HTTP_X_FORWARDED_FOR");  
        }  
        if (customerIpAddress == null || customerIpAddress.length() == 0 || "unknown".equalsIgnoreCase(customerIpAddress)) {  
            customerIpAddress = request.getRemoteAddr();  
        }  
        
        StringTokenizer stk = new StringTokenizer(customerIpAddress, ":");
		if (stk.hasMoreTokens()) {
			customerIpAddress = stk.nextToken();
		}

		createOrderForm.setCustomerIp(customerIpAddress);
		createOrderForm.setDescription("Środki do wykorzystania w portalu Geoanalizy");
		createOrderForm.setMerchantPosId(payUSettings.getPosId());
		createOrderForm.setNotifyUrl("http://mapy.gis-expert.pl/geoanalityka-web/rest/billing/payu_notify");

		List<Product> products = new ArrayList<>();
		Product product = new Product("1 gr. do wykorzystania w serwisie Geoanalizy", 1, formData.getAmount() * 100);
		products.add(product);

		createOrderForm.setProducts(products);
		createOrderForm.setTotalAmount(formData.getAmount() * 100);
		createOrderForm.setBuyer(new Buyer(buyerAccount));

		Order order = new Order();
		order.setStatus(OrderStatus.PENDING);
		order.setBuyer(buyerAccount);
		order.setAmount(formData.getAmount());
		order.setDate(new Date());

		orderRepository.create(order);
		billingService.addOrder(buyerAccount, order);
		createOrderForm.setExtOrderId(order.getId().toString() + "_" + order.getOrderHash());

		Buyer payuBuyer = new Buyer(buyerAccount);
		createOrderForm.setBuyer(payuBuyer);

		String payuRedirectUrl = payuClient.createOrder(createOrderForm);
		
		if (payuRedirectUrl == null) {
			return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(null).build();
		}
		
		BaseResponse responseEntity = new BaseResponse();
		responseEntity.message = payuRedirectUrl;
		responseEntity.responseStatus = Response.Status.OK;

		return Response.status(Response.Status.OK).entity(responseEntity).build();
	}

	@POST
	@Path("/payu_notify")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response payuNotify(CreateOrderNotify data) {
		
		// Extract IP address from an IP:PORT string
		StringTokenizer astk = new StringTokenizer(data.getOrder().getExtOrderId(), "_");
		Long orderId = null;
		if (astk.hasMoreTokens()) {
			orderId = Long.parseLong(astk.nextToken());
		}

		if (orderId == null) {
			log.warn("Incomming order update from PayU. Received invalid order ID: (" + data.getOrder().getExtOrderId() + ")");
			return Response.status(Response.Status.NOT_FOUND).build();
		}

		Order order = orderRepository.find(orderId);
		order.setPayuOrderId(data.getOrder().getOrderId());

		if (order.getStatus() != OrderStatus.COMPLETED) {
			switch (data.getOrder().getStatus()) {
			case "COMPLETED":
				order.setStatus(OrderStatus.COMPLETED);
				Account buyer = order.getBuyer();
				buyer.setCredits(buyer.getCredits() + (order.getAmount() * 100));
				accountRepository.edit(buyer);
				break;
			case "CANCELED":
				order.setStatus(OrderStatus.CANCELED);
				break;
			case "PENDING":
				order.setStatus(OrderStatus.PENDING);
				break;
			case "REJECTED":
				order.setStatus(OrderStatus.REJECTED);
				break;
			}
		}

		orderRepository.edit(order);

		log.info("Incomming order update from PayU. Order " + order.getId() + " has been updated with status: "
				+ order.getStatus().name());

		return Response.status(Response.Status.OK).build();
	}
}
