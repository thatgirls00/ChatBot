package org.example.chatbot.domain;

import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;

@Entity
@Table(name = "academic_schedule")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AcademicSchedule implements Serializable {

    private static final long serialVersionUID = 1L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date")
    private String date;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String content;

    @Column(length = 64, unique = true)
    private String hash;
}