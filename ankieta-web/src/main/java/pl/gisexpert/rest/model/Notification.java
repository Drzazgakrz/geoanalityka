package pl.gisexpert.rest.model;

@lombok.Setter
@lombok.Getter
@lombok.AllArgsConstructor
public class Notification {
    private String globalId;
    private int objectId;
    private boolean success;
    private String token;
}
