package com.secondbrain.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.repository.config.EnableElasticsearchRepositories;

@Configuration
@EnableElasticsearchRepositories(basePackages = "com.secondbrain.elasticsearch")
public class ElasticsearchConfig {
}
