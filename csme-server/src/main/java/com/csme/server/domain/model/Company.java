package com.csme.server.domain.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

/** Entreprise cliente de la plateforme. Toutes les données métier lui sont rattachées. */
@Entity
@Table(name = "companies")
public class Company extends BaseEntity {

    @Column(nullable = false, length = 32, updatable = false)
    private String code;

    @Column(nullable = false, length = 160)
    private String name;

    @Column(name = "legal_name", length = 200)
    private String legalName;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 2)
    private String country;

    @Column(nullable = false)
    private boolean active = true;

    @Column(length = 255)
    private String address;

    @Column(length = 120)
    private String city;

    @Column(length = 40)
    private String phone;

    @Column(length = 254)
    private String email;

    @Column(length = 255)
    private String website;

    @Column(name = "tax_id", length = 64)
    private String taxId;

    @Column(name = "trade_register", length = 64)
    private String tradeRegister;

    protected Company() {
    }

    public Company(String code, String name, String legalName, String currency, String country) {
        this.code = code;
        this.name = name;
        this.legalName = legalName;
        this.currency = currency;
        this.country = country;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getLegalName() {
        return legalName;
    }

    public String getCurrency() {
        return currency;
    }

    public String getCountry() {
        return country;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    /** Met à jour les informations générales. Le code, la devise et le pays restent inchangés. */
    public void updateProfile(String name, String legalName, String address, String city, String phone, String email,
                              String website, String taxId, String tradeRegister) {
        this.name = name;
        this.legalName = legalName;
        this.address = address;
        this.city = city;
        this.phone = phone;
        this.email = email;
        this.website = website;
        this.taxId = taxId;
        this.tradeRegister = tradeRegister;
    }

    public String getAddress() {
        return address;
    }

    public String getCity() {
        return city;
    }

    public String getPhone() {
        return phone;
    }

    public String getEmail() {
        return email;
    }

    public String getWebsite() {
        return website;
    }

    public String getTaxId() {
        return taxId;
    }

    public String getTradeRegister() {
        return tradeRegister;
    }
}
