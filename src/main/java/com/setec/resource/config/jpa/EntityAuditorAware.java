//package com.setec.resource.config.jpa;
//
//import com.setec.online_survey.security.CustomUserDetails;
//import jakarta.validation.constraints.NotNull;
//import org.springframework.data.domain.AuditorAware;
//import org.springframework.security.core.Authentication;
//import org.springframework.security.core.context.SecurityContextHolder;
//
//import java.util.Optional;
//
//public class EntityAuditorAware implements AuditorAware<String> {
//    @org.jetbrains.annotations.NotNull
//    @NotNull
//    @Override
//    public Optional<String> getCurrentAuditor() {
//
//        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
//
//        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails user)) {
//            return Optional.of("SA_Admin");
//        }
//
//        return Optional.of(user.getUsername());
//    }
//}