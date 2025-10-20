package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "academic_notices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicNotice implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String noticeDate;
    private String author;

    @Column(columnDefinition = "TEXT")
    private String link;

    @Column(length = 64, unique = true)
    private String hash;
}