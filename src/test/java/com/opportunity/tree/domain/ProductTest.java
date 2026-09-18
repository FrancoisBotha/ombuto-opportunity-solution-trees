package com.opportunity.tree.domain;

import static com.opportunity.tree.domain.ProductTestSamples.*;
import static com.opportunity.tree.domain.TeamTestSamples.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.opportunity.tree.web.rest.TestUtil;
import org.junit.jupiter.api.Test;

class ProductTest {

    @Test
    void equalsVerifier() throws Exception {
        TestUtil.equalsVerifier(Product.class);
        Product product1 = getProductSample1();
        Product product2 = new Product();
        assertThat(product1).isNotEqualTo(product2);

        product2.setId(product1.getId());
        assertThat(product1).isEqualTo(product2);

        product2 = getProductSample2();
        assertThat(product1).isNotEqualTo(product2);
    }

    @Test
    void teamTest() {
        Product product = getProductRandomSampleGenerator();
        Team teamBack = getTeamRandomSampleGenerator();

        product.setTeam(teamBack);
        assertThat(product.getTeam()).isEqualTo(teamBack);

        product.team(null);
        assertThat(product.getTeam()).isNull();
    }
}
