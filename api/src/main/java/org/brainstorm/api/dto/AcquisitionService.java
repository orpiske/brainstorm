package org.brainstorm.api.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Table(name = "acquisition_service")
@Entity
public class AcquisitionService {

    @Id
    @SequenceGenerator(
            name = "acquisition_service_id_seq",
            sequenceName = "acquisition_service_id_seq",
            allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "acquisition_service_id_seq")
    private int id;

    @Column
    private String name;
    @Column
    private String gav;
    @Column
    private String description;
    @Column
    private State state;
    @Column
    private String path;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGav() {
        return gav;
    }

    public void setGav(String gav) {
        this.gav = gav;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public State getState() {
        return state;
    }

    public void setState(State state) {
        this.state = state;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }
}
