package com.setec.resource.util;

import com.setec.resource.base.BaseSpecification;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

public class SpecUtils {

    public static <T> Specification<T> buildFinalSpec(
            BaseSpecification<T> baseSpec,
            BaseSpecification.FilterDto filterBody,
            WebRequest request,
            String globalOperator,
            Map<String, Object> mandatoryConditions
    ) {
        // 1. Resolve Dynamic Filter (Body or URL Params)
        BaseSpecification.FilterDto filterDto = (filterBody != null) ? filterBody : 
                FilterUtils.buildFilterDto(request, FilterUtils.GlobalOperator.fromString(globalOperator));

        Specification<T> dynamicSpec = baseSpec.filter(filterDto);

        // 2. Build Mandatory Specification (Security/Ownership)
        Specification<T> mandatorySpec = (root, query, cb) -> {
            if (mandatoryConditions == null || mandatoryConditions.isEmpty()) {
                return cb.conjunction();
            }

            return mandatoryConditions.entrySet().stream()
                    .map(entry -> {
                        String[] pathParts = entry.getKey().split("\\.");
                        jakarta.persistence.criteria.Path<Object> path = root.get(pathParts[0]);
                        for (int i = 1; i < pathParts.length; i++) {
                            path = path.get(pathParts[i]);
                        }
                        return cb.equal(path, entry.getValue());
                    })
                    .reduce(cb::and)
                    .orElse(cb.conjunction());
        };

        // 3. Combine: Mandatory AND Dynamic
        return Specification.where(mandatorySpec).and(dynamicSpec);
    }
}