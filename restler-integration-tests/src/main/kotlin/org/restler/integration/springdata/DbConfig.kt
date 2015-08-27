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
import org.springframework.orm.jpa.JpaTransactionManager
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean
import org.springframework.orm.jpa.vendor.Database
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter
import javax.sql.DataSource

@Configuration
open class DbConfig {

    private fun databasePopulator(): DatabasePopulator {
        val populator = ResourceDatabasePopulator()

        val schema = ByteArrayResource("CREATE TABLE Persons(id INT PRIMARY KEY, firstName VARCHAR(255), lastName VARCHAR(255));".toByteArray("UTF-8"))
        val data = ByteArrayResource("""INSERT INTO person (id, name) VALUES ('0', 'test name');
                                        INSERT INTO person (id, name) VALUES ('1', 'test name');""".toByteArray("UTF-8"))

        populator.addScript(schema)
        populator.addScript(data)
        return populator
    }

    private fun databaseCleaner(): DatabasePopulator {
        val H2_CLEANER_SCRIPT: Resource = ByteArrayResource("DROP TABLE Persons;".toByteArray("UTF-8"))
        val populator = ResourceDatabasePopulator()
        populator.addScript(H2_CLEANER_SCRIPT)
        return populator
    }

    Bean open fun dataSourceInitializer(dataSource: DataSource): DataSourceInitializer {
        val initializer = DataSourceInitializer()
        initializer.setDataSource(dataSource)
        initializer.setDatabasePopulator(databasePopulator())
        initializer.setDatabaseCleaner(databaseCleaner())
        return initializer
    }

    Bean open fun dataSource(): DataSource =
            EmbeddedDatabaseBuilder().setType(EmbeddedDatabaseType.H2).build()

    @Bean open fun entityManagerFactory(): LocalContainerEntityManagerFactoryBean {
        val vendorAdapter = HibernateJpaVendorAdapter()
        vendorAdapter.setDatabase(Database.H2)
        vendorAdapter.setGenerateDdl(true)

        val factory = LocalContainerEntityManagerFactoryBean()
        factory.setJpaVendorAdapter(vendorAdapter)
        factory.setPackagesToScan(this.javaClass.getPackage().getName())
        factory.setDataSource(dataSource())

        return factory
    }

    @Bean open fun transactionManager() = JpaTransactionManager()
}

