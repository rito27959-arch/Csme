package com.csme.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "agencies")
public class Agency extends TenantOwnedEntity {

    @Column(nullable = false, length = 32)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(length = 120)
    private String city;

    @Column(length = 40)
    private String phone;

    @Column(nullable = false)
    private boolean active = true;

    protected Agency() {
    }

    public Agency(UUID companyId, String code, String name, String city, String phone) {
        super(companyId);
        update(code, name, city, phone);
    }

    public final void update(String code, String name, String city, String phone) {
        this.code = code;
        this.name = name;
        this.city = city;
        this.phone = phone;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
