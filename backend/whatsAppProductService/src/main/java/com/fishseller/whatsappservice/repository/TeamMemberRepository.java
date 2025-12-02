package com.fishseller.whatsappservice.repository;

import com.fishseller.whatsappservice.model.TeamMember;
import com.fishseller.whatsappservice.model.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamMemberRepository extends JpaRepository<TeamMember, Long> {

    /**
     * Find team member by WhatsApp phone number
     */
    Optional<TeamMember> findByWaPhoneNumber(String waPhoneNumber);

    Optional<TeamMember> findByPhoneNumber(String phoneNumber);

    List<TeamMember> findByIsActiveTrue();

    /**
     * Find team member by WhatsApp phone number and role
     */
    Optional<TeamMember> findByWaPhoneNumberAndRole(String waPhoneNumber, UserRole role);

    /**
     * Find all team members by role
     */
    List<TeamMember> findByRole(UserRole role);
}
