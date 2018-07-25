package pl.gisexpert.rest.model;

@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.ToString
public class SubmitFormData {
    private String token;
    private String data;
}
