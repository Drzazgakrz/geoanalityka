package pl.gisexpert.rest.model;


@lombok.AllArgsConstructor
@lombok.Getter
@lombok.Setter
@lombok.NoArgsConstructor
@lombok.EqualsAndHashCode
@lombok.ToString
public class Attributes {
    private String Typ_zgloszenia;
    private String Zglaszajacy;
    private String Status;
    private String login;
}
