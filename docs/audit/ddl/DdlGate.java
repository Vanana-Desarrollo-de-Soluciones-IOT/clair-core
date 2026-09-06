import jakarta.persistence.Converter;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.MappedSuperclass;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;

import java.io.File;
import java.lang.annotation.Annotation;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;

public class DdlGate {
    @SuppressWarnings("unchecked")
    public static void main(String[] args) throws Exception {
        Path classes = Paths.get(args[0]);
        Path out = Paths.get(args[1]);
        Files.deleteIfExists(out);
        StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect")
                .applySetting("hibernate.boot.allow_jdbc_metadata_access", "false")
                .applySetting("hibernate.physical_naming_strategy",
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .applySetting("hibernate.implicit_naming_strategy",
                        "org.springframework.boot.orm.jpa.hibernate.SpringImplicitNamingStrategy")
                .applySetting("jakarta.persistence.schema-generation.scripts.action", "create")
                .applySetting("jakarta.persistence.schema-generation.scripts.create-target", out.toString())
                .build();
        MetadataSources sources = new MetadataSources(registry);
        List<Class<? extends Annotation>> mapped = List.of(Entity.class, MappedSuperclass.class, Embeddable.class, Converter.class);
        List<String> names;
        try (Stream<Path> s = Files.walk(classes)) {
            names = s.filter(p -> p.toString().endsWith(".class"))
                    .map(p -> classes.relativize(p).toString().replace(File.separatorChar, '.').replaceAll("\\.class$", ""))
                    .sorted().collect(Collectors.toList());
        }
        int n = 0;
        for (String name : names) {
            Class<?> c;
            try { c = Class.forName(name, false, DdlGate.class.getClassLoader()); }
            catch (Throwable t) { continue; }
            if (mapped.stream().anyMatch(c::isAnnotationPresent)) { sources.addAnnotatedClass(c); n++; }
        }
        System.err.println("mapped classes: " + n);
        sources.buildMetadata().buildSessionFactory().close();
    }
}
