//package com.setec.resource.config.jpa;
//
//import jakarta.persistence.EntityListeners;
//import jakarta.persistence.MappedSuperclass;
//import jakarta.persistence.Temporal;
//import jakarta.persistence.TemporalType;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//import org.springframework.data.annotation.CreatedBy;
//import org.springframework.data.annotation.CreatedDate;
//import org.springframework.data.annotation.LastModifiedBy;
//import org.springframework.data.annotation.LastModifiedDate;
//import org.springframework.data.jpa.domain.support.AuditingEntityListener;
//
//import java.time.LocalDateTime;
//
//@NoArgsConstructor
//@Getter
//@Setter
//@MappedSuperclass
//@EntityListeners(AuditingEntityListener.class)
//public abstract class Auditable<T> {
//
//    @CreatedBy
//    protected T createdBy;
//
//    @Temporal(TemporalType.TIMESTAMP)
//    @CreatedDate
//    protected LocalDateTime createdDate;
//
//    @LastModifiedBy
//    protected T lastModifiedBy;
//
//    @Temporal(TemporalType.TIMESTAMP)
//    @LastModifiedDate
//    protected LocalDateTime lastModifiedDate;
//
//}