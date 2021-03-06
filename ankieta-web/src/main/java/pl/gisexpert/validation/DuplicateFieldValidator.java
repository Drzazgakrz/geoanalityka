package pl.gisexpert.validation;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;
import javax.faces.validator.FacesValidator;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

@FacesValidator(value = "duplicateFieldValidator")
public class DuplicateFieldValidator implements Validator {

    @Override
    public void validate(FacesContext context, UIComponent component, Object value)
            throws ValidatorException {
        // Obtain the client ID of the first field from f:attribute.
        String field1Id = (String) component.getAttributes().get("field1Id");

        // Find the actual JSF component for the client ID.
        UIInput textInput = (UIInput) context.getViewRoot().findComponent(field1Id);
        if (textInput == null) {
            throw new IllegalArgumentException(String.format("Unable to find component with id %s", field1Id));
        }
        // Get its value, the entered text of the first field.
        String field1 = (String) textInput.getValue();

        // Cast the value of the entered text of the second field back to String.
        String confirm = (String) value;

        // Check if the first text is actually entered and compare it with second text.
        if (field1 != null && field1.length() != 0 && !field1.equals(confirm)) {
            FacesMessage message = new FacesMessage("Podane wartości nie są równe");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }
}
