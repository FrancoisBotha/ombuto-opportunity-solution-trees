package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Weekly customer interview or other evidence source; opportunities are mined from these.
 */
@Entity
@Table(name = "interview")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class Interview implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 2, max = 200)
    @Column(name = "title", length = 200, nullable = false)
    private String title;

    @Size(max = 200)
    @Column(name = "participant", length = 200)
    private String participant;

    @NotNull
    @Column(name = "interview_date", nullable = false)
    private LocalDate interviewDate;

    @Lob
    @Column(name = "notes")
    private String notes;

    @Size(max = 2000)
    @Column(name = "recording_url", length = 2000)
    private String recordingUrl;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @ManyToOne(optional = false)
    @NotNull
    @JsonIgnoreProperties(value = { "team" }, allowSetters = true)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    private User interviewer;

    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "interviews")
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Set<Opportunity> opportunities = new HashSet<>();

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public Interview id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public Interview title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getParticipant() {
        return this.participant;
    }

    public Interview participant(String participant) {
        this.setParticipant(participant);
        return this;
    }

    public void setParticipant(String participant) {
        this.participant = participant;
    }

    public LocalDate getInterviewDate() {
        return this.interviewDate;
    }

    public Interview interviewDate(LocalDate interviewDate) {
        this.setInterviewDate(interviewDate);
        return this;
    }

    public void setInterviewDate(LocalDate interviewDate) {
        this.interviewDate = interviewDate;
    }

    public String getNotes() {
        return this.notes;
    }

    public Interview notes(String notes) {
        this.setNotes(notes);
        return this;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getRecordingUrl() {
        return this.recordingUrl;
    }

    public Interview recordingUrl(String recordingUrl) {
        this.setRecordingUrl(recordingUrl);
        return this;
    }

    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public Interview createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public Interview product(Product product) {
        this.setProduct(product);
        return this;
    }

    public User getInterviewer() {
        return this.interviewer;
    }

    public void setInterviewer(User user) {
        this.interviewer = user;
    }

    public Interview interviewer(User user) {
        this.setInterviewer(user);
        return this;
    }

    public Set<Opportunity> getOpportunities() {
        return this.opportunities;
    }

    public void setOpportunities(Set<Opportunity> opportunities) {
        if (this.opportunities != null) {
            this.opportunities.forEach(i -> i.removeInterview(this));
        }
        if (opportunities != null) {
            opportunities.forEach(i -> i.addInterview(this));
        }
        this.opportunities = opportunities;
    }

    public Interview opportunities(Set<Opportunity> opportunities) {
        this.setOpportunities(opportunities);
        return this;
    }

    public Interview addOpportunity(Opportunity opportunity) {
        this.opportunities.add(opportunity);
        opportunity.getInterviews().add(this);
        return this;
    }

    public Interview removeOpportunity(Opportunity opportunity) {
        this.opportunities.remove(opportunity);
        opportunity.getInterviews().remove(this);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Interview)) {
            return false;
        }
        return getId() != null && getId().equals(((Interview) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "Interview{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", participant='" + getParticipant() + "'" +
            ", interviewDate='" + getInterviewDate() + "'" +
            ", notes='" + getNotes() + "'" +
            ", recordingUrl='" + getRecordingUrl() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
