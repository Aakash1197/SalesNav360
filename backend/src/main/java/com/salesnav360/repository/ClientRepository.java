package com.salesnav360.repository;

import com.salesnav360.entity.ClientRecord;
import com.salesnav360.enums.ClientStatus;
import com.salesnav360.enums.ClientType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<ClientRecord, Long> {

    @Query("""
            select c from ClientRecord c
            where c.status <> com.salesnav360.enums.ClientStatus.ARCHIVED
              and (:status is null or c.status = :status)
              and (:assignedTo is null or lower(c.assignedTo) = lower(:assignedTo))
              and (:query is null
                   or lower(c.fullName) like lower(concat('%', :query, '%'))
                   or lower(coalesce(c.email, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(c.companyName, '')) like lower(concat('%', :query, '%'))
                   or lower(coalesce(c.phone, '')) like lower(concat('%', :query, '%')))
            """)
    Page<ClientRecord> search(
            @Param("query") String query,
            @Param("status") ClientStatus status,
            @Param("assignedTo") String assignedTo,
            Pageable pageable);

    long countByTypeAndStatusNot(ClientType type, ClientStatus status);

    @Query("""
            select c from ClientRecord c
            where (:email is not null and lower(c.email) = lower(:email))
               or (:phone is not null and c.phone = :phone)
            order by c.id
            """)
    List<ClientRecord> findPotentialDuplicates(
            @Param("email") String email,
            @Param("phone") String phone);

    Optional<ClientRecord> findByIdAndStatusNot(Long id, ClientStatus status);
}

