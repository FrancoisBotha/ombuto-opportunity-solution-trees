package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.opportunity.tree.domain.enumeration.MeetingTranscriptSource;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Transcript of a meeting attached to exactly one tree node (Epic 12).
 */
@Entity
@Table(name = "meeting_transcript")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class MeetingTranscript implements Serializable {

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

    @NotNull
    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Size(max = 500)
    @Column(name = "attendees", length = 500)
    private String attendees;

    @Lob
    @Column(name = "body", nullable = false)
    private String body;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private MeetingTranscriptSource source;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

    @Column(name = "edited_date")
    private Instant editedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    private User author;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "team" }, allowSetters = true)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "product", "owner" }, allowSetters = true)
    private Outcome outcome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "outcome", "parent", "owner", "interviews", "tags" }, allowSetters = true)
    private Opportunity opportunity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "opportunity", "owner", "tags" }, allowSetters = true)
    private Solution solution;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "solution", "owner" }, allowSetters = true)
    private Assumption assumption;

    @ManyToOne(fetch = FetchType.LAZY)
    @JsonIgnoreProperties(value = { "opportunity", "assumption" }, allowSetters = true)
    private Evidence evidence;

    // jhipster-needle-entity-add-field - JHipster will add fields here

    public Long getId() {
        return this.id;
    }

    public MeetingTranscript id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public MeetingTranscript title(String title) {
        this.setTitle(title);
        return this;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getMeetingDate() {
        return this.meetingDate;
    }

    public MeetingTranscript meetingDate(LocalDate meetingDate) {
        this.setMeetingDate(meetingDate);
        return this;
    }

    public void setMeetingDate(LocalDate meetingDate) {
        this.meetingDate = meetingDate;
    }

    public String getAttendees() {
        return this.attendees;
    }

    public MeetingTranscript attendees(String attendees) {
        this.setAttendees(attendees);
        return this;
    }

    public void setAttendees(String attendees) {
        this.attendees = attendees;
    }

    public String getBody() {
        return this.body;
    }

    public MeetingTranscript body(String body) {
        this.setBody(body);
        return this;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public MeetingTranscriptSource getSource() {
        return this.source;
    }

    public MeetingTranscript source(MeetingTranscriptSource source) {
        this.setSource(source);
        return this;
    }

    public void setSource(MeetingTranscriptSource source) {
        this.source = source;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public MeetingTranscript createdDate(Instant createdDate) {
        this.setCreatedDate(createdDate);
        return this;
    }

    public void setCreatedDate(Instant createdDate) {
        this.createdDate = createdDate;
    }

    public Instant getEditedDate() {
        return this.editedDate;
    }

    public MeetingTranscript editedDate(Instant editedDate) {
        this.setEditedDate(editedDate);
        return this;
    }

    public void setEditedDate(Instant editedDate) {
        this.editedDate = editedDate;
    }

    public User getAuthor() {
        return this.author;
    }

    public void setAuthor(User user) {
        this.author = user;
    }

    public MeetingTranscript author(User user) {
        this.setAuthor(user);
        return this;
    }

    public Product getProduct() {
        return this.product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    public MeetingTranscript product(Product product) {
        this.setProduct(product);
        return this;
    }

    public Outcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public MeetingTranscript outcome(Outcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public Opportunity getOpportunity() {
        return this.opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public MeetingTranscript opportunity(Opportunity opportunity) {
        this.setOpportunity(opportunity);
        return this;
    }

    public Solution getSolution() {
        return this.solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
    }

    public MeetingTranscript solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public Assumption getAssumption() {
        return this.assumption;
    }

    public void setAssumption(Assumption assumption) {
        this.assumption = assumption;
    }

    public MeetingTranscript assumption(Assumption assumption) {
        this.setAssumption(assumption);
        return this;
    }

    public Evidence getEvidence() {
        return this.evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public MeetingTranscript evidence(Evidence evidence) {
        this.setEvidence(evidence);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof MeetingTranscript)) {
            return false;
        }
        return getId() != null && getId().equals(((MeetingTranscript) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "MeetingTranscript{" +
            "id=" + getId() +
            ", title='" + getTitle() + "'" +
            ", meetingDate='" + getMeetingDate() + "'" +
            ", attendees='" + getAttendees() + "'" +
            ", body='" + getBody() + "'" +
            ", source='" + getSource() + "'" +
            ", createdDate='" + getCreatedDate() + "'" +
            ", editedDate='" + getEditedDate() + "'" +
            "}";
    }
}
