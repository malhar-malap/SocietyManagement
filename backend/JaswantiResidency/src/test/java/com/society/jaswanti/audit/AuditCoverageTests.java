package com.society.jaswanti.audit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the "everything important is logged" promise: a new state-changing endpoint cannot be added without
 * either auditing it or writing down why it is exempt.
 */
@SpringBootTest
class AuditCoverageTests {

    private static final Set<RequestMethod> WRITE_METHODS =
            EnumSet.of(RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH, RequestMethod.DELETE);

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    private RequestMappingHandlerMapping handlerMapping;

    private Map<RequestMappingInfo, HandlerMethod> applicationHandlers() {
        return handlerMapping.getHandlerMethods().entrySet().stream()
                .filter(e -> e.getValue().getBeanType().getPackageName().startsWith("com.society.jaswanti"))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    @Test
    @DisplayName("Every POST/PUT/PATCH/DELETE endpoint is either @AuditedAction or @AuditExempt with a reason")
    void everyWriteEndpointIsAuditedOrExempt() {
        List<String> missing = new ArrayList<>();
        int writeEndpoints = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> entry : applicationHandlers().entrySet()) {
            Set<RequestMethod> methods = entry.getKey().getMethodsCondition().getMethods();
            if (methods.isEmpty() || methods.stream().noneMatch(WRITE_METHODS::contains)) {
                continue;
            }
            writeEndpoints++;
            HandlerMethod handler = entry.getValue();
            AuditedAction audited = handler.getMethodAnnotation(AuditedAction.class);
            AuditExempt exempt = handler.getMethodAnnotation(AuditExempt.class);
            if (audited == null && (exempt == null || exempt.reason().isBlank())) {
                missing.add(methods + " " + entry.getKey().getPatternValues() + " -> "
                        + handler.getBeanType().getSimpleName() + "." + handler.getMethod().getName());
            }
        }
        assertThat(writeEndpoints).as("sanity: write endpoints discovered").isGreaterThan(30);
        assertThat(missing).as("Write endpoints with no audit decision").isEmpty();
    }

    @Test
    @DisplayName("Every @AuditedAction is fully described and uses a target type the resolver understands")
    void auditedActionsAreWellFormed() {
        Set<String> knownTargets = Stream.of(AuditTargetResolver.class.getDeclaredFields())
                .filter(f -> Modifier.isStatic(f.getModifiers()) && f.getType() == String.class)
                .map(f -> {
                    try {
                        return (String) f.get(null);
                    } catch (IllegalAccessException e) {
                        throw new IllegalStateException(e);
                    }
                })
                .collect(Collectors.toSet());

        List<String> problems = new ArrayList<>();
        for (HandlerMethod handler : applicationHandlers().values()) {
            AuditedAction a = handler.getMethodAnnotation(AuditedAction.class);
            if (a == null) {
                continue;
            }
            String where = handler.getBeanType().getSimpleName() + "." + handler.getMethod().getName();
            if (a.action().isBlank() || a.module().isBlank() || a.summary().isBlank()) {
                problems.add(where + ": blank action/module/summary");
            }
            if (!a.action().matches("[A-Z][A-Z_]*") || !a.module().matches("[A-Z][A-Z_]*")) {
                problems.add(where + ": action/module must be UPPER_SNAKE_CASE");
            }
            if (!a.target().isEmpty() && !knownTargets.contains(a.target())) {
                problems.add(where + ": unknown target type " + a.target());
            }
        }
        assertThat(problems).isEmpty();
    }
}
