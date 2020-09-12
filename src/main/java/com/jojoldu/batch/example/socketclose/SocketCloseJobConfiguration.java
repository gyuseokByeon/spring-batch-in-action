package com.jojoldu.batch.example.socketclose;

import com.jojoldu.batch.entity.product.Store;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JdbcPagingItemReader;
import org.springframework.batch.item.database.PagingQueryProvider;
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder;
import org.springframework.batch.item.database.support.SqlPagingQueryProviderFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.BeanPropertyRowMapper;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jojoldu@gmail.com on 11/09/2020
 * Blog : http://jojoldu.tistory.com
 * Github : http://github.com/jojoldu
 */
@Slf4j
@RequiredArgsConstructor
@Configuration
public class SocketCloseJobConfiguration {
    private static final String BEAN_PREFIX = "socketClose";
    private static final int chunkSize = 1;

    private final JobBuilderFactory jobBuilderFactory;
    private final StepBuilderFactory stepBuilderFactory;
    private final DataSource dataSource; // DataSource DI

    @Bean(BEAN_PREFIX+"_job")
    public Job job() throws Exception {
        return jobBuilderFactory.get(BEAN_PREFIX+"_job")
                .start(step())
                .build();
    }

    @Bean(BEAN_PREFIX+"_step")
    public Step step() throws Exception {
        return stepBuilderFactory.get(BEAN_PREFIX+"_step")
                .<Store, Store>chunk(chunkSize)
                .reader(reader())
                .writer(writer())
                .build();
    }

    @Bean(BEAN_PREFIX+"_reader")
    public JdbcPagingItemReader<Store> reader() throws Exception {
        Map<String, Object> parameterValues = new HashMap<>();
        parameterValues.put("name", "jojoldu");

        return new JdbcPagingItemReaderBuilder<Store>()
                .pageSize(chunkSize)
                .fetchSize(chunkSize)
                .dataSource(dataSource)
                .rowMapper(new BeanPropertyRowMapper<>(Store.class))
                .queryProvider(createQueryProvider())
                .parameterValues(parameterValues)
                .name(BEAN_PREFIX+"_reader")
                .build();
    }

    private ItemWriter<Store> writer() {
        return list -> {
            for (Store store: list) {
                log.info("Current Store={}", store);
            }
        };
    }

    @Bean
    public PagingQueryProvider createQueryProvider() throws Exception {
        SqlPagingQueryProviderFactoryBean queryProvider = new SqlPagingQueryProviderFactoryBean();
        queryProvider.setDataSource(dataSource);
        queryProvider.setSelectClause("id, name");
        queryProvider.setFromClause("FROM store");
        queryProvider.setWhereClause("WHERE name=:name AND SLEEP(121)=0"); // 1개 조회시마다 sleep(61) => 즉, 61초
        queryProvider.setSortKey("id");

        return queryProvider.getObject();
    }
}