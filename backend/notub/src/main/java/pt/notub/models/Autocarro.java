package pt.notub.models;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("Autocarro")
public class Autocarro extends Veiculo {

    public Autocarro() {}
}
