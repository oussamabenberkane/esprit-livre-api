package com.oussamabenberkane.espritlivre.service.dto;

import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingMethod;
import com.oussamabenberkane.espritlivre.domain.enumeration.ShippingProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;

/**
 * DTO for App User registration and profile management.
 */
public class AppUserDTO {

    private String id;

    private Instant createdDate;

    @Size(max = 50)
    private String login;

    @Size(max = 50)
    private String firstName;

    @NotNull
    @Size(max = 50)
    private String lastName;

    @Email
    @Size(min = 5, max = 254)
    private String email;

    @Size(min = 2, max = 10)
    private String langKey;

    @Size(max = 256)
    private String imageUrl;

    @NotNull
    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String wilaya;

    @Size(max = 100)
    private String city;

    @Size(max = 500)
    private String streetAddress;

    @Size(max = 10)
    private String postalCode;

    private ShippingMethod defaultShippingMethod;

    private ShippingProvider defaultShippingProvider;

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getLogin() {
        return login;
    }

    public void setLogin(String login) {
        this.login = login;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getLangKey() {
        return langKey;
    }

    public void setLangKey(String langKey) {
        this.langKey = langKey;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getWilaya() {
        return wilaya;
    }

    public void setWilaya(String wilaya) {
        this.wilaya = wilaya;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public ShippingMethod getDefaultShippingMethod() {
        return defaultShippingMethod;
    }

    public void setDefaultShippingMethod(ShippingMethod defaultShippingMethod) {
        this.defaultShippingMethod = defaultShippingMethod;
    }

    public ShippingProvider getDefaultShippingProvider() {
        return defaultShippingProvider;
    }

    public void setDefaultShippingProvider(ShippingProvider defaultShippingProvider) {
        this.defaultShippingProvider = defaultShippingProvider;
    }

    public Instant getCreatedDate() {
        return createdDate;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    @Override
    public String toString() {
        return "AppUserDTO{" +
            "id='" + id + '\'' +
            ", login='" + login + '\'' +
            ", firstName='" + firstName + '\'' +
            ", lastName='" + lastName + '\'' +
            ", email='" + email + '\'' +
            ", phone='" + phone + '\'' +
            ", wilaya='" + wilaya + '\'' +
            ", city='" + city + '\'' +
            '}';
    }
}
