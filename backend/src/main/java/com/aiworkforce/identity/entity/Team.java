package com.aiworkforce.identity.entity;
import com.aiworkforce.core.base.AuditableEntity;
import com.aiworkforce.identity.entity.Organization;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = ""team"")
@Getter
@Setter
public class Team extends AuditableEntity {
    private String name;
    private String description;
    
    @ManyToOne
    @JoinColumn(name = ""organization_id"")
    private Organization organization;
    
    @ManyToOne
    @JoinColumn(name = ""manager_id"") // Lazy reference to Employee via ID
    private com.aiworkforce.identity.entity.Employee manager;
}
