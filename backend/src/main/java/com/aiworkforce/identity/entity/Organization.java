package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "organization")
@Getter
@Setter
public class Organization extends AuditableEntity {
    private String name;
    private String domain;
    private String logoUrl;
    
    private Double latitude;
    private Double longitude;
    private Integer allowedRadiusMeters;
    private Integer currentSprintNumber;
}
