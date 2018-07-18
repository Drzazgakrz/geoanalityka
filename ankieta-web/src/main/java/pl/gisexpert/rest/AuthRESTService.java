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

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.mail.MessagingException;
import javax.net.ssl.HttpsURLConnection;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.common.collect.Sets;
import com.google.gson.Gson;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.credential.DefaultPasswordService;
import org.apache.shiro.crypto.hash.DefaultHashService;
import org.primefaces.json.JSONObject;
import org.slf4j.Logger;

import pl.gisexpert.cms.data.*;
import pl.gisexpert.cms.model.*;
import pl.gisexpert.cms.service.LoginAttemptService;
import pl.gisexpert.rest.Validator.RegistrationValidator;
import pl.gisexpert.rest.model.*;
import pl.gisexpert.rest.util.producer.qualifier.RESTI18n;
import pl.gisexpert.service.GlobalConfigService;
import pl.gisexpert.service.MailService;
import pl.gisexpert.service.PasswordHasher;
import pl.gisexpert.util.RandomTokenGenerator;

import static org.apache.shiro.authc.credential.DefaultPasswordService.DEFAULT_HASH_ALGORITHM;

@Path("/auth")
@RequestScoped
public class AuthRESTService {

    @Context
    ServletContext servletContext;

    @Inject
    private AccountRepository accountRepository;

    @Inject
    private RoleRepository roleRepository;

    @Inject
    private AccessTokenRepository accessTokenRepository;

    @Inject
    private LoginAttemptRepository loginAttemptRepository;

    @Inject
    private LoginAttemptService loginAttemptService;

    @Inject
    private GlobalConfigService appConfig;

    @Inject
    private MailService mailService;

    @Inject
    private PasswordHasher passwordHasher;

    @Inject
    private RegistrationValidator registrationValidator;

    @RESTI18n
    private ResourceBundle i18n = ResourceBundle.getBundle("pl.gisexpert.i18n.Text");

    @Inject
    private Logger log;

    private final RandomTokenGenerator resetPasswordTokenGenerator = new RandomTokenGenerator();

    private MessageFormat formatter = new MessageFormat("");

    public AuthRESTService() {
        formatter.setLocale(i18n.getLocale());
    }

    @POST
    @Path("/resendMail")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resendVerificationMail(@Context HttpServletRequest request, String username) {
        Account account = accountRepository.findByEmail(username);
        if (account == null) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Konto nie jest zarejestrowane w systemie");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }

        UUID confirmationCode = UUID.randomUUID();
        AccountConfirmation accountConfirmation = new AccountConfirmation(confirmationCode);

        account.setAccountConfirmation(accountConfirmation);

        String url = request.getRequestURL().toString();
        String baseURL = url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath();
        String confirmAccountURL = baseURL + "/rest/auth/confirm?confirmationCode=" + confirmationCode;

        formatter.applyPattern(i18n.getString("account.confirm.emailtextwithpassword"));
        Object[] params = {confirmAccountURL};

        String emailText = formatter.format(params);

        if (AccountStatus.UNCONFIRMED.equals(account.getAccountStatus())) {
            try {
                accountRepository.edit(account);
                mailService.sendMail(
                        i18n.getString("account.confirm.subject"),
                        emailText,
                        username);
                BaseResponse successResponse = new BaseResponse(Status.OK, "Mail został wysłany ponownie");
                return Response.status(Response.Status.OK).entity(successResponse).build();
            } catch (Exception e) {
                BaseResponse errorStatus = new BaseResponse(Status.BAD_REQUEST, "Wysłanie maila nie powiodło się");
                return Response.status(Response.Status.BAD_REQUEST).entity(errorStatus).build();
            }
        }
        BaseResponse errorStatus = new BaseResponse(Status.BAD_REQUEST, "Twoje konto nie jest nie potwierdzone");
        return Response.status(Response.Status.BAD_REQUEST).entity(errorStatus).build();
    }

