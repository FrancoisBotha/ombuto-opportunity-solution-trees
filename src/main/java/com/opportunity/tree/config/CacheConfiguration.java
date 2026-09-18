package com.opportunity.tree.config;

import java.time.Duration;
import org.ehcache.config.builders.*;
import org.ehcache.jsr107.Eh107Configuration;
import org.hibernate.cache.jcache.ConfigSettings;
import org.springframework.boot.cache.autoconfigure.JCacheManagerCustomizer;
import org.springframework.boot.hibernate.autoconfigure.HibernatePropertiesCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tech.jhipster.config.JHipsterProperties;

@Configuration
@EnableCaching
public class CacheConfiguration {

    private final javax.cache.configuration.Configuration<Object, Object> jcacheConfiguration;

    public CacheConfiguration(JHipsterProperties jHipsterProperties) {
        var ehcache = jHipsterProperties.getCache().getEhcache();

        jcacheConfiguration = Eh107Configuration.fromEhcacheCacheConfiguration(
            CacheConfigurationBuilder.newCacheConfigurationBuilder(
                Object.class,
                Object.class,
                ResourcePoolsBuilder.heap(ehcache.getMaxEntries())
            )
                .withExpiry(ExpiryPolicyBuilder.timeToLiveExpiration(Duration.ofSeconds(ehcache.getTimeToLiveSeconds())))
                .build()
        );
    }

    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer(javax.cache.CacheManager cacheManager) {
        return hibernateProperties -> hibernateProperties.put(ConfigSettings.CACHE_MANAGER, cacheManager);
    }

    @Bean
    public JCacheManagerCustomizer cacheManagerCustomizer() {
        return cm -> {
            createCache(cm, com.opportunity.tree.repository.UserRepository.USERS_BY_LOGIN_CACHE);
            createCache(cm, com.opportunity.tree.repository.UserRepository.USERS_BY_EMAIL_CACHE);
            createCache(cm, com.opportunity.tree.domain.User.class.getName());
            createCache(cm, com.opportunity.tree.domain.Authority.class.getName());
            createCache(cm, com.opportunity.tree.domain.User.class.getName() + ".authorities");
            createCache(cm, com.opportunity.tree.domain.Team.class.getName());
            createCache(cm, com.opportunity.tree.domain.TeamMember.class.getName());
            createCache(cm, com.opportunity.tree.domain.Product.class.getName());
            createCache(cm, com.opportunity.tree.domain.Outcome.class.getName());
            createCache(cm, com.opportunity.tree.domain.Opportunity.class.getName());
            createCache(cm, com.opportunity.tree.domain.Opportunity.class.getName() + ".interviews");
            createCache(cm, com.opportunity.tree.domain.Opportunity.class.getName() + ".tags");
            createCache(cm, com.opportunity.tree.domain.OpportunityLink.class.getName());
            createCache(cm, com.opportunity.tree.domain.Solution.class.getName());
            createCache(cm, com.opportunity.tree.domain.Solution.class.getName() + ".tags");
            createCache(cm, com.opportunity.tree.domain.SolutionLink.class.getName());
            createCache(cm, com.opportunity.tree.domain.Assumption.class.getName());
            createCache(cm, com.opportunity.tree.domain.Assumption.class.getName() + ".experiments");
            createCache(cm, com.opportunity.tree.domain.Experiment.class.getName());
            createCache(cm, com.opportunity.tree.domain.Experiment.class.getName() + ".assumptions");
            createCache(cm, com.opportunity.tree.domain.Interview.class.getName());
            createCache(cm, com.opportunity.tree.domain.Interview.class.getName() + ".opportunities");
            createCache(cm, com.opportunity.tree.domain.Comment.class.getName());
            createCache(cm, com.opportunity.tree.domain.Tag.class.getName());
            createCache(cm, com.opportunity.tree.domain.Tag.class.getName() + ".opportunities");
            createCache(cm, com.opportunity.tree.domain.Tag.class.getName() + ".solutions");
            // jhipster-needle-ehcache-add-entry
        };
    }

    private void createCache(javax.cache.CacheManager cm, String cacheName) {
        javax.cache.Cache<Object, Object> cache = cm.getCache(cacheName);
        if (cache != null) {
            cache.clear();
        } else {
            cm.createCache(cacheName, jcacheConfiguration);
        }
    }
}
