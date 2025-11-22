package se.spin.prototype.Beans;

public class Gender {
    private final GenderEnum id;
    private final String description;

    public Gender(GenderEnum id, String description) {
        this.id = id;
        this.description = description;
    }

    public GenderEnum getId() { return id; }
    public String getDescription() { return description; }
}
