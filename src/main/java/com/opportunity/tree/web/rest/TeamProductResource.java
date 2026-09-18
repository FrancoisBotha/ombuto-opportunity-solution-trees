package com.opportunity.tree.web.rest;

import com.opportunity.tree.service.ProductService;
import com.opportunity.tree.service.dto.ProductDTO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing team-scoped product endpoints (FR-006, FR-007).
 * The current user must be a member of the team (any role) to list its
 * products; the response includes archived products, each carrying the
 * {@code archived} flag so the UI can render them as such.
 */
@RestController
@RequestMapping("/api/teams")
public class TeamProductResource {

    private static final Logger LOG = LoggerFactory.getLogger(TeamProductResource.class);

    private final ProductService productService;

    public TeamProductResource(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{teamId}/products")
    public List<ProductDTO> getAllProductsByTeam(@PathVariable("teamId") Long teamId) {
        LOG.debug("REST request to get Products for team {}", teamId);
        return productService.findAllByTeam(teamId);
    }
}
