package com.example.demo.conversation;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    Optional<Conversation> findByBuyer_UsernameAndSeller_Username(String buyerUsername, String sellerUsername);

    List<Conversation> findByBuyer_UsernameOrSeller_Username(String buyerUsername, String sellerUsername);

    @Query("""
            select c from Conversation c
            where c.buyer.username = :username
               or c.seller.username = :username
            order by c.updatedAt desc
            """)
    List<Conversation> findByParticipant(@Param("username") String username);

    @Query("""
            select c from Conversation c
            where c.id = :id
              and (c.buyer.username = :username or c.seller.username = :username)
            """)
    Optional<Conversation> findByIdAndParticipant(@Param("id") Long id, @Param("username") String username);
}