    @GET
    @Path("/checkToken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkToken(@Context HttpServletRequest request) {
        String token = request.getHeader("token");
        AccessToken accessToken = accessTokenRepository.findByToken(token);
        if (accessToken != null) {
            LoginAttempt loginAttempt = new LoginAttempt();
            loginAttempt.setDate(new Date());
            loginAttempt.setAccount(accessToken.getAccount());
            loginAttempt.setIp(request.getRemoteAddr());
            token = UUID.randomUUID().toString();

            while (accessTokenRepository.findByToken(token) != null) {
                token = UUID.randomUUID().toString();
            }

            accessToken.setToken(token);

            Date date = new Date();

            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.MINUTE, 240); // token will expire after 240 minutes
            date = cal.getTime();
//comment
            accessToken.setExpires(date);
            accessToken = accessTokenRepository.create(accessToken, true);
            Account konto = accessToken.getAccount();
            String arcGisToken = getAccessToMapForUsers();
            GetTokenResponse getTokenStatus =
                    new GetTokenResponse(konto.getFirstName(),
                            konto.getLastName(),
                            accessToken.getToken(),
                            date,
                            Response.Status.OK,
                            "Successfully generated token",
                            arcGisToken);

            loginAttempt.setSuccessful(true);
            konto.setLastLoginDate(new Date());
            accountRepository.edit(konto);
            loginAttemptRepository.create(loginAttempt);
            return Response.status(Response.Status.OK).entity(getTokenStatus).build();
        } else {
            BaseResponse errorStatus = new BaseResponse(Status.UNAUTHORIZED, token + "Token nie pasuje do żadnego konta");
            return Response.status(Response.Status.UNAUTHORIZED).entity(errorStatus).build();
        }
    }

    @POST
    @Path("/register")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response registerAccount(@Context HttpServletRequest request, RegisterForm formData) {
        Map<String, String> errors = registrationValidator.validate(formData);
        if (errors.size() > 0) {
            BaseResponse errorStatus = new BaseResponse(Status.BAD_REQUEST, (new Gson()).toJson(errors));
            return Response.status(Response.Status.BAD_REQUEST).entity(errorStatus).build();
        }

        final AddressForm addressForm = formData.getAddress();

        final Address address = new Address(addressForm.getZipCode(),
                addressForm.getCity(),
                addressForm.getStreet(),
                addressForm.getBuildingNumber(),
                addressForm.getFlatNumber());

        UUID confirmationCode = UUID.randomUUID();

        Role role = roleRepository.findByName("Ankietowani");

        Account account =
                new Account(formData.getUsername(),
                        formData.getFirstname(),
                        formData.getLastname(),
                        passwordHasher.hashPassword(formData.getPassword()),
                        addressForm.getPhone(),
                        Sets.newHashSet(role),
                        new Date(),
                        AccountStatus.UNCONFIRMED,
                        new AccountConfirmation(confirmationCode.toString()),
                        address
                );

        role.addAccount(account);
        address.setAccount(account);

        try {
            accountRepository.create(account);
            roleRepository.edit(role);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            BaseResponse errorStatus = new BaseResponse(Status.INTERNAL_SERVER_ERROR, "Nastąpił nieoczekiwany błąd przy tworzeniu konta." +
                    " Skontaktuj się z administratorem");
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(errorStatus).build();
        }

        String subject = i18n.getString("account.confirm.subject");

        ResourceBundle i18n = ResourceBundle.getBundle("pl.gisexpert.i18n.Text");
        formatter.applyPattern(i18n.getString("account.confirm.emailtext"));
        String url = request.getRequestURL().toString();
        String baseURL = url.substring(0, url.length() - request.getRequestURI().length()) + request.getContextPath();
        String confirmAccountURL = "<a href = " + baseURL + "/rest/auth/confirm?confirmationCode=" + confirmationCode +
                "> Potwierdzenie </a>";
        Object[] params = {confirmAccountURL};
        String emailText = formatter.format(params);

        try {
            mailService.sendMail(subject, emailText, account.getUsername());
        } catch (MessagingException e) {
            BaseResponse registerStatus = new BaseResponse(Status.GATEWAY_TIMEOUT, "Niestety wysyłanie maila weryfikacyjnego nie " +
                    "powiodło się. Skontaktuj się z administratorem");
            return Response.status(Response.Status.OK).entity(registerStatus).build();
        }

        BaseResponse registerStatus = new BaseResponse(Status.OK, "Mail został wysłany");
        return Response.status(Response.Status.OK).entity(registerStatus).build();
    }

    @POST
    @Path("/getToken")
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getToken(@Context HttpServletRequest request, GetTokenForm formData) {

        Account account = accountRepository.findByEmail(formData.getUsername());

        LoginAttempt loginAttempt = new LoginAttempt();
        loginAttempt.setDate(new Date());
        loginAttempt.setAccount(account);
        loginAttempt.setIp(request.getRemoteAddr());

        if (account == null) {
            loginAttempt.setSuccessful(false);
            loginAttemptRepository.create(loginAttempt);
            BaseResponse rs = new BaseResponse(Response.Status.UNAUTHORIZED,
                    i18n.getString("account.validation.usernamenotexists"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(rs).build();
        }

        List<LoginAttempt> recentLoginAttempts = loginAttemptService.findRecentLoginAttempts(5, account, 100);
        if (recentLoginAttempts != null && recentLoginAttempts.size() == 20) {
            BaseResponse rs = new BaseResponse(Response.Status.FORBIDDEN,
                    i18n.getString("account.validation.toomanyloginattempts"));
            return Response.status(Response.Status.FORBIDDEN).entity(rs).build();
        }

        DefaultPasswordService passwordService = new DefaultPasswordService();
        DefaultHashService dhs = new DefaultHashService();
        dhs.setHashIterations(5);
        dhs.setHashAlgorithmName(DEFAULT_HASH_ALGORITHM);
        passwordService.setHashService(dhs);

        if (!passwordService.passwordsMatch(formData.getPassword(), account.getPassword())) {
            loginAttempt.setSuccessful(false);
            loginAttemptRepository.create(loginAttempt);
            BaseResponse rs = new BaseResponse(Response.Status.UNAUTHORIZED,
                    i18n.getString("account.validation.authfailed"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(rs).build();
        }

        if (account.getAccountStatus() == AccountStatus.UNCONFIRMED) {
            BaseResponse rs = new BaseResponse(Response.Status.UNAUTHORIZED,
                    i18n.getString("account.validation.notconfirmed"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(rs).build();
        }

        if (account.getAccountStatus() == AccountStatus.DISABLED) {
            BaseResponse rs = new BaseResponse(Response.Status.UNAUTHORIZED,
                    i18n.getString("account.validation.disabled"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(rs).build();
        }

        if (account.getAccountStatus() == AccountStatus.CONFIRMED) {
            BaseResponse rs = new BaseResponse(Response.Status.UNAUTHORIZED,
                    i18n.getString("account.validation.confirmed"));
            return Response.status(Response.Status.UNAUTHORIZED).entity(rs).build();
        }

        AccessToken accessToken = new AccessToken();
        String token = UUID.randomUUID().toString();

        while (accessTokenRepository.findByToken(token) != null) {
            token = UUID.randomUUID().toString();
        }

        accessToken.setToken(token);

        Date date = new Date();

        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, 240); // token will expire after 240 minutes
        date = cal.getTime();

        accessToken.setExpires(date);
        accessToken.setAccount(account);
        accessToken = accessTokenRepository.create(accessToken, true);
        String arcGisToken = getAccessToMapForUsers();
        GetTokenResponse getTokenStatus =
                new GetTokenResponse(account.getFirstName(),
                        account.getLastName(),
                        accessToken.getToken(),
                        date,
                        Response.Status.OK,
                        "Successfully generated token",
                        arcGisToken);

        loginAttempt.setSuccessful(true);
        account.setLastLoginDate(new Date());
        accountRepository.edit(account);
        loginAttemptRepository.create(loginAttempt);

        return Response.status(Response.Status.OK).entity(getTokenStatus).build();

    }

    @POST
    @Path("/submitSurvey")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response submitSurvey(SubmitFormData submitFormData) {
        AccessToken accessToken = accessTokenRepository.findByToken(submitFormData.getToken());

        if (accessToken == null) {
            return Response.status(Status.UNAUTHORIZED).build();
        }

        Date date = new Date();

        if (date.after(accessToken.getExpires())) {
            accessTokenRepository.remove(accessToken);
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        HttpURLConnection connection = null;

        try {
            URL url = new URL("http://services1.arcgis.com/mQcAehnytds8jMvo/arcgis/rest/services/Bilgoraj_ankieta/FeatureServer/0/applyEdits");
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type",
                    "application/x-www-form-urlencoded");

            connection.setRequestProperty("Content-Length",
                    Integer.toString(submitFormData.getUpdate().getBytes().length));
            connection.setRequestProperty("Content-Language", "en-US");

            connection.setUseCaches(false);
            connection.setDoOutput(true);

            DataOutputStream wr = new DataOutputStream(
                    connection.getOutputStream());
            wr.writeBytes(submitFormData.getUpdate());
            wr.close();

            InputStream is = connection.getInputStream();
            BufferedReader rd = new BufferedReader(new InputStreamReader(is));
            StringBuilder response = new StringBuilder(); // or StringBuffer if Java version 5+
            String line;
            while ((line = rd.readLine()) != null) {
                response.append(line);
                response.append('\r');
            }
            rd.close();
            return Response.status(Response.Status.OK).build();
        } catch (Exception e) {
            accessTokenRepository.remove(accessToken);
            SecurityUtils.getSubject().logout();
            e.printStackTrace();
            return Response.status(Response.Status.FORBIDDEN).build();
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    @GET
    @Path("/renewToken")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response renewToken(@Context HttpServletRequest request) {
        String token = request.getHeader("token");

        AccessToken accessToken = accessTokenRepository.findByToken(token);

        if (accessToken == null) {
            return Response.status(Response.Status.FORBIDDEN).build();
        }

        Date date = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.add(Calendar.MINUTE, 240); // token will expire after 240 minutes
        date = cal.getTime();

        accessToken.setExpires(date);
        accessTokenRepository.edit(accessToken);

        log.debug("Udało się odświeżyć autentyfikację: " + accessToken.getToken());

        return Response.status(Response.Status.OK).build();

    }

    @POST
    @Path("/resetPassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@Context HttpServletRequest request, ResetPasswordForm resetPasswordForm) {


        String subject = i18n.getString("account.resetpassword.emailtitle");

        Account account = accountRepository.findByEmail(resetPasswordForm.getUsername());

        if (account == null) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Podany adres E-Mail nie jest zarejestrowany w systemie");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }

        String token = resetPasswordTokenGenerator.nextToken();
        Date expirationDate = new Date();
        Calendar cal = Calendar.getInstance();
        cal.setTime(expirationDate);
        cal.add(Calendar.HOUR, 24);
        expirationDate = cal.getTime();

        ResetPassword resetPassword = new ResetPassword(token, expirationDate);

        account.setResetPassword(resetPassword);
        accountRepository.edit(account);

        formatter.applyPattern(i18n.getString("account.resetpassword.emailtext"));

        String baseURL = "http://ankieta-test-frontend.gis-expert.pl/aplikacja3d_bilgoraj/bilgoraj_v2?resetToken=";
        String resetPasswordURL = baseURL + token;
        Object[] params = {resetPasswordURL};
        String emailText = formatter.format(params);

        try {
            mailService.sendMail(subject, emailText, resetPasswordForm.getUsername());
            BaseResponse rs = new BaseResponse(Status.OK, "Wiadomość została wysłana");
            return Response.status(Status.OK).entity(rs).build();
        } catch (Exception e) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Nie udało się wysłać maila. W celu ponownego " +
                    "wysłania spróbuj zalogować się w systemie lub skontaktuj się z administratorem");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }
    }

    @POST
    @Path("/changePassword")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response resetPassword(@Context HttpServletRequest request, ChangePasswordForm changePasswordForm) {

        DefaultPasswordService passwordService = new DefaultPasswordService();
        DefaultHashService dhs = new DefaultHashService();
        dhs.setHashIterations(5);
        dhs.setHashAlgorithmName("SHA-256");
        passwordService.setHashService(dhs);

        Account account = accountRepository.findByResetPasswordToken(changePasswordForm.getResetPasswordToken());

        if (account == null) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Nieprawidłowy token");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }
        if (changePasswordForm.getConfirmPassword() == null || changePasswordForm.getPassword() == null ||
                changePasswordForm.getConfirmPassword().isEmpty() || changePasswordForm.getPassword().isEmpty()) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Pole nie może być puste");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }
        if (changePasswordForm.getPassword().length() < 6) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Hasło powinno zawierać co najmniej 6 znaków");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }
        if (!changePasswordForm.getConfirmPassword().equals(changePasswordForm.getPassword())) {
            BaseResponse rs = new BaseResponse(Status.BAD_REQUEST, "Podane hasła nie są zgodne");
            return Response.status(Response.Status.BAD_REQUEST).entity(rs).build();
        }

        account.setPassword(passwordService.encryptPassword(changePasswordForm.getPassword()));
        account.setResetPassword(new ResetPassword());
        accountRepository.edit(account);

        BaseResponse rs = new BaseResponse(Status.OK, "Hasło zostało zmienione");
        return Response.status(Status.OK).entity(rs).build();
    }

    @GET
    @Path("/signOut")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response deauthToken(@Context HttpServletRequest request) {
        String token = request.getHeader("token");

        if (token == null) {
            return Response.status(Response.Status.UNAUTHORIZED).build();
        }

        AccessToken tokenEntity = accessTokenRepository.findByToken(token);
        if (tokenEntity != null) {
            accessTokenRepository.remove(tokenEntity);
        }

        SecurityUtils.getSubject().logout();

        return Response.status(Response.Status.OK).build();
    }

    @GET
    @Path("/backgroundImage")
    @Produces("image/jpg")
    public Response getBackgroudImage(@Context HttpServletRequest request) {
        return Response.ok()
                .entity(this.servletContext.getResourceAsStream("/WEB-INF/splash2.jpg"))
                .type("image/jpeg")
                .build();
    }

    @GET
    @Path("/getAnonymousAccess")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAccessToMapForGuests(@Context HttpServletRequest request) {

        try {
            String data = "client_id=sGBPNhD2vAUbbiMS&client_secret=f51719b69a5f4015a8a004a5d4a200d6&grant_type=client_credentials";
            String response = sendRequest(data).toString();
            JSONObject object = new JSONObject(response);
            object.put("arcGisToken",object.getString("access_token"));
            object.remove("access_token");
            return Response.status(Status.OK).entity(object.toString()).build();
        } catch (Exception e) {
            e.printStackTrace();
            return Response.status(Status.BAD_GATEWAY).build();
        }
    }
    public String getAccessToMapForUsers() {
        try {
            String data = "client_id=cCm4VuqLeIBtwBBc&client_secret=95dc23f5e40e4ca9a8d2f61aed80c1c3&grant_type=client_credentials";
            StringBuffer response = sendRequest(data);
            JSONObject json;
            if (response!=null) {
                log.info(response.toString());
                json = new JSONObject(response.toString());
                return json.getString("access_token");
            }
        } catch (Exception e) {
            e.printStackTrace();

        }
        return null;
    }
    private StringBuffer sendRequest(String data) throws Exception{
        String url = "https://www.arcgis.com/sharing/oauth2/token";
        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        con.setDoOutput(true);
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.writeBytes(data);
        wr.flush();
        wr.close();
        int responseCode = con.getResponseCode();
        if(responseCode == 200){
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(con.getInputStream()));
            String inputLine;
            StringBuffer response = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }
            return response;
        }
        return null;
    }
    @GET
    public void signInToArcGis(@Context HttpServletRequest request){

    }
}