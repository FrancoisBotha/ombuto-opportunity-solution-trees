package com.opportunity.tree.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

/**
 * Name + URL link on any tree node. Exactly one node relationship is set.
 */
@Entity
@Table(name = "node_link")
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@SuppressWarnings("common-java:DuplicatedBlocks")
public class NodeLink implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "sequenceGenerator")
    @SequenceGenerator(name = "sequenceGenerator")
    @Column(name = "id")
    private Long id;

    @NotNull
    @Size(min = 1, max = 100)
    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @NotNull
    @Size(max = 2000)
    @Pattern(regexp = "^https?:\\/\\/.+")
    @Column(name = "url", length = 2000, nullable = false)
    private String url;

    @NotNull
    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @NotNull
    @Column(name = "created_date", nullable = false)
    private Instant createdDate;

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

    public NodeLink id(Long id) {
        this.setId(id);
        return this;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public NodeLink name(String name) {
        this.setName(name);
        return this;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return this.url;
    }

    public NodeLink url(String url) {
        this.setUrl(url);
        return this;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Integer getSortOrder() {
        return this.sortOrder;
    }

    public NodeLink sortOrder(Integer sortOrder) {
        this.setSortOrder(sortOrder);
        return this;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public Instant getCreatedDate() {
        return this.createdDate;
    }

    public NodeLink createdDate(Instant createdDate) {
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

    public NodeLink product(Product product) {
        this.setProduct(product);
        return this;
    }

    public Outcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(Outcome outcome) {
        this.outcome = outcome;
    }

    public NodeLink outcome(Outcome outcome) {
        this.setOutcome(outcome);
        return this;
    }

    public Opportunity getOpportunity() {
        return this.opportunity;
    }

    public void setOpportunity(Opportunity opportunity) {
        this.opportunity = opportunity;
    }

    public NodeLink opportunity(Opportunity opportunity) {
        this.setOpportunity(opportunity);
        return this;
    }

    public Solution getSolution() {
        return this.solution;
    }

    public void setSolution(Solution solution) {
        this.solution = solution;
    }

    public NodeLink solution(Solution solution) {
        this.setSolution(solution);
        return this;
    }

    public Assumption getAssumption() {
        return this.assumption;
    }

    public void setAssumption(Assumption assumption) {
        this.assumption = assumption;
    }

    public NodeLink assumption(Assumption assumption) {
        this.setAssumption(assumption);
        return this;
    }

    public Evidence getEvidence() {
        return this.evidence;
    }

    public void setEvidence(Evidence evidence) {
        this.evidence = evidence;
    }

    public NodeLink evidence(Evidence evidence) {
        this.setEvidence(evidence);
        return this;
    }

    // jhipster-needle-entity-add-getters-setters - JHipster will add getters and setters here

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof NodeLink)) {
            return false;
        }
        return getId() != null && getId().equals(((NodeLink) o).getId());
    }

    @Override
    public int hashCode() {
        // see https://vladmihalcea.com/how-to-implement-equals-and-hashcode-using-the-jpa-entity-identifier/
        return getClass().hashCode();
    }

    // prettier-ignore
    @Override
    public String toString() {
        return "NodeLink{" +
            "id=" + getId() +
            ", name='" + getName() + "'" +
            ", url='" + getUrl() + "'" +
            ", sortOrder=" + getSortOrder() +
            ", createdDate='" + getCreatedDate() + "'" +
            "}";
    }
}
