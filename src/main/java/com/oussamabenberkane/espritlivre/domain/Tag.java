package com.oussamabenberkane.espritlivre.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.oussamabenberkane.espritlivre.domain.enumeration.TagType;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * A Tag.
 */
@Entity
@Table(name = "tag")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Tag implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "tagSequenceGenerator")
    @SequenceGenerator(name = "tagSequenceGenerator", sequenceName = "tag_seq", allocationSize = 1)
    @Column(name = "id")
    private Long id;

    @Column(name = "name_en")
    private String nameEn;

    @Column(name = "name_fr")
    private String nameFr;

    @Enumerated(EnumType.STRING)
    @Column(name = "type")
    private TagType type;

    @Column(name = "active")
    private Boolean active;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "rel_tag__book", joinColumns = @JoinColumn(name = "tag_id"), inverseJoinColumns = @JoinColumn(name = "book_id"))
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "tags" }, allowSetters = true)
    private Set<Book> books = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Tag id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNameEn() {
        return this.nameEn;
    }

    public Tag nameEn(String nameEn) {
        this.setNameEn(nameEn);
        return this;
    }

    public void setNameEn(String nameEn) {
        this.nameEn = nameEn;
    }

    public String getNameFr() {
        return this.nameFr;
    }

    public Tag nameFr(String nameFr) {
        this.setNameFr(nameFr);
        return this;
    }

    public void setNameFr(String nameFr) {
        this.nameFr = nameFr;
    }

    public TagType getType() {
        return this.type;
    }

    public Tag type(TagType type) {
        this.setType(type);
        return this;
    }

    public void setType(TagType type) {
        this.type = type;
    }

    public Boolean getActive() {
        return this.active;
    }

    public Tag active(Boolean active) {
        this.setActive(active);
        return this;
    }

    public void setActive(Boolean active) {
        this.active = active;
    }

    public Set<Book> getBooks() {
        return this.books;
    }

    public void setBooks(Set<Book> books) {
        this.books = books;
    }

    public Tag books(Set<Book> books) {
        this.setBooks(books);
        return this;
    }

    public Tag addBook(Book book) {
        this.books.add(book);
        return this;
    }

    public Tag removeBook(Book book) {
        this.books.remove(book);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Tag)) {
            return false;
        }
        return getId() != null && getId().equals(((Tag) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Tag{" +
            "id=" + getId() +
            ", nameEn='" + getNameEn() + "'" +
            ", nameFr='" + getNameFr() + "'" +
            ", type='" + getType() + "'" +
            ", active='" + getActive() + "'" +
            "}";
    }
}
