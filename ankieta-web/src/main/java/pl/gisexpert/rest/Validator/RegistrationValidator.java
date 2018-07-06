package pl.gisexpert.rest.Validator;

import pl.gisexpert.cms.data.AccountRepository;
import pl.gisexpert.rest.reCaptcha.CaptchaVerifyUtils;
import pl.gisexpert.rest.model.RegisterForm;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.*;

@RequestScoped
public class RegistrationValidator {

    @Inject
    private AccountRepository accountRepository;

    @Inject
    private CaptchaVerifyUtils captchaVerifyUtils;

    public Map<String,String> validate(RegisterForm form){
        Map<String,String> errors = new HashMap<>();
        this.checkFirstName(errors,form.getFirstname());
        this.checkLastname(errors,form.getLastname());
        this.checkMail(errors,form.getUsername());
        this.checkPassword(errors,form.getPassword(),form.getConfirmPassword());
        this.checkStreet(errors,form.getAddress().getStreet());
        this.checkBuildingNumber(errors,form.getAddress().getBuildingNumber());
        this.checkZipCode(errors,form.getAddress().getZipCode());
        if(!form.getAddress().getFlatNumber().equals(""))
        this.checkFlatNumber(errors,form.getAddress().getFlatNumber());
        this.checkCity(errors,form.getAddress().getCity());
        this.checkPhone(errors,form.getAddress().getPhone());
        //this.checkCaptcha(errors,form.getCaptcha());
        return errors;
    }
    private void checkFirstName(Map<String,String> errors, String firstName) {
        if (!matching("(^[A-Z ÀÁÂÃÄÅ ĄŻŹ ÒÓÔÕÖØ Ł Ć ĘŚ Ń ÈÉÊË Ç ÌÍÎÏ ÙÚÛÜ Ñ ]{1})([a-zàáâãäåąźżòóÓłćęśńôõöøèéêëçìíîïùúûüÿñ]{2,29}$)", firstName))
        errors.put("firstname","Imię musi zaczynać się z wielkiej litery i mieć co najmniej 3 znaki");

    }
    private void checkLastname(Map<String,String> errors, String lastname) {
        if (!matching("(^[A-Z ÀÁÂÃÄÅ ĄŻŹ ÒÓÔÕÖØ Ł Ć ĘŚ Ń ÈÉÊË Ç ÌÍÎÏ ÙÚÛÜ Ñ]{1})([a-zàáâãäåąźżòóÓłćęśńôõöøèéêëçìíîïùúûüÿñ]{2,29}$)",lastname))
        errors.put("lastname","Nazwisko musi zaczynać się z wielkiej litery i mieć co najmniej 3 znaki");
    }

    private void checkMail(Map<String,String> errors, String email){
        if (!matching("(^[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.+[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*@+(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?$)",email)){
            errors.put("username","Email ma nieprawidłowy format");
        }
        else if(accountRepository.checkIfUserWithThisMailExist(email))
            errors.put("username","Konto z tym mailem istnieje, zaloguj się na nie zamiast tworzyć nowe");
    }

    private void checkPassword(Map<String,String> errors, String password, String confirmPassword) {
        if (password == null || confirmPassword == null) {
            errors.put("password","Hasło powinno zawierać co najmniej 6 znaków");
        }
        else if (password.length() < 6) {
            errors.put("password","Hasło powinno zawierać co najmniej 6 znaków");
        }
        else if(!password.equals(confirmPassword)){
            errors.put("confirmPassword","Hasła nie pokrywają się");
        }
    }
    private void checkStreet(Map<String,String> errors, String street){
        if (!matching("(^[A-Z ÀÁÂÃÄÅ ĄŻŹ ÒÓÔÕÖØ Ł Ć ĘŚ Ń ÈÉÊË Ç ÌÍÎÏ ÙÚÛÜ Ñ]{1})([a-zàáâãäåąźżòóÓłćęśńôõöøèéêëçìíîïùúûüÿñ]{2,29}$)",street))
            errors.put("street","Nazwa ulicy musi zaczynać się z wielkiej litery i mieć co najmniej 3 znaki");
    }
    private void checkBuildingNumber(Map<String,String> errors, String buildingNumber){
        if (!matching("(^[0-9]{1,5}$)",buildingNumber))
            errors.put("buildingNumber","Numer budynku ma nieprawidłowy format");
    }

    private void checkFlatNumber(Map<String,String> errors, String flatNumber){
        if (!matching("(^[0-9]{0,5}$)",flatNumber))
            errors.put("flatNumber","Numer lokalu ma nieprawidłowy format");
    }

    private void checkZipCode(Map<String,String> errors, String zipCode){
        if (!matching("(^[0-9]{2}-?[0-9]{3}$)",zipCode))
            errors.put("zipCode","Kod pocztowy ma nieprawidłowy format");
    }
    private void checkCity(Map<String,String> errors, String city){
        if (!matching("(^[A-Z ÀÁÂÃÄÅ ĄŻŹ ÒÓÔÕÖØ Ł Ć ĘŚ Ń ÈÉÊË Ç ÌÍÎÏ ÙÚÛÜ Ñ ]{1})([a-zàáâãäåąźżòóÓłćęśńôõöøèéêëçìíîïùúûüÿñ]{2,29}$)",city))
            errors.put("city","Nazwa miasta musi zaczynać się z wielkiej litery i mieć co najmniej 3 znaki");
    }
    private void checkPhone(Map<String,String> errors, String phone){
        if (!matching("(^[0-9]{9,11}$)",phone))
            errors.put("phone","Number telefonu ma nieprawidłowy format");
    }
    /*private void checkCaptcha(Map<String,String> errors, String captcha){
        if(!captchaVerifyUtils.verify(captcha))
            errors.put("captcha","Nie przeszedłeś ochrony anty botowej");
    }*/

    private Boolean matching(String pattern, String regex){
        Pattern p = Pattern.compile(pattern);
        Matcher match = p.matcher(regex);
        return match.find();
    }
}