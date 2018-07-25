package pl.gisexpert.rest.model;


@lombok.Getter
@lombok.Setter
@lombok.EqualsAndHashCode
@lombok.NoArgsConstructor
@lombok.AllArgsConstructor
@lombok.ToString
public class FormToArcgis {
    private Attributes attributes;
    private Geometry geometry;
    private Object symbol;
    private Object infoTemplate;
}
