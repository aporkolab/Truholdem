package com.truholdem.architecture;

import com.tngtech.archunit.base.DescribedPredicate;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaModifier;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.library.Architectures;
import com.tngtech.archunit.library.dependencies.SlicesRuleDefinition;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.persistence.Entity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;
import static com.tngtech.archunit.library.Architectures.layeredArchitecture;


@DisplayName("TruHoldem Architecture Tests")
class ArchitectureTest {

    private static final String BASE_PACKAGE = "com.truholdem";
    
    private static JavaClasses importedClasses;

    @BeforeAll
    static void setUp() {
        
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages(BASE_PACKAGE);
    }

    
    
    
    
    @Nested
    @DisplayName("1. Layer Dependency Rules")
    class LayerDependencyRules {

        
        @Test
        @DisplayName("Controllers should only call Services, not Repositories directly")
        void controllersShouldOnlyCallServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..controller..")
                    .should().dependOnClassesThat().resideInAPackage("..repository..")
                    .because("Controllers should delegate business logic to Services, not access data layer directly");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Services should not depend on Controllers")
        void servicesShouldNotDependOnControllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..service..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Services should not know about the presentation layer - this would create circular dependencies");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Repositories should not call Services")
        void repositoriesShouldNotCallServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..service..")
                    .because("Repositories should not depend on Services - they are the lowest layer");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Repositories should not depend on Controllers")
        void repositoriesShouldNotDependOnControllers() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..repository..")
                    .should().dependOnClassesThat().resideInAPackage("..controller..")
                    .because("Repositories must remain isolated from presentation layer");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Model classes should not depend on Services or Controllers")
        void modelClassesShouldBeIndependent() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..model..")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..repository..")
                    .because("Entity classes should be POJOs with JPA annotations only");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Domain classes should not depend on infrastructure layers")
        void domainClassesShouldBeIndependent() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..domain..")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..controller..", "..repository..")
                    .because("Domain layer should be independent from infrastructure - it can only depend on model and itself");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("DTO classes should be independent from business logic")
        void dtoClassesShouldBeIndependent() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..dto..")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..repository..", "..controller..")
                    .because("DTOs should be simple data transfer objects without business logic");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Layered architecture should be properly maintained")
        void layeredArchitectureShouldBeRespected() {
            Architectures.LayeredArchitecture layeredArchitecture = layeredArchitecture()
                    .consideringAllDependencies()
                    .layer("Controller").definedBy("..controller..")
                    .layer("Service").definedBy("..service..")
                    .layer("Repository").definedBy("..repository..")
                    .layer("Model").definedBy("..model..")
                    .layer("DTO").definedBy("..dto..")
                    .layer("Config").definedBy("..config..")
                    .layer("Security").definedBy("..security..")
                    .layer("Exception").definedBy("..exception..")
                    .layer("Handler").definedBy("..handler..")
                    .layer("Mapper").definedBy("..mapper..")
                    .layer("Domain").definedBy("..domain..")
                    .layer("Application").definedBy("..application..")
                    .layer("WebSocket").definedBy("..websocket..")
                    .layer("Listener").definedBy("..listener..")

                    .whereLayer("Controller").mayNotBeAccessedByAnyLayer()
                    .whereLayer("Service").mayOnlyBeAccessedByLayers("Controller", "Service", "Config", "Security", "Handler", "Application", "WebSocket")
                    .whereLayer("Repository").mayOnlyBeAccessedByLayers("Service", "Config", "Security")
                    .whereLayer("Model").mayOnlyBeAccessedByLayers("Controller", "Service", "Repository", "DTO", "Config", "Security", "Handler", "Mapper", "Domain", "Application", "WebSocket")
                    .whereLayer("DTO").mayOnlyBeAccessedByLayers("Controller", "Service", "Config", "Handler", "Mapper", "WebSocket")
                    .whereLayer("Exception").mayOnlyBeAccessedByLayers("Controller", "Service", "Config", "Handler", "Domain")
                    .whereLayer("Domain").mayOnlyBeAccessedByLayers("Controller", "Service", "Config", "Handler", "Application", "Exception")
                    .whereLayer("Application").mayOnlyBeAccessedByLayers("Controller", "Config")
                    .whereLayer("Mapper").mayOnlyBeAccessedByLayers("Service", "Controller", "Config")
                    .whereLayer("WebSocket").mayOnlyBeAccessedByLayers("Controller", "Config", "Listener", "Application")
                    .whereLayer("Listener").mayOnlyBeAccessedByLayers("Config");

            layeredArchitecture.check(importedClasses);
        }

        
        @Test
        @DisplayName("Exception classes should not depend on Services")
        void exceptionsShouldNotDependOnServices() {
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..exception..")
                    .and().haveSimpleNameNotContaining("Handler")
                    .should().dependOnClassesThat().resideInAnyPackage("..service..", "..repository..")
                    .because("Exception classes should be simple and not contain business logic");

            rule.check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("2. Naming Conventions")
    class NamingConventions {

        
        @Test
        @DisplayName("Classes in controller package should be named *Controller")
        void controllersShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..controller..")
                    .and().areAnnotatedWith(RestController.class)
                    .should().haveSimpleNameEndingWith("Controller")
                    .because("Controller classes should follow the naming convention *Controller");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in service package should be named *Service")
        void servicesShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..service..")
                    .and().areAnnotatedWith(Service.class)
                    .should().haveSimpleNameEndingWith("Service")
                    .because("Service classes should follow the naming convention *Service");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Interfaces in repository package should be named *Repository")
        void repositoriesShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..repository..")
                    .and().areInterfaces()
                    .should().haveSimpleNameEndingWith("Repository")
                    .because("Repository interfaces should follow the naming convention *Repository");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in dto package should be named *Dto, *Request, or *Response")
        void dtosShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..dto..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Dto")
                    .orShould().haveSimpleNameEndingWith("Request")
                    .orShould().haveSimpleNameEndingWith("Response")
                    .orShould().haveSimpleNameEndingWith("Message")
                    .orShould().haveSimpleNameEndingWith("Result")
                    .because("DTO classes should end with Dto, Request, Response, Message, or Result");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in exception package should be named *Exception or *Handler")
        void exceptionsShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..exception..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Exception")
                    .orShould().haveSimpleNameEndingWith("Handler")
                    .because("Exception classes should follow the naming convention *Exception or *Handler");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in config package should be named *Config or *Configuration or contain config-related name")
        void configClassesShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..config..")
                    .and().areTopLevelClasses()
                    .should().haveSimpleNameEndingWith("Config")
                    .orShould().haveSimpleNameEndingWith("Configuration")
                    .orShould().haveSimpleNameEndingWith("Properties")
                    .orShould().haveSimpleNameEndingWith("Filter")
                    .orShould().haveSimpleNameEndingWith("Indicator")
                    .orShould().haveSimpleNameEndingWith("Initializer")
                    .because("Configuration classes should follow Spring naming conventions");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in filter package should be named *Filter")
        void filtersShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..filter..")
                    .should().haveSimpleNameEndingWith("Filter")
                    .because("Filter classes should follow the naming convention *Filter");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes in mapper package should contain 'Mapper' in name")
        void mappersShouldBeNamedCorrectly() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..mapper..")
                    .should().haveSimpleNameContaining("Mapper")
                    .because("Mapper classes should contain 'Mapper' in the name");

            rule.check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("3. Annotation Rules")
    class AnnotationRules {

        
        @Test
        @DisplayName("@RestController should only be used in controller package")
        void restControllerAnnotationOnlyInControllerPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(RestController.class)
                    .should().resideInAPackage("..controller..")
                    .because("@RestController annotation should only be used in the controller package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("@Controller should only be used in controller package")
        void controllerAnnotationOnlyInControllerPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Controller.class)
                    .should().resideInAPackage("..controller..")
                    .because("@Controller annotation should only be used in the controller package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("@Service should only be used in service or security package")
        void serviceAnnotationOnlyInServicePackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().resideInAnyPackage("..service..", "..security..")
                    .because("@Service annotation should only be used in service or security packages");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("@Repository should only be used in repository package")
        void repositoryAnnotationOnlyInRepositoryPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Repository.class)
                    .should().resideInAPackage("..repository..")
                    .because("@Repository annotation should only be used in the repository package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("@Entity should only be used in model package")
        void entityAnnotationOnlyInModelPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..model..")
                    .because("@Entity annotation should only be used in the model package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("@Configuration should only be used in config package")
        void configurationAnnotationOnlyInConfigPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Configuration.class)
                    .should().resideInAPackage("..config..")
                    .because("@Configuration annotation should only be used in the config package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("All REST controller classes should have @RestController annotation")
        void controllerClassesShouldHaveRestControllerAnnotation() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..controller..")
                    .and().haveSimpleNameEndingWith("Controller")
                    .and().areNotInterfaces()
                    .and().haveSimpleNameNotContaining("WebSocket")
                    .should().beAnnotatedWith(RestController.class)
                    .because("REST Controller classes should be annotated with @RestController");

            rule.check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("4. Package Rules")
    class PackageRules {

        
        @Test
        @DisplayName("There should be no circular dependencies between business packages")
        void noCircularDependencies() {
            // Note: config<->security bidirectional dependency is allowed as it's a standard Spring Security pattern
            // We exclude these from the cycle check and focus on business logic packages
            JavaClasses businessClasses = new ClassFileImporter()
                    .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                    .importPackages("com.truholdem")
                    .that(DescribedPredicate.describe("not in config or security",
                        clazz -> !clazz.getPackageName().contains(".config") &&
                                 !clazz.getPackageName().contains(".security")));

            ArchRule rule = SlicesRuleDefinition.slices()
                    .matching("com.truholdem.(*)..")
                    .should().beFreeOfCycles()
                    .allowEmptyShould(true)
                    .because("Circular dependencies between business packages make the codebase harder to maintain");

            rule.check(businessClasses);
        }

        
        @Test
        @DisplayName("All classes should be under com.truholdem package")
        void allClassesShouldBeUnderBasePackage() {
            ArchRule rule = classes()
                    .should().resideInAPackage("com.truholdem..")
                    .because("All application classes should be under the base package com.truholdem");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Controller package should not have sub-packages")
        void controllerPackageShouldNotHaveSubPackages() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..controller..")
                    .should().resideInAPackage("..controller")
                    .because("Controller package should be flat without sub-packages");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Only Application class should be in root package")
        void onlyApplicationInRootPackage() {
            ArchRule rule = classes()
                    .that().resideInAPackage("com.truholdem")
                    .and().resideOutsideOfPackages(
                            "com.truholdem.controller..",
                            "com.truholdem.service..",
                            "com.truholdem.repository..",
                            "com.truholdem.model..",
                            "com.truholdem.dto..",
                            "com.truholdem.exception..",
                            "com.truholdem.config..",
                            "com.truholdem.filter..",
                            "com.truholdem.handler..",
                            "com.truholdem.mapper..",
                            "com.truholdem.security..",
                            "com.truholdem.domain..",
                            "com.truholdem.application..",
                            "com.truholdem.websocket..",
                            "com.truholdem.observability..",
                            "com.truholdem.listener.."
                    )
                    .should().haveSimpleNameEndingWith("Application")
                    .because("Root package should only contain the main Application class");

            rule.check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("5. Coding Rules")
    class CodingRules {

        
        @Test
        @DisplayName("No field injection - use constructor injection instead")
        void noFieldInjection() {
            ArchRule rule = noFields()
                    .should().beAnnotatedWith(Autowired.class)
                    .because("Field injection makes testing difficult - use constructor injection instead");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("No System.out.println in business logic")
        void noSystemOutPrintln() {
            
            ArchRule rule = noClasses()
                    .that().resideInAPackage("..service..")
                    .or().resideInAPackage("..controller..")
                    .or().resideInAPackage("..repository..")
                    .should().accessField(System.class, "out")
                    .orShould().accessField(System.class, "err")
                    .because("Use proper logging framework (SLF4J) instead of System.out/err");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        
        @Test
        @DisplayName("Controller methods should prefer ResponseEntity return type")
        void controllerMethodsShouldReturnResponseEntity() {
            
            
            
            ArchRule rule = methods()
                    .that().areDeclaredInClassesThat().areAnnotatedWith(RestController.class)
                    .and().arePublic()
                    .and().haveNameNotMatching(".*equals.*|.*hashCode.*|.*toString.*")
                    .should().haveRawReturnType(ResponseEntity.class)
                    .orShould().haveRawReturnType(Void.TYPE)
                    .orShould().haveRawReturnType(String.class)
                    .because("Controller methods should return ResponseEntity for consistent API responses");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        
        @Test
        @DisplayName("Services should not throw generic Exception")
        void servicesShouldNotThrowGenericException() {
            ArchRule rule = noMethods()
                    .that().areDeclaredInClassesThat().resideInAPackage("..service..")
                    .should().declareThrowableOfType(Exception.class)
                    .because("Services should throw specific exceptions for better error handling");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        
        @Test
        @DisplayName("Repositories should extend JpaRepository or CrudRepository")
        void repositoriesShouldExtendSpringDataInterfaces() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..repository..")
                    .and().areInterfaces()
                    .should().beAssignableTo(JpaRepository.class)
                    .orShould().beAssignableTo(CrudRepository.class)
                    .because("Repositories should extend Spring Data interfaces");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Entity classes should reside in model package")
        void entitiesShouldResideInModelPackage() {
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Entity.class)
                    .should().resideInAPackage("..model..")
                    .because("Entity classes should be organized in the model package");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Classes ending with 'Util' or 'Utils' should have private constructor (unless Spring beans)")
        void utilityClassesShouldHavePrivateConstructor() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Util")
                    .or().haveSimpleNameEndingWith("Utils")
                    .and().areNotAnnotatedWith(org.springframework.stereotype.Component.class)
                    .and().areNotAnnotatedWith(org.springframework.stereotype.Service.class)
                    .should().haveOnlyPrivateConstructors()
                    .because("Utility classes should not be instantiated (Spring beans excluded)");

            
            rule.allowEmptyShould(true).check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("6. Security Rules")
    class SecurityRules {

        
        @Test
        @DisplayName("No hardcoded static password constants")
        void noHardcodedPasswords() {
            
            
            ArchRule rule = noFields()
                    .that().haveNameContaining("password").or().haveNameContaining("Password")
                    .and().areStatic()
                    .and().areFinal()
                    .and().areDeclaredInClassesThat().resideOutsideOfPackage("..dto..")
                    .should().haveRawType(String.class)
                    .because("Passwords should not be hardcoded as static constants");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        
        @Test
        @DisplayName("JWT configuration should be injected, not hardcoded")
        void jwtSecretFromConfiguration() {
            ArchRule rule = classes()
                    .that().resideInAPackage("..security..")
                    .and().haveSimpleNameContaining("Jwt")
                    .should().dependOnClassesThat().resideInAPackage("..config..")
                    .because("JWT secrets should be loaded from configuration, not hardcoded");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Security classes should not have public fields")
        void securityClassesShouldNotHavePublicFields() {
            ArchRule rule = noFields()
                    .that().areDeclaredInClassesThat().resideInAPackage("..security..")
                    .should().bePublic()
                    .because("Security classes should encapsulate their fields");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Authentication logic should be in security or service packages")
        void authenticationLogicLocation() {
            ArchRule rule = classes()
                    .that().haveSimpleNameContaining("Auth")
                    .and().areNotAnnotatedWith(RestController.class)
                    .and().haveSimpleNameNotContaining("Controller")
                    .and().areNotEnums()
                    .should().resideInAnyPackage("..security..", "..service..", "..config..")
                    .because("Authentication logic should be in security, service, or config packages");

            rule.check(importedClasses);
        }
    }

    
    
    
    
    @Nested
    @DisplayName("7. Additional Quality Rules")
    class AdditionalQualityRules {

        
        @Test
        @DisplayName("Interfaces should not use 'I' prefix")
        void interfacesShouldNotUseIPrefix() {
            ArchRule rule = noClasses()
                    .that().areInterfaces()
                    .should().haveSimpleNameStartingWith("I")
                    .because("Java convention: interfaces should not have 'I' prefix");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Abstract classes should have 'Abstract' or 'Base' prefix (except sealed hierarchies)")
        void abstractClassesNaming() {
            ArchRule rule = classes()
                    .that().haveModifier(JavaModifier.ABSTRACT)
                    .and().areNotInterfaces()
                    
                    .and().haveSimpleNameNotEndingWith("Event")
                    .and().haveSimpleNameNotEndingWith("Exception")
                    .should().haveSimpleNameStartingWith("Abstract")
                    .orShould().haveSimpleNameStartingWith("Base")
                    .orShould().haveSimpleNameContaining("Abstract")
                    .because("Abstract classes should be clearly identifiable by name (sealed hierarchies excluded)");

            rule.allowEmptyShould(true).check(importedClasses);
        }

        
        @Test
        @DisplayName("Services should have focused responsibilities")
        void servicesShouldHaveFocusedResponsibilities() {
            
            ArchRule rule = classes()
                    .that().areAnnotatedWith(Service.class)
                    .should().accessClassesThat().resideInAnyPackage(
                            "..repository..",
                            "..service..",
                            "..model..",
                            "..dto..",
                            "..exception..",
                            "..config..",
                            "..security..",
                            "..domain..",
                            "..application..",
                            "java..",
                            "org.springframework..",
                            "org.slf4j..",
                            "jakarta.."
                    )
                    .because("Services should have focused responsibilities and clear dependencies");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Handler classes should be in appropriate packages")
        void handlerClassesLocation() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Handler")
                    .should().resideInAnyPackage("..handler..", "..exception..", "..security..")
                    .because("Handler classes should be in handler, exception, or security packages");

            rule.check(importedClasses);
        }

        
        @Test
        @DisplayName("Listener classes should be in appropriate packages")
        void listenerClassesLocation() {
            ArchRule rule = classes()
                    .that().haveSimpleNameEndingWith("Listener")
                    .should().resideInAnyPackage("..listener..", "..application..", "..config..")
                    .because("Listener classes should be in listener, application, or config packages");

            rule.check(importedClasses);
        }
    }
}
