package org.restler.integration.springdata

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.datasource.init.DataSourceInitializer
import org.springframework.jdbc.datasource.init.DatabasePopulator
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import javax.sql.DataSource

Configuration
open class DbConfig {

    private fun databasePopulator(): DatabasePopulator {
        val populator = ResourceDatabasePopulator();

        val H2_SCHEMA_SCRIPT: Resource = ByteArrayResource("CREATE TABLE Persons(id INT PRIMARY KEY, firstName VARCHAR(255), lastName VARCHAR(255));".toByteArray("UTF-8"));

        populator.addScript(H2_SCHEMA_SCRIPT);
        //populator.addScript(H2_DATA_SCRIPT);
        return populator;
    }

    private fun databaseCleaner(): DatabasePopulator {
        val H2_CLEANER_SCRIPT: Resource = ByteArrayResource("DROP TABLE Persons;".toByteArray("UTF-8"));
        val populator = ResourceDatabasePopulator();
        populator.addScript(H2_CLEANER_SCRIPT);
        return populator;
    }

    Bean open fun dataSourceInitializer(dataSource: DataSource): DataSourceInitializer {
        val initializer = DataSourceInitializer();
        initializer.setDataSource(dataSource);
        initializer.setDatabasePopulator(databasePopulator());
        initializer.setDatabaseCleaner(databaseCleaner());
        return initializer;
    }

    Bean open fun dataSource(): DataSource =
            EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()


}

